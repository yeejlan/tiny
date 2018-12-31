package example

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.annotation.WebServlet

import tiny.*

import tiny.annotation.TinyApplication
import tiny.annotation.TinyControllers
import tiny.annotation.TinyHelpers

import javax.inject.Inject
import dagger.Component
import dagger.Provides
import dagger.Module

import tiny.generated.DaggerMagicBox

@WebServlet(name="ExampleServlet",urlPatterns=arrayOf("/*"))
class ExampleServlet() : HttpServlet() {
	
	override fun init() {
		TinyApp.init("testing", "config/development/tiny.properties") 
		TinyApp.bootstrap(ExampleBootstrap())
		TinyController.loadControllers("example.controller")
		tiny.generated.TinyHelperLoader.loadHelpers()
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

@TinyApplication("demo2", daggerModules = arrayOf(AppModule::class))
@TinyControllers("example.controller")
//@TinyHelpers("example.helper")
class ExampleBootstrap : TinyBootstrap {
	@Inject lateinit var cat: Cat

	init{
		val dd = DaggerMagicBox.create()
		dd.inject(this)
	}

	override fun bootstrap() {
		println("write your bootstrap code here")
		cat.miao()
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


@Component(modules = arrayOf(AppModule::class))
interface MMMM{
	fun inject(target: ExampleServlet)
	fun inject(target: ExampleBootstrap)
}


class CatAPP {
	@Inject lateinit var _ccccat: Cat
}