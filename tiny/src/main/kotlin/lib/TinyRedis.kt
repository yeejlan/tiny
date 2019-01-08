package tiny.lib

import io.lettuce.core.support.ConnectionPoolSupport
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.RedisURI

import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import java.time.Duration

import tiny.*

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

class TinyRedis(host: String, port:Int = 6379, database: Int = 1, timeout: Duration = connectTimeout): AutoCloseable {
	private val _host: String
	private val _port: Int
	private val _database: Int
	private val _timeout: Duration
	private lateinit var _pool: GenericObjectPool<StatefulRedisConnection<String, String>>
	private lateinit var _client: RedisClient
	private val _conn = ThreadLocal<StatefulRedisConnection<String, String>?>()
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

	fun connect(): StatefulRedisConnection<String, String> {
		val conn = _pool.borrowObject()
		_conn.set(conn)
		return conn
	}

	override fun close() {
		val conn = _conn.get()
		if(conn != null){
			_pool.returnObject(conn)
		}
	}

	override fun toString(): String {
		return this::class.java.getSimpleName() + "[${_name}]"
	}

	private inner class RedisShutdownHook() : TinyShutdownHook {

		override fun shutdownProcess() {
			try{
				_pool.close()
				_client.shutdown()
			}catch(e: Throwable){
				//pass
			}
		}
	}
}
