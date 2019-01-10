package example

import tiny.*
import tiny.lib.*
import tiny.annotation.TinyApplication

@TinyApplication
class ExampleApp : TinyBootstrap {

	override fun bootstrap() {

		val env = System.getProperty("tiny.appliction.env") ?: "production"
		val appName = "exampleapp"

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
	val appName = "exampleapp"

	TinyApp.init(env, appName)

	val jdbc = TinyJdbc()

	jdbc.use("account")
	jdbc.exec("select * from user limit 5")

	TinyApp.shutdown()
}