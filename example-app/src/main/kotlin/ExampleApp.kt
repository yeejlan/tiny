package example

import tiny.*
import tiny.lib.*
import tiny.lib.db.*
import tiny.annotation.*
import javax.servlet.annotation.WebListener

@TinyApplication
class ExampleApp : TinyBootstrap {

	override fun bootstrap() {

		val env = System.getProperty("tiny.appliction.env") ?: "production"
		val appName = "exampleapp"

		TinyApp.init(env, appName)

		TinyRouter.addRoute("/hello/(.*)", "user/hello", arrayOf(Pair(1, "username")))
	}
}

@WebListener
class MyFileCleanerCleanupListener() : org.apache.commons.fileupload.servlet.FileCleanerCleanup()

fun main(args: Array<String>) {

	testCache(123, "nana", 456L)
	//test()
	//TinyApp.runJetty()
}


fun test(){

	val env = System.getProperty("tiny.appliction.env") ?: "production"
	val appName = "exampleapp"

	TinyApp.init(env, appName)

	val jdbc = TinyRegistry["db.account"] as TinyJdbc
	//val jdbc = TinyRegistry.get("db.account", TinyJdbc::class.java)

	val users = jdbc.queryForList("select id,name from user where id < :id order by id desc limit 5", mapOf(
			"id" to 1002,
			"name" to "note.gif"
		))

	users.ex?.printStackTrace()
	DebugUtil.print(users.data)

	TinyApp.shutdown()
}


@CacheAdd("cccc")
fun testCache(a: Int, b: String, c: Long): String{
	return "a=$a, b=$b, c=$c"
}
