package example.script

import tiny.*
import tiny.lib.*

class Hello{

	fun run() {

		val redisConfig = TinyConfig("config/${TinyApp.getEnvString()}/redis.ini")
		val dbConfig = TinyConfig("config/${TinyApp.getEnvString()}/db.ini")

		val loader = TinyResourceLoader()
		loader.loadRedis(redisConfig, "redis.default", fixedPoolSize = 1)
		loader.loadJdbc(dbConfig, "db.account", fixedPoolSize = 1)


		printRegister()
		printHello()
		throw HelloScriptException("something is wrong")

	}

	fun printRegister() {
		DebugUtil.print(TinyRegistry.getStorage())
	}

	fun printHello() {
		println("this is hello script")
	}
}

private class HelloScriptException(message: String?) : Throwable(message)