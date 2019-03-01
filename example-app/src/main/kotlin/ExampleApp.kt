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
	app.bootstrap()

	try{
		test()

		if(!app.script.isEmpty()){
			TinyScript.run(app.env, app.name, app.script)
		}
	}finally{
		TinyApp.shutdown()
	}


}


fun test(){

	//println(testCache(123, 8L, 456L))
	
	val jdbc = TinyRegistry["db.account"] as TinyJdbc
	//val jdbc = TinyRegistry.get("db.account", TinyJdbc::class.java)

	val result = jdbc.queryForList("select id,name from user where id < :id order by id desc limit 5", mapOf(
			"id" to 1002,
			"name" to "note.gif"
		))
	//val uu = toTinyResult<User>()

	val trUsers = TinyResult<List<Map<String,Any>>>(result)
	val trUserObjs = TinyResult.fromList(result, User::class)
	if(trUsers.error()){
		DebugUtil.print(trUsers.cause)
	}else{
		DebugUtil.print(trUsers.data())
	}

	if(trUserObjs.error()){
		DebugUtil.print(trUserObjs.cause)
	}else{
		DebugUtil.print(trUserObjs.data())
	}
	
}

class User(val map: HashMap<String, Any>) {
	var id: Int by map
	var name: String by map
}

@AddCache("int_test_{a}")
fun testCache(a: Int, b: Long, c: Long): Int{
	return (a+c+b).toInt()
}
