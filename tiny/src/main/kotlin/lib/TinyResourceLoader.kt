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

	fun getRedisPoolConfig(config: TinyConfig, configName: String): LettuceRedisPoolConfig{
		val poolConfig = LettuceRedisPoolConfig().doConfig(config, configName)
		return poolConfig
	}

	fun getHikariConfig(config: TinyConfig, configName: String): DataSourceHikariConfig{
		val hikariConfig = DataSourceHikariConfig().doConfig(config, configName)
		return hikariConfig
	}

	fun loadTinyRedis(config: TinyConfig, configName: String, fixedPoolSize: Int = 0) {
		var poolConfig = getRedisPoolConfig(config, configName)
		if(fixedPoolSize > 0) {
			poolConfig = poolConfig.doFixedPoolConfig(fixedPoolSize)
		}
		val redisPool = LettuceRedisPool().create(poolConfig)

		TinyRegistry["pool." + configName] = redisPool
		TinyRegistry[configName] = TinyRedis(redisPool)
	}

	fun loadTinyJdbc(config: TinyConfig, configName: String, fixedPoolSize: Int = 0) {
		var hikariConfig = getHikariConfig(config, configName)
		if(fixedPoolSize > 0) {
			hikariConfig = hikariConfig.doFixedPoolConfig(fixedPoolSize)
		}
		val dataSourceHikari = DatasourceHiKari().create(hikariConfig)

		TinyRegistry["hikari." + configName] = dataSourceHikari
		TinyRegistry[configName] = TinyJdbc(dataSourceHikari)
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
				loadTinyJdbc(config, dataSourceName)
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
				loadTinyRedis(config, redisName)
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