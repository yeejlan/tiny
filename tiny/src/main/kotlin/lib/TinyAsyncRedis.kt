package tiny.lib

import tiny.*
import tiny.lib.redis.*
import com.fasterxml.jackson.module.kotlin.*
import com.fasterxml.jackson.databind.DeserializationFeature
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.RedisFuture
import io.lettuce.core.TransactionResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(TinyAsyncRedis::class.java)
private val objectMapper = jacksonObjectMapper()

class TinyAsyncRedis(ds: LettuceAsyncDataSource) {
	private var _datasource: LettuceAsyncDataSource 

	init{
		_datasource = ds
		objectMapper.enable(DeserializationFeature.USE_LONG_FOR_INTS)
	}

	fun exec(body: (RedisAsyncCommands<String, String>) -> RedisFuture<Any>): CompletableFuture<Boolean> {

		val retFut = CompletableFuture<Boolean>()
		_datasource.getConnection().thenCompose{conn ->
		var fut: RedisFuture<Any>? = null
		try{
			val asyncCommands = conn.async()
			fut = body(asyncCommands)
		}catch(e: Throwable){
			conn.closeAsync()
			logger.error("Redis async exec error on ${this} " + e)
			retFut.complete(false)
		}
		fut?.whenComplete{ _, e -> 
				conn.closeAsync()
				if(e is Throwable) {
					logger.error("Redis exec future error on ${this} " + e)
				}
				retFut.complete(true)
			}
		}
		return retFut
	}

	fun query(body: (RedisAsyncCommands<String, String>) -> RedisFuture<String>): CompletableFuture<String> {

		val retFut = CompletableFuture<String>()
		_datasource.getConnection().thenCompose{conn ->
		var fut: RedisFuture<String>? = null
		try{
			val asyncCommands = conn.async()
			fut = body(asyncCommands)
		}catch(e: Throwable){
			conn.closeAsync()
			logger.error("Redis async query error on ${this} " + e)
			retFut.complete("")
		}
		fut?.whenComplete{ v, e -> 
				conn.closeAsync()
				if(e is Throwable) {
					logger.error("Redis query future error on ${this} " + e)
				}
				retFut.complete(v)
			}
		}
		return retFut
	}

	fun set(key: String, value: String, expireSeconds: Long = 3600): CompletableFuture<Boolean> {
		val fut = exec({ commands ->
			commands.multi()
			commands.set(key, value)
			commands.expire(key, expireSeconds)
			@Suppress("UNCHECKED_CAST")
			commands.exec() as RedisFuture<Any>
		})
		return fut
	}

	fun set(key: String, value: Any, expireSeconds: Long = 3600): CompletableFuture<Boolean> {
		var valueStr: String
		try {
			valueStr =  objectMapper.writeValueAsString(value)
		}catch (e: Throwable){
			return CompletableFuture.completedFuture(false)
		}
		return set(key, valueStr, expireSeconds)
	}

	fun expire(key: String, expireSeconds: Long = 3600): CompletableFuture<Boolean> {
		val fut = exec({ commands ->
			@Suppress("UNCHECKED_CAST")
			commands.expire(key, expireSeconds) as RedisFuture<Any>
		})
		return fut
	}

	fun delete(key: String): CompletableFuture<Boolean> {
		val fut = exec({ commands ->
			@Suppress("UNCHECKED_CAST")
			commands.del(key) as RedisFuture<Any>
		})
		return fut
	}

	fun get(key: String): CompletableFuture<String> {
		val fut = query({ commands ->
			commands.get(key)
		})

		return fut
	}

	fun <T> get(key: String, valueType: Class<T> ): CompletableFuture<T?> {
		val retFut = CompletableFuture<T?>()

		val fut = this.get(key)
		fut.whenComplete{ value, e -> 
			if(value.isEmpty() || e is Throwable){
				retFut.complete(null)
				return@whenComplete
			}
			try {
				val obj = objectMapper.readValue(value, valueType)
				retFut.complete(obj)
				return@whenComplete
			}catch (e: Throwable){
				retFut.complete(null)
				return@whenComplete
			}			
		}
		return retFut
	}

	override fun toString(): String {
		return this::class.java.getSimpleName() + "[${_datasource}]"
	}
}
