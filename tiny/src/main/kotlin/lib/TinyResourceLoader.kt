package tiny.lib

import com.zaxxer.hikari.HikariDataSource
import tiny.*
import tiny.lib.db.*
import tiny.lib.redis.*
import tiny.lib.session.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(TinyResourceLoader::class.java)

class TinyResourceLoader{
	val _envString = TinyApp.getEnvString()

	fun buildRedisPool(config: TinyConfig, prefix: String): LettuceRedisPool{
		val poolConfig = LettuceRedisPoolConfig().doConfig(config, prefix)
		return LettuceRedisPool().create(poolConfig)
	}

	fun buildDataSourceHikari(config: TinyConfig, prefix: String): HikariDataSource{
		val hikariConfig = DataSourceHikariConfig().doConfig(config, prefix)
		return DatasourceHiKari().create(hikariConfig)
	}

	fun load(){
		_loadDataSourceHikari()
		_loadRedis()
		_loadSessionStorage()
	}

	private fun _loadDataSourceHikari(){
		val configFile = "config/${_envString}/db.ini"
		lateinit var config: TinyConfig
		try{
			config = TinyConfig(configFile)
		}catch(e: TinyException){
			return
		}

		val configMatcher = "^db\\.([_a-zA-Z0-9]+)\\.url".toRegex()
		for(one in config.getConfigMap()){
			val key = one.key
			if(configMatcher.containsMatchIn(key)){
				val dataSourceName = key.substring(0, key.length - ".url".length)
				val dataSourceHikari = buildDataSourceHikari(config, dataSourceName)
				TinyRegistry["hikari." + dataSourceName] = dataSourceHikari
				TinyRegistry[dataSourceName] = TinyJdbc(dataSourceHikari)
			}
		}
	}

	private fun _loadRedis(){
		val configFile = "config/${_envString}/redis.ini"
		lateinit var config: TinyConfig
		try{
			config = TinyConfig(configFile)
		}catch(e: TinyException){
			return 
		}

		val configMatcher = "^redis\\.([_a-zA-Z0-9]+)\\.host".toRegex()
		for(one in config.getConfigMap()){
			val key = one.key
			if(configMatcher.containsMatchIn(key)){
				val redisName = key.substring(0, key.length - ".host".length)
				val redisPool = buildRedisPool(config, redisName)
				TinyRegistry["pool." + redisName] = redisPool
				TinyRegistry[redisName] = TinyRedis(redisPool)
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