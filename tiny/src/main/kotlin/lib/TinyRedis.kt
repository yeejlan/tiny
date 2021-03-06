package tiny.lib

import tiny.*
import tiny.lib.redis.*
import com.fasterxml.jackson.module.kotlin.*
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.DeserializationFeature
import io.lettuce.core.api.StatefulRedisConnection
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(TinyRedis::class.java)
private val objectMapper = jacksonObjectMapper()

class TinyRedis(ds: LettuceDataSource) {
	private var _datasource: LettuceDataSource 

	init{
		_datasource = ds
		objectMapper.enable(DeserializationFeature.USE_LONG_FOR_INTS)
	}

	fun exec(body: (StatefulRedisConnection<String, String>) -> Unit, throwEx: Boolean = false) {
		val conn = _datasource.getConnection()
		conn.use {
			try{
				body(conn)
			}catch(e: Throwable){
				logger.error("Redis exec error on ${this} " + e)
				if(throwEx){
					throw e
				}
			}
		}
	}

	fun query(body: (StatefulRedisConnection<String, String>) -> String?, throwEx: Boolean = false): String {
		val conn = _datasource.getConnection()
		var value: String? = null
		conn.use {
			try{
				value = body(conn)
			}catch(e: Throwable){
				logger.error("Redis query error on ${this} " + e)
				if(throwEx){
					throw e
				}
			}
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
			return
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
			return null
		}
	}

	fun get(key: String, valueType: JavaType): Any? {
		val value = this.get(key)
		if(value.isEmpty()){
			return null
		}
		try {
			return objectMapper.readValue(value, valueType)
		}catch (e: Throwable){
			return null
		}
	}

	override fun toString(): String {
		return this::class.java.getSimpleName() + "[${_datasource}]"
	}
}
