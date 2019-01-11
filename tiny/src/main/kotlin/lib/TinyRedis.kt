package tiny.lib

import tiny.*
import tiny.lib.redis.*

class TinyRedis(host: String, port:Int = 6379, database: Int = 1, timeout: Duration = connectTimeout) {


	fun exec(body: (StatefulRedisConnection<String, String>) -> Unit) {
		val conn = _pool.borrowObject()
		try{
			body(conn)
		}finally{
			_pool.returnObject(conn)
		}
	}

	fun query(body: (StatefulRedisConnection<String, String>) -> String?): String {
		val conn = _pool.borrowObject()
		var value: String?
		try{
			value = body(conn)
		}finally{
			_pool.returnObject(conn)
		}
		return value ?: ""
	}

	fun set(key: String, value: String, expireSeconds: Long = 3600) {
		exec({ connection ->
			val commands = connection.sync()
			commands.multi()
			commands.set(key, value)
			commands.expire(key, expireSeconds)
			commands.exec()
		})
	}

	fun set(key: String, value: Any, expireSeconds: Long = 3600) {
		var valueStr: String
		try {
			valueStr =  objectMapper.writeValueAsString(value)
		}catch (e: Throwable){
			throw e
		}
		set(key, valueStr, expireSeconds)
	}

	fun expire(key: String, expireSeconds: Long = 3600) {
		exec({ connection ->
			val commands = connection.sync()
			commands.expire(key, expireSeconds)
		})
	}

	fun delete(key: String) {
		exec({ connection ->
			val commands = connection.sync()
			commands.del(key)
		})
	}

	fun get(key: String): String {
		val value = query({ connection ->
			val commands = connection.sync()
			commands.get(key)
		})

		return value
	}

	fun <T> get(key: String, valueType: Class<T> ): T? {
		
		val value = this.get(key)
		if(value.isEmpty()){
			return null
		}
		try {
			return objectMapper.readValue(value, valueType)
		}catch (e: Throwable){
			throw e
		}
	}

	override fun toString(): String {
		return this::class.java.getSimpleName() + "[${_name}]"
	}

	private inner class RedisShutdownHook() : TinyShutdownHook {

		override fun shutdownProcess() {
			_pool.close()
			_client.shutdown()
		}
	}
}
