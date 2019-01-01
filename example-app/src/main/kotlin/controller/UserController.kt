package example.controller

import tiny.annotation.AutoWeave
import tiny.annotation.Controller
import javax.inject.Inject

@Controller
@AutoWeave
class UserController @AutoWeave constructor(){

	@Inject lateinit var man: Man

	fun indexAction(): Any {
		return "this is user/index page"
	}

	fun infoAction(): Any {
		return "this is user/info page"
	}

	fun apple(color: String): Any {
		man.run()
		return "this is a ${color} apple"
	}	

	fun doSomething() {}

	private fun privateAction(): Any {
		return "this is private place"
	}
}

class Man @Inject constructor() {
	fun run(){
		println("a MAN is running")
	}
}