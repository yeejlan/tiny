package example

import tiny.*
import tiny.lib.*
import tiny.lib.db.*
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
	val ret = jdbc.queryForList("select * from user limit 5 1", null)
	println("queryForList result= " + ret)
	val ret2 = jdbc.queryForMap("select * from user limit 5", null)
	println("queryForMap result= " + ret2)

	TinyApp.shutdown()
}