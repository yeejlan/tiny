package example.controller

class UserController {

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