package example

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.annotation.WebServlet

import tiny.*
import tiny.annotation.TinyApplication

@TinyApplication
class ExampleApp : TinyBootstrap {

	override fun bootstrap() {

		val env = System.getProperty("tiny.appliction.env") ?: "production"
		val appName = "tiny"

		TinyApp.init(env, appName)

		TinyRouter.addRoute("/hello/(.*)", "user/hello", arrayOf(Pair(1, "username")))
	}
}

fun main(args: Array<String>) {
	test()
	//TinyApp.runJetty()
}

fun test(){
	val env = System.getProperty("tiny.appliction.env") ?: "production"
	val appName = "tiny"
	TinyApp.init(env, appName)

	for(i in 1..5){
		TinyLog.log("the content ${i}", "log${i}")		
	}
	Thread.sleep(1000)
}