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
	
		tiny.web.TinyControllerLoader.loadActions()
		tiny.web.TinyHelperLoader.loadHelpers()
	}

	override fun doGet(request: HttpServletRequest, response: HttpServletResponse){
		TinyRouter.dispatch(request, response)
	}

	override fun doPost(request: HttpServletRequest, response: HttpServletResponse){
		TinyRouter.dispatch(request, response)
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

		val env = "development"
		val appName = "tiny"
		val configFile = "config/${env}/${appName}.properties"
		TinyApp.init(env, configFile)
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
	
	//callAction()
	TinyApp.runJetty(ExampleServlet::class.java)
}

class Cat @Inject constructor() {
	fun miao() {
		println("miao~ miao~ miao~")
	}
}