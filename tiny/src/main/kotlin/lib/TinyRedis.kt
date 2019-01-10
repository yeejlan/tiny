package tiny.lib

import com.fasterxml.jackson.module.kotlin.*
import io.lettuce.core.support.ConnectionPoolSupport
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.RedisURI

import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import java.time.Duration

import tiny.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(TinyRedis::class.java)
private val objectMapper = jacksonObjectMapper()

private val connectTimeout = Duration.ofSeconds(3)
private val poolMaxTotal = 10
private val poolMaxIdle = 10
private val poolMinIdle = 2
private val poolMaxWaitMillis = 3000L
private val poolMinEvictableIdleTimeMillis = 600000L
private val poolTimeBetweenEvictionRunsMillis = 600000L

class TinyRedisPoolConfig() {
	var maxTotal = poolMaxTotal
	var maxIdle = poolMaxIdle
	var minIdle = poolMinIdle
	var maxWaitMillis = poolMaxWaitMillis
	var minEvictableIdleTimeMillis = poolMinEvictableIdleTimeMillis
	var timeBetweenEvictionRunsMillis = poolTimeBetweenEvictionRunsMillis
	private val _config = GenericObjectPoolConfig<Any>()

	init{
		_config.setTestOnBorrow(false)
		_config.setTestOnReturn(false)
		_config.setTestWhileIdle(false)
	}

	fun getConfig(): GenericObjectPoolConfig<Any>{
		return _config
	}

	fun doConfig(): TinyRedisPoolConfig{
		if(maxIdle > maxTotal){
			maxIdle = maxTotal
		}
		if(minIdle > maxIdle){
			minIdle = maxIdle
		}
		_config.setMinIdle(minIdle)
		_config.setMaxTotal(maxTotal)
		_config.setMaxIdle(maxIdle)
		_config.setMaxWaitMillis(maxWaitMillis)
		_config.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis)
		_config.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis)
		return this
	}

	fun doConfig(config: TinyConfig, prefix: String): TinyRedisPoolConfig{
		maxTotal = config.getInt("${prefix}.maxTotal", poolMaxTotal)
		maxIdle = config.getInt("${prefix}.maxIdle", poolMaxIdle)
		minIdle = config.getInt("${prefix}.minIdle", poolMinIdle)
		maxWaitMillis = config.getLong("${prefix}.maxWaitMillis", poolMaxWaitMillis)
		minEvictableIdleTimeMillis = config.getLong("${prefix}.minEvictableIdleTimeMillis", poolMinEvictableIdleTimeMillis)
		timeBetweenEvictionRunsMillis = config.getLong("${prefix}.timeBetweenEvictionRunsMillis", poolTimeBetweenEvictionRunsMillis)
		return doConfig()
	}
}

class TinyRedis(host: String, port:Int = 6379, database: Int = 1, timeout: Duration = connectTimeout) {
	private val _host: String
	private val _port: Int
	private val _database: Int
	private val _timeout: Duration
	private lateinit var _pool: GenericObjectPool<StatefulRedisConnection<String, String>>
	private lateinit var _client: RedisClient
	private var _name: String = ""

	init {
		_host = host
		_port = port
		_database = database
		_timeout = timeout
		_name = "host=${_host}, port=${_port}, database=${_database}"
	}

	fun create(redisConfig: TinyRedisPoolConfig? = null): TinyRedis{
		val uri = RedisURI.Builder.redis(_host, _port)
					.withDatabase(_database)
					.withTimeout(_timeout)
					.build()

		var config = redisConfig
		if(config == null){
			config = TinyRedisPoolConfig().doConfig()
		}
		_client = RedisClient.create(uri)
		_pool = ConnectionPoolSupport.createGenericObjectPool({ _client.connect() }, config.getConfig())

		TinyApp.addShutdownHook(RedisShutdownHook())
		return this	
	}

	fun exec(body: (StatefulRedisConnection<String, String>) -> Unit) {
		val conn = _pool.borrowObject()
		try{
			body(conn)
		}catch(e: Throwable){
			logger.warn("exec error", e)
		}finally{
			_pool.returnObject(conn)
		}
	}

	fun query(body: (StatefulRedisConnection<String, String>) -> String?): String {
		val conn = _pool.borrowObject()
		var value: String? = null
		try{
			value = body(conn)
		}catch(e: Throwable){
			logger.warn("exec error", e)
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
