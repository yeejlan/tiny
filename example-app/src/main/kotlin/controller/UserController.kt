package example.controller

import tiny.annotation.AutoWeave
import tiny.annotation.Controller

@Controller
@AutoWeave
class UserController @AutoWeave constructor(){

	fun indexAction(): Any {
		return "this is user/index page"
	}

	fun infoAction(): Any {
		return "this is user/info page"
	}

	fun doSomething() {}

	private fun privateAction(): Any {
		return "this is private place"
	}
}