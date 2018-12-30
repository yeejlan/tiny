package example.controller

import tiny.annotation.DaggerInject

@DaggerInject
class UserController {

	fun constructor(name: String){}

	fun indexAction(): Any {
		return "this is index page"
	}

	fun userAction(): Any {
		return "this is user page"
	}

	fun aaa() {}

	private fun cccAction(): Any {
		return "this is a private function"
	}
}