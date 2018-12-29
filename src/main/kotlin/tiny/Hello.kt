package tiny

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.annotation.WebServlet

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHandler

import javax.inject.Inject
import dagger.Component
import dagger.Provides
import dagger.Module


import groovy.text.StreamingTemplateEngine

class DDDog {
	fun getDog(): String{
		return "[this is a dog]"
	}
}

fun include(): String{
	return "[this is a include]"
}

fun main(args: Array<String>) {
	val text = """
	aaa<%=dog.getDog()%>cccc"""

	val template = StreamingTemplateEngine().createTemplate(text)
	val model: Map<String, Any> = mapOf(
			"firstname" to "Grace",
			"lastname"  to "Hopper",
			"accepted"  to true,
			"title"     to "Groovy for COBOL programmers",
			"dog" to DDDog()
		)
	val str = template.make(model)
	println(str)

	TinyApp.init("development", "config/development/tiny.properties")
	TinyApp.bootstrap()
	println(TinyApp.getEnv())
	println(TinyApp.getEnvString())
	val config = TinyApp.getConfig()
	println(config)
	println(config["timezone"])
	println(config.getLong("cdc",10))
	return
	val server = Server(8080)
	val handler = ServletHandler()
    server.setHandler(handler)
    handler.addServletWithMapping(Hello::class.java, "/*")
    server.start()
    server.join()
}

@WebServlet(name="mytest",urlPatterns=arrayOf("/*"), loadOnStartup=1)
class Hello() : HttpServlet() {
  val message = "hello~"

  @Inject lateinit var _cat: Cat
  
  override fun init() {
     //pass
  }

  override fun doGet(request: HttpServletRequest, response: HttpServletResponse){

      response.setContentType("text/html;charset=UTF-8")

      val out = response.getWriter()
      out.println(message)
      println(message)
      DaggerMagicBox.create().inject(this)
      _cat.miao()
  }

  override fun destroy(){
      //pass
  }
}


class Cat @Inject constructor(){
	@Inject lateinit var _dog: Dog
	@Inject lateinit var _fish: Fish

	fun miao(){
		println("miao miao miao")
		_dog.wang()
		_fish.swim()
	}
}

open class Dog constructor(){
	open fun wang() {
		println("wang wang")
	}
}

class SmallDog @Inject constructor() : Dog() {
	override fun wang() {
		super.wang()
		println("[small dog] wang wang")
	}
}

interface Fish{
	fun swim()
}

class GoldenFish @Inject constructor() : Fish {
	@Inject lateinit var _man : Man

	override fun swim() {
		println("[GoldenFish] swim...")
		_man.run()
	}
}


class Man @Inject constructor(){
	fun run(){
		println("[man] running")
	}
}


@Module
class FishModule {

	@Provides fun provideFish(impl: GoldenFish): Fish {
		return impl
	}

	@Provides fun provideDog(impl: SmallDog): Dog {
		return impl
	}	
}


@Component(modules = arrayOf(FishModule::class))
interface MagicBox{
	fun inject(app: Hello)
}