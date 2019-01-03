package tiny

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.lang.reflect.InvocationTargetException

data class TinyRewrite(val rewriteUrl: String, val action: String, val matchList: List<Map<Int, String>>? = null)

object TinyRouter{
	private val _routers: HashMap<Regex, TinyRewrite> = HashMap()
	private val _controller = ThreadLocal<String>()
	private val _action = ThreadLocal<String>()

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
	* add a regex router
	* TinyRewrite('shop/product/(\d+)', 'shop/showprod', array(1 => 'prod_id'))
	* will match uri '/shop/product/1001' to 'shop' controller and 'showprod' action, with $_GET['prod_id'] = 1001
	**/
	@JvmStatic fun addRoute(rule: TinyRewrite){
		val r = rule.rewriteUrl.toRegex()
		_routers.put(r, rule)
	}

	/**
	* router dispatch to controller/action
	*/
	@JvmStatic fun dispatch(request: HttpServletRequest, response: HttpServletResponse){
		_controller.set("")
		_action.set("")

		val uri = request.getPathInfo().trim('/').toLowerCase()
		val routeMatched = false
		var controller = ""
		var action = ""

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

		val ctx = TinyWebContext(request, response)
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

		try{

			if(actionPair != null){
				_controller.set(controller)
				_action.set(action)
				_callMethod(ctx, actionPair)
			}else{
				_pageNotFound(ctx)
			}

		}catch(e: InvocationTargetException){
			ctx.exception = e
			_internalServerError(ctx, e)	
		}
	}

	private fun _callMethod(ctx: TinyWebContext, actionPair: ActionPair){
		val targetClz = actionPair.first
		val targetAction = actionPair.second

		val _instance = targetClz.newInstance()
		(_instance as TinyController).ctx = ctx

		val _method = targetClz.getMethod(targetAction)

		val out = _method.invoke(_instance)
		val writer = ctx.response.getWriter()
		if(out is groovy.lang.Writable){
			out.writeTo(writer)
		}else{
			writer.print(out)
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

	private fun _internalServerError(ctx: TinyWebContext, e: Throwable){
		ctx.response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)

		val actionKey = "error/page500"
		val actions = TinyController.getActions()
		val actionPair = actions.get(actionKey)
		if(actionPair == null){
			_printInternalError(ctx, e)
			return
		}
		
		_callMethod(ctx, actionPair)
	}

	fun _printInternalError(ctx: TinyWebContext, e: Throwable){
		val writer = ctx.response.getWriter()
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