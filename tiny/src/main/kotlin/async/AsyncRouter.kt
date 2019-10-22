package tiny.async

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.lang.reflect.InvocationTargetException
import java.net.URLDecoder
import java.io.InputStream
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.File
import java.nio.file.Paths
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import org.slf4j.LoggerFactory
import tiny.lib.TinyProfiler
import tiny.*

private val DEFAULT_EXPIRE_TIME_IN_MILLIS = TimeUnit.DAYS.toMillis(30)
private val ONE_SECOND_IN_MILLIS = TimeUnit.SECONDS.toMillis(1)
private val staticExtraDir = TinyApp.getConfig()["static.extra.dir"]
private val profilingName = TinyApp.getConfig()["profiling.name"]
private val profilingToken = TinyApp.getConfig()["profiling.token"]
private val logger = LoggerFactory.getLogger(AsyncRouter::class.java)

object AsyncRouter{
	private val _controller = ThreadLocal<String>()
	private val _action = ThreadLocal<String>()
	private val _ctx = ThreadLocal<AsyncWebContext>()
	private val _cachedThreadPool = Executors.newCachedThreadPool()

	/**
	* get current controller
	**/
	@JvmStatic fun currController(): String {
		return _controller.get()
	}

	/**
	* get current Action
	**/
	@JvmStatic fun currAction(): String {
		return _action.get()
	}

	/**
	* get ctx
	**/
	@JvmStatic fun ctx(): AsyncWebContext {
		return _ctx.get()
	}

	/**
	* terminate current request handling
	**/
	@JvmStatic fun exit(): Unit {
		throw TinyRouterExitException("exit")
	}

	/**
	* router dispatch to controller/action
	*/
	@JvmStatic fun dispatch(request: HttpServletRequest, response: HttpServletResponse){
		_controller.set("")
		_action.set("")

		val requestUri = request.getPathInfo()
		var uri = requestUri
		if(uri == null) {
			uri = "/"
		}
		uri = uri.trim('/').toLowerCase()
		var routeMatched = false
		var controller = ""
		var action = ""
		val aCtx = request.startAsync()
		val ctx = AsyncWebContext(aCtx, request, response)
		_ctx.set(ctx)

		if(TinyApp.getEnv() > TinyApp.PRODUCTION){
			val staticFileFound = TinyRouter._serveStaticFile(ctx as TinyWebContext) 
			if(staticFileFound){
				return
			}
		}

		//check rewrite rules
		val extraParams: HashMap<String, String> = HashMap()
		for(route in TinyRouter.routers()){
			val(r, objRewrite) = route
			val matchResult = r.matchEntire(requestUri)
			if(matchResult == null){
				continue
			}
			//route matched
			val rewriteToArr = objRewrite.rewriteTo.split('/')
			if(rewriteToArr.size == 2){
				controller = rewriteToArr[0]
				action = rewriteToArr[1]
			}
			//add params
			if(objRewrite.paramMapping != null){
				val matches = matchResult.groupValues
				if(matches.size != objRewrite.paramMapping.size + 1){
					throw TinyException("Match map does not match the rewrite rule: " + objRewrite)
				}
				for(one in objRewrite.paramMapping){
					if(one.first < matches.size){
						extraParams.put(one.second, matches[one.first])
					}
				}
			}

			for(param in extraParams){
				ctx.params[param.key] = param.value
			}

			routeMatched = true
			break
		}

		//normal controller/action parse
		if(!routeMatched){
			val uriArr = uri.split('/')  //format: 'controller/action'
			if(uriArr.size == 1){
				controller = uriArr[0]
				action = "index"
			}else if(uriArr.size == 2){
				controller = uriArr[0]
				action = uriArr[1]
			}
		}		

		_cachedThreadPool.execute({
			try{
				callAction(ctx, controller, action)
			}catch(e: Throwable){
				logger.error(e.toString())
			}finally{
				ctx.complete()
			}
		})
	}


	/**
	* find controller and call action
	**/
	@JvmStatic fun callAction(ctx: AsyncWebContext, controllerStr: String, actionStr: String){
		
		var controller = controllerStr
		var action = actionStr		
		if(controller.isEmpty()){
			controller = "home"
		}

		if(action.isEmpty()){
			action = "index"
		}

		val actions = TinyController.getActions()
		val actionKey = "${controller}/${action}"

		ctx.request.setCharacterEncoding("UTF-8")
		ctx.response.setContentType("text/html;charset=UTF-8")

		val actionPair = actions.get(actionKey)

		if(actionPair != null){ //action found
			_controller.set(controller)
			_action.set(action)
			try{
				_callMethod(ctx, actionPair)
			}catch(e: Throwable){
				if(e is TinyRouterExitException){ //ignore TinyRouterExitException
					//pass
				}else if(e is InvocationTargetException && e.getTargetException() is TinyRouterExitException){
					//pass
				}else{
					ctx.exception = e
					_internalServerError(ctx)
				}
			} finally{
				//make sure session got saved
				ctx.session.save()
			}
		}else{ //action not found
			_pageNotFound(ctx)
		}

	}

	private fun _callMethod(ctx: AsyncWebContext, actionPair: ActionPair){
		TinyProfiler.disable()
		if(!profilingName.isEmpty() && !profilingToken.isEmpty() && ctx.params[profilingName] == profilingToken){
			TinyProfiler.init()
			TinyProfiler.enable()
		}
		val targetClz = actionPair.first
		val targetAction = actionPair.second

		val _instance = targetClz.newInstance()
		(_instance as TinyController).ctx = ctx

		val _method = targetClz.getMethod(targetAction)
		val _before = targetClz.getMethod("before")
		val _after = targetClz.getMethod("after")

		ctx.loadSession()
		_before.invoke(_instance)
		val out = _method.invoke(_instance)
		_after.invoke(_instance)
		ctx.session.save()
		val writer = ctx.response.getWriter()
		if(null == out) {
			//pass
		}else if(out is groovy.lang.Writable){
			out.writeTo(writer)
		}else{
			writer.print(out)
		}
		if(TinyProfiler.enabled()){
			writer.print("<!--profiling begin---------\r\n")
			writer.print(TinyProfiler.toString())
			writer.print("\r\n---------profiling end-->")
		}
	}

	private fun _pageNotFound(ctx: AsyncWebContext){
		ctx.response.setStatus(HttpServletResponse.SC_NOT_FOUND)

		val actionKey = "error/page404"
		val actions = TinyController.getActions()
		val actionPair = actions.get(actionKey)
		if(actionPair == null){
			val writer = ctx.response.getWriter()
			writer.println("Page Not Found!")
			return
		}
	
		_callMethod(ctx, actionPair)

	}

	private fun _internalServerError(ctx: AsyncWebContext){
		ctx.response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)

		val actionKey = "error/page500"
		val actions = TinyController.getActions()
		val actionPair = actions.get(actionKey)
		if(actionPair == null){
			_printInternalError(ctx)
			return
		}
		
		_callMethod(ctx, actionPair)
	}

	private fun _printInternalError(ctx: AsyncWebContext){
		val writer = ctx.response.getWriter()
		val e = ctx.exception!!
		writer.println("Internal Server Error!")
		if(TinyApp.getEnv() > TinyApp.PRODUCTION){
			writer.println("<br /><pre>\r\n")
			if(e is InvocationTargetException ){
				val targetException = e.getTargetException()
				targetException.printStackTrace(writer)
			}else{
				e.printStackTrace(writer)
			}
			writer.println("</pre><br />\r\n")
		}
	}
}