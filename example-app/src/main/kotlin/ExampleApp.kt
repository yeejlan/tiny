package example

import tiny.*
import tiny.lib.*
import tiny.lib.db.*
import tiny.annotation.*
import javax.servlet.annotation.WebListener

@TinyApplication
class ExampleApp : TinyBootstrap {
	val name = "exampleapp"
	val env = System.getProperty("tiny.app.env") ?: "production"
	val script = System.getProperty("tiny.app.script") ?: ""

	override fun bootstrap() {

		TinyApp.init(env, name)
		TinyRouter.addRoute("/hello/(.*)", "user/hello", arrayOf(Pair(1, "username")))
	}
}

fun main(args: Array<String>) {

	val app = ExampleApp()
	test()

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

	val trUsers = TinyResult<List<Map<String,Any>>>(users)
	DebugUtil.print(trUsers.error)
	if(trUsers.error == null){
		DebugUtil.print(trUsers.data)
	}

	//users.ex?.printStackTrace()
	//DebugUtil.print(users.data)
	DebugUtil.print(TinyRegistry.getStorage())
	DebugUtil.print(Thread.currentThread().getStackTrace()[1])
	val result = TinyResult<Int>("get int error", null)
	val result2 = TinyResult<String>("get string error", result)
	val result3 = TinyResult<Int>("3rd error", "no idea")
	val result4 = TinyResult<String>(null, "Nana")
	DebugUtil.print(result.cause)
	DebugUtil.print(result2.cause)
	DebugUtil.print(result3.cause)
	DebugUtil.print(result4.cause)
	DebugUtil.inspect(result4.data)
	TinyApp.shutdown()
}

@AddCache("int_test_{a}")
fun testCache(a: Int, b: Long, c: Long): Int{
	return (a+c+b).toInt()
}
