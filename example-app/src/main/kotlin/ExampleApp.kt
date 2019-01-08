package example

import tiny.*
import tiny.lib.*
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
	//test()
	TinyApp.runJetty()
}


fun test(){
	val env = System.getProperty("tiny.appliction.env") ?: "production"
	val appName = "tiny"
	TinyApp.init(env, appName)

	//val redis = TinyRedis("127.0.0.1").create()
	println(TinyRegistry)

	for(i in 1..5){
		//TinyLog.log("the content ${i}", "log${i}")		
	}
	Thread.sleep(1)
	TinyApp.shutdown()
}