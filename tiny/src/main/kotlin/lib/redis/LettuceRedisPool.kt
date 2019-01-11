package tiny.lib.redis

import io.lettuce.core.support.ConnectionPoolSupport
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.RedisClient
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
private val defaultHost = "127.0.0.1"
private val defaultPort = 6379
private val defaultDatabase = 1

class LettuceRedisPoolConfig() {
	var host = defaultHost
	var port = defaultPort
	var database = defaultDatabase
	var maxTotal = poolMaxTotal
	var maxIdle = poolMaxIdle
	var minIdle = poolMinIdle
	var poolName: String = ""
	private val _config = GenericObjectPoolConfig<Any>()

	init{
		_config.setTestOnBorrow(false)
		_config.setTestOnReturn(false)
		_config.setTestWhileIdle(false)
		_config.setMaxWaitMillis(poolMaxWaitMillis)
		_config.setMinEvictableIdleTimeMillis(poolMinEvictableIdleTimeMillis)
		_config.setTimeBetweenEvictionRunsMillis(poolTimeBetweenEvictionRunsMillis)		
	}

	fun getConfig(): GenericObjectPoolConfig<Any>{
		return _config
	}

	fun doConfig(): LettuceRedisPoolConfig{
		if(maxIdle > maxTotal){
			maxIdle = maxTotal
		}
		if(minIdle > maxIdle){
			minIdle = maxIdle
		}
		_config.setMinIdle(minIdle)
		_config.setMaxTotal(maxTotal)
		_config.setMaxIdle(maxIdle)
		return this
	}

	fun doConfig(config: TinyConfig, prefix: String): LettuceRedisPoolConfig{
		poolName = prefix
		host = config["${prefix}.host"]
		port = config.getInt("${prefix}.port", defaultPort)
		database = config.getInt("${prefix}.database", defaultDatabase)
		maxTotal = config.getInt("${prefix}.lettuce.maxTotal", poolMaxTotal)
		maxIdle = config.getInt("${prefix}.lettuce.maxIdle", poolMaxIdle)
		minIdle = config.getInt("${prefix}.lettuce.minIdle", poolMinIdle)
		return doConfig()
	}
}



class LettuceRedisPool(): LettuceDataSource {

	private lateinit var _pool: GenericObjectPool<StatefulRedisConnection<String, String>>
	private lateinit var _client: RedisClient
	private var _name: String = ""
	private val _statefulConnection = ThreadLocal<StatefulRedisConnection<String, String>>()

	fun create(config: LettuceRedisPoolConfig): LettuceRedisPool{
		val uri = RedisURI.Builder.redis(config.host, config.port)
					.withDatabase(config.database)
					.withTimeout(connectTimeout)
					.build()

		_name = config.poolName
		_client = RedisClient.create(uri)
		_pool = ConnectionPoolSupport.createGenericObjectPool({ _client.connect() }, config.getConfig())

		TinyApp.addShutdownHook(RedisShutdownHook())
		return this	
	}

	override fun getConnection(): StatefulRedisConnection<String, String> {
		val conn = _pool.borrowObject()
		_statefulConnection.set(conn)
		return conn
	}

	override fun close(){
		val conn = _statefulConnection.get()
		if(conn != null){
			_statefulConnection.remove()
			_pool.returnObject(conn)
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