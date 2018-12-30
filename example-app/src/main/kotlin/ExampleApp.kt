package example

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.annotation.WebServlet

import tiny.TinyApp
import tiny.TinyController
import tiny.TinyView
import tiny.TinyConfig
import tiny.TinyBootstrap

import tiny.annotation.TinyApplication
import tiny.annotation.TinyControllers
import tiny.annotation.TinyHelpers

import javax.inject.Inject
import dagger.Component
import dagger.Provides
import dagger.Module

@WebServlet(name="ExampleServlet",urlPatterns=arrayOf("/*"))
class ExampleServlet() : HttpServlet() {
	@Inject lateinit var cat: Cat

	override fun init() {
		TinyApp.init("testing", "config/development/tiny.properties") 
		TinyApp.bootstrap(ExampleBootstrap())
		TinyController.loadControllers("example.controller")
		TinyView.loadHelpers("example.helper")

		DaggerMagicBox.create().inject(this)
	}

	override fun doGet(request: HttpServletRequest, response: HttpServletResponse){
		response.setContentType("text/html;charset=UTF-8")
		val out = response.getWriter()
		val view = TinyView()
		out.println(view.render("body"))
		cat.miao()
	}

	override fun destroy(){
		//pass
	}
}

@TinyApplication(name = "demo")
@TinyControllers("example.controller")
@TinyHelpers("example.helper")
class ExampleBootstrap : TinyBootstrap {
	override fun bootstrap() {
		println("write your bootstrap code here")
	}
}

fun main(args: Array<String>) {
	val config = TinyConfig("config/development/tiny.properties")
	println(config.getBoolean("dd"))
	TinyApp.runJetty(ExampleServlet::class.java)
}

class Cat @Inject constructor() {
	fun miao() {
		println("miao~ miao~ miao~")
	}
}

@Module
class AppModule {

}


@Component(modules = arrayOf(AppModule::class))
interface MagicBox{
	fun inject(app: ExampleServlet)
}
