package tiny

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import tiny.TinyWebContext

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

		val uri = request.getPathInfo().trim('/')
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

		//val one
		val actions = TinyController.getActions()
		//if()

	}
}