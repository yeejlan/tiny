package example.controller

import tiny.annotation.AutoWeave
import tiny.annotation.Controller
import javax.inject.Inject
import tiny.TinyController

@Controller
class UserController @AutoWeave constructor(): TinyController() {

	@Inject lateinit var man: Man

	fun indexAction(): Any {
		throw Exception("This is user/index page, tesing Internal Server Error")
	}

	fun infoAction(): Any {
		this.view["username"] = "Lina"
		return render("body")
	}

	fun apple(color: String): Any {
		return "this is a ${color} apple"
	}	

	fun helloAction(): Any {
		man.run()
		return ctx.params
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