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
		val configFile = "config/${env}/${appName}.properties"

		TinyApp.init(env, configFile)

		TinyRouter.addRoute("/hello/(.*)", "user/hello", arrayOf(Pair(1, "username")))
	}
}

fun main(args: Array<String>) {

	TinyApp.runJetty(tiny.web.TinyServlet::class.java)
}
