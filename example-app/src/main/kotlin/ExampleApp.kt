package example

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.annotation.WebServlet

import tiny.*

import tiny.annotation.TinyApplication
import tiny.annotation.AutoWeave

import javax.inject.Inject
import dagger.Component
import dagger.Provides
import dagger.Module


@AutoWeave
@WebServlet(name="ExampleServlet",urlPatterns=arrayOf("/*"))
class ExampleServlet() : HttpServlet() {
	
	override fun init() {
		TinyApp.init("testing", "config/development/tiny.properties") 
		TinyApp.bootstrap(ExampleBootstrap())
	}

	override fun doGet(request: HttpServletRequest, response: HttpServletResponse){
		response.setContentType("text/html;charset=UTF-8")
		val out = response.getWriter()
		val view = TinyView()
		out.println(view.render("body"))
	}

	override fun destroy(){
		//pass
	}
}

@AutoWeave
@TinyApplication(config = "demo2")
class ExampleBootstrap @AutoWeave constructor() : TinyBootstrap {
	@Inject lateinit var cat: Cat

	override fun bootstrap() {
		println("write your bootstrap code here~")
		cat.miao()
	}
}

fun callAction() {
	val app = ExampleBootstrap()
	app.bootstrap()

	val userController = example.controller.UserController()
	println(userController.infoAction())
	println(userController.apple("RED"))
}

fun main(args: Array<String>) {
	
	callAction()
}

class Cat @Inject constructor() {
	fun miao() {
		println("miao~ miao~ miao~")
	}
}