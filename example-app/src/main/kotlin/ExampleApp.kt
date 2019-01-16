package example

import tiny.*
import tiny.lib.*
import tiny.lib.db.*
import tiny.annotation.*
import javax.servlet.annotation.WebListener

@TinyApplication
class ExampleApp : TinyBootstrap {
	val name = "exampleapp"
	val env = System.getProperty("tiny.appliction.env") ?: "production"
	val script = System.getProperty("tiny.appliction.script") ?: ""

	override fun bootstrap() {

		TinyApp.init(env, name)
		TinyRouter.addRoute("/hello/(.*)", "user/hello", arrayOf(Pair(1, "username")))
	}
}

fun main(args: Array<String>) {
	val app = ExampleApp()
	TinyScript.run(app.env, app.name, "example.script.Hello")
	return
	test()
	return

	if(!app.script.isEmpty()){
		TinyScript.run(app.env, app.name, app.script)
		return
	}

	//TinyApp.runJetty()
}


fun test(){

	val app = ExampleApp()
	app.bootstrap()

	//println(testCache(123, "nana", 456L))
	
	val jdbc = TinyRegistry["db.account"] as TinyJdbc
	//val jdbc = TinyRegistry.get("db.account", TinyJdbc::class.java)

	val users = jdbc.queryForList("select id,name from user where id < :id order by id desc limit 5", mapOf(
			"id" to 1002,
			"name" to "note.gif"
		))


	users.ex?.printStackTrace()
	DebugUtil.print(users.data)
	DebugUtil.print(TinyRegistry.getStorage())

	TinyApp.shutdown()
}


@CacheAdd("int_test_{a}")
fun testCache(a: Int, b: String, c: Long): Int{
	return (a+c).toInt()
}
