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
class ExampleBootstrap : TinyBootstrap {
	@Inject lateinit var cat: Cat

	init{

	}

	override fun bootstrap() {
		println("write your bootstrap code here~")
		cat.miao()
	}
}

fun main(args: Array<String>) {
	val app = ExampleBootstrap()
	DaggerMMMM.create().weave(app)
	app.bootstrap()

	val userC = example.controller.UserController()
	println(userC.infoAction())

	println(userC.apple("RED"))
//tiny.weaver.TinyBird


}

class Cat @Inject constructor() {
	fun miao() {
		println("miao~ miao~ miao~")
	}
}

@Component(modules = arrayOf(AppModule::class))
interface MMMM{
	fun weave(target: ExampleServlet)
	fun weave(target: ExampleBootstrap)
}


class CatAPP {
	@Inject lateinit var _ccccat: Cat
}