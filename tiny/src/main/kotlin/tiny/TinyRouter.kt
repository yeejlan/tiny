package tiny

data class TinyRewrite(val rewriteUrl: String, val action: String, val matchList: List<Map<Int, String>>? = null)

object TinyRouter{
	private val _routers: HashMap<Regex, TinyRewrite> = HashMap()
	private val _controller = ThreadLocal<String>()
	private val _action = ThreadLocal<String>()

	/**
	* get current controller
	**/
	fun currController(): String {
		return _controller.get()
	}

	/**
	* get current Action
	**/
	fun currAction(): String {
		return _action.get()
	}

	/**
	* add a regex router
	* TinyRewrite('shop/product/(\d+)', 'shop/showprod', array(1 => 'prod_id'))
	* will match uri '/shop/product/1001' to 'shop' controller and 'showprod' action, with $_GET['prod_id'] = 1001
	**/
	fun addRoute(rule: TinyRewrite){
		val r = rule.rewriteUrl.toRegex()
		_routers.put(r, rule)
	}

	/**
	* router dispatch to controller/action
	*/
	fun dispatch(){
	}


	/**
	* find controller and call action
	**/
	fun callAction(controller: String, action: String){

	}
}