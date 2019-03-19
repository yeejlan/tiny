package tiny.lib

import tiny.*
import tiny.lib.*
import io.lettuce.core.api.StatefulRedisConnection

object TinyCache {

	private val redisProvider = TinyApp.getConfig()["cache.storage.provider"]
	private val redis: TinyRedis = TinyRegistry[redisProvider]
	private val cacheEnable = TinyApp.getConfig().getBoolean("cache.enable")
	private val cachePrefix = TinyApp.getConfig()["cache.prefix"]
	private val cacheExpireSeconds = TinyApp.getConfig().getLong("cache.expire.seconds", 3600)

	@JvmStatic fun exec(body: (StatefulRedisConnection<String, String>) -> Unit) {
		if(!cacheEnable){
			return
		}
		redis.exec(body)
	}

	@JvmStatic fun query(body: (StatefulRedisConnection<String, String>) -> String?): String {
		if(!cacheEnable){
			return ""
		}
		return redis.query(body)
	}

	@JvmStatic fun set(key: String, value: String, expireSeconds: Long = cacheExpireSeconds) {
		if(!cacheEnable){
			return
		}
		redis.set(cachePrefix + key, value, expireSeconds)
	}

	@JvmStatic fun set(key: String, value: Any, expireSeconds: Long = cacheExpireSeconds) {
		if(!cacheEnable){
			return
		}
		redis.set(cachePrefix + key, value, expireSeconds)
	}

	@JvmStatic fun get(key: String): String {
		if(!cacheEnable){
			return ""
		}
		val value = redis.get(cachePrefix + key)
		return value
	}

	@JvmStatic fun <T> get(key: String, valueType: Class<T> ): T? {
		if(!cacheEnable){
			return null
		}
		return redis.get(cachePrefix + key, valueType)
	}

	@JvmStatic fun delete(key: String) {
		if(!cacheEnable){
			return
		}
		redis.delete(cachePrefix + key)
	}

	@JvmStatic fun expire(key: String, expireSeconds: Long = cacheExpireSeconds) {
		if(!cacheEnable){
			return
		}
		redis.expire(cachePrefix + key, expireSeconds)
	}

}