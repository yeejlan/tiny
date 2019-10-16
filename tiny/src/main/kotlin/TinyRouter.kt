package tiny

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
import tiny.lib.TinyProfiler

private val DEFAULT_EXPIRE_TIME_IN_MILLIS = TimeUnit.DAYS.toMillis(30)
private val ONE_SECOND_IN_MILLIS = TimeUnit.SECONDS.toMillis(1)
private val staticExtraDir = TinyApp.getConfig()["static.extra.dir"]
private val profilingName = TinyApp.getConfig()["profiling.name"]
private val profilingToken = TinyApp.getConfig()["profiling.token"]

private data class TinyRewrite(val regex: String, val rewriteTo: String, val paramMapping: Array<Pair<Int, String>>? = null)

object TinyRouter{
	private val _routers: HashMap<Regex, TinyRewrite> = HashMap()
	private val _controller = ThreadLocal<String>()
	private val _action = ThreadLocal<String>()
	private val _ctx = ThreadLocal<TinyWebContext>()

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
	@JvmStatic fun ctx(): TinyWebContext {
		return _ctx.get()
	}

	/**
	* terminate current request handling
	**/
	@JvmStatic fun exit(): Unit {
		throw TinyRouterExitException("exit")
	}

	/**
	* add a regex router
	* TinyRewrite("shop/product/(\d+)", "shop/showprod", array(1 => "prod_id"))
	* will match uri "/shop/product/1001" to "shop" controller and "showprod" action, with ctx.params["prod_id"] = 1001
	**/
	@JvmStatic fun addRoute(regex: String, rewriteTo: String, paramMapping: Array<Pair<Int, String>>? = null){
		val r = regex.toRegex()
		val rule = TinyRewrite(regex, rewriteTo, paramMapping)
		_routers.put(r, rule)
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
		val ctx = TinyWebContext(request, response)
		_ctx.set(ctx)

		if(TinyApp.getEnv() > TinyApp.PRODUCTION){
			val staticFileFound = _serveStaticFile(ctx) 
			if(staticFileFound){
				return
			}
		}

		//check rewrite rules
		val extraParams: HashMap<String, String> = HashMap()
		for(route in _routers){
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

		callAction(ctx, controller, action)
	}


	/**
	* find controller and call action
	**/
	@JvmStatic fun callAction(ctx: TinyWebContext, controllerStr: String, actionStr: String){
		
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

	private fun _callMethod(ctx: TinyWebContext, actionPair: ActionPair){
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

	private fun _pageNotFound(ctx: TinyWebContext){
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

	private fun _internalServerError(ctx: TinyWebContext){
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

	private fun _printInternalError(ctx: TinyWebContext){
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

	private fun _serveStaticFile(ctx: TinyWebContext): Boolean {
		val fileNotFound = false

		val BASEPATH = "static"

		val request = ctx.request
		val response = ctx.response

		val uri = request.getPathInfo()
		if(uri == null) {
			return fileNotFound
		}
		val filePath = URLDecoder.decode(uri, "UTF-8")

		var file: File? = null
		if(!staticExtraDir.isEmpty()){
			file = Paths.get(staticExtraDir, filePath).toFile()
		}
		if(file!=null && !file.exists()){//not found in extra dir
			val resourceUrl = TinyRouter::class.java.classLoader.getResource(BASEPATH + filePath)
			if(resourceUrl == null){
				return fileNotFound
			}

			file = File(resourceUrl.toURI())
		}


		if(file!=null && file.exists() && file.isFile() && file.canRead()){
			//pass
		}else{
			return fileNotFound
		}
		
		val fileLength = file.length()
		val fileName = file.getName()
		val lastModified = file.lastModified()

		val isModified = _isStaticFileModified(ctx, fileName, lastModified)
		if(!isModified){
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED)
			return true //file Found
		}

		_setStaticFileCache(ctx, fileName, lastModified)

		val context = request.getServletContext()
		if(context == null){
			throw TinyException("Could not get ServletContext!")
		}
		response.setHeader("Content-Length", fileLength.toString())
		response.setHeader("Content-Type", context.getMimeType(fileName))
		response.setHeader("Content-Disposition", "inline;filename=${fileName}")

		var inStream: FileInputStream? = null
		val buffer = ByteArray(8192)
		try {
			inStream = FileInputStream(file)
			var bytesRead: Int
			val outStream = response.getOutputStream()

			do{
				bytesRead = inStream.read(buffer, 0, buffer.size)
				outStream.write(buffer, 0, bytesRead)

			}while (bytesRead == buffer.size)

		}catch(e: Throwable){
			throw e
		}
		finally {
			if (inStream != null) {
				inStream.close()
			}
		}

		return true //file Found
	}

	private fun _setStaticFileCache(ctx: TinyWebContext, fileName: String, lastModified: Long) {

		val etag = "w/${fileName}-${lastModified}"
		ctx.response.setHeader("ETag", etag)
		ctx.response.setDateHeader("Last-Modified", lastModified)
		ctx.response.setDateHeader("Expires", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME_IN_MILLIS)
	}

	private fun _isStaticFileModified(ctx: TinyWebContext, etag: String, lastModified: Long): Boolean {
		val ifNoneMatch = ctx.request.getHeader("If-None-Match")

		if (ifNoneMatch == etag) {
			return false
		}
		else {
			val ifModifiedSince = ctx.request.getDateHeader("If-Modified-Since")
			if(ifModifiedSince + ONE_SECOND_IN_MILLIS > lastModified){
				return false
			}
		}
		return true
	}

}

class TinyRouterExitException(message: String?) : Throwable(message)