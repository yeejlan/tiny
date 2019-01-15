package tiny.lib

import tiny.*
import tiny.lib.*

object TinyCache {

	private val redis = TinyRegistry["redis.default"] as TinyRedis
	private val cacheEnable = TinyApp.getConfig().getBoolean("cache.enable")
	private val cachePrefix = TinyApp.getConfig()["cache.prefix"]
	private val cacheExpireSeconds = TinyApp.getConfig().getLong("cache.expire.seconds", 3600)

	@JvmStatic fun set(key: String, value: Any, expireSeconds: Long = cacheExpireSeconds) {
		if(!cacheEnable){
			return
		}
		redis.set(cachePrefix + key, value, expireSeconds)
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

}