package tiny.lib

import tiny.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(TinyResourceLoader::class.java)

class TinyResourceLoader{
	val _envString = TinyApp.getEnvString()

	fun buildRedis(config: TinyConfig, prefix: String): TinyRedis{
		val host = config.getString("${prefix}.host")
		val port = config.getInt("${prefix}.port")
		val database = config.getInt("${prefix}.database")
		val poolConfig = TinyRedisPoolConfig().doConfig(config, "${prefix}.pool")
		return TinyRedis(host, port, database).create(poolConfig)

	}

	fun load(){
		_loadRedis()
		_loadSessionStorage()
	}

	private fun _loadRedis(){
		val configFile = "config/${_envString}/redis.ini"
		lateinit var config: TinyConfig
		try{
			config = TinyConfig(configFile)
		}catch(e: TinyException){
			return 
		}

		val configMatcher = "redis\\.([_a-zA-Z0-9]+)\\.host".toRegex()
		for(one in config.getConfigMap()){
			val key = one.key
			if(configMatcher.containsMatchIn(key)){
				val redisName = key.substring(0, key.length - ".host".length)
				val tinyRedis = buildRedis(config, redisName)
				TinyRegistry[redisName] = tinyRedis
			}
		}
	}

	private fun _loadSessionStorage() {
		val configName = "session.storage"
		var storageName = TinyApp.getConfig()[configName]
		if(storageName.isEmpty()){
			logger.warn("""App config["${configName}"] not found, fallback to "redis" """)
			storageName = "redis"
		}
		val storageSupported = SessionStorage.getStorageSupported()
		if(!storageSupported.contains(storageName)) {
			logger.warn("""Session storage not supported: "${storageName}", session disabled""")
			return
		}
		when (storageName) {
			"redis" -> SessionStorage.set(SessionStorageRedis())
		}
	}
}