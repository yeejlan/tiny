package controller

import tiny.annotation.Controller

@Controller
class TestController{

	fun indexAction(): Any {
		return "this is index"
	}

	fun userAction(): Any {
		return "this is user"
	}
}