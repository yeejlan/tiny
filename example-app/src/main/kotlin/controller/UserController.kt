package example.controller

import tiny.annotation.AutoWeave
import tiny.annotation.Controller
import javax.inject.Inject
import tiny.TinyController

@Controller
class UserController @AutoWeave constructor(): TinyController() {

	@Inject lateinit var man: Man

	fun indexAction(): Any {
		throw Exception("This is user/index page, tesing internal server error")
	}

	fun infoAction(): Any {
		return render("body")
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