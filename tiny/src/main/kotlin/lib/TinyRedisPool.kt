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
private val poolMinEvictableIdleTimeMillis = 120000L
private val poolTimeBetweenEvictionRunsMillis = 120000L

class TinyRedisPool(host: String, port:Int = 6379, database: Int = 1, timeout: Duration = connectTimeout) {
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
		settle()
	}

	fun settle() {
		val uri = RedisURI.Builder.redis(_host, _port)
					.withDatabase(_database)
					.withTimeout(_timeout)
					.build()

		_name = uri.toString()
		_client = RedisClient.create(uri)
		val config = GenericObjectPoolConfig<Any>()
		config.setMinIdle(poolMinIdle)
		config.setMaxTotal(poolMaxTotal)
		config.setMaxIdle(poolMaxIdle)
		config.setMaxWaitMillis(poolMaxWaitMillis)
		config.setMinEvictableIdleTimeMillis(poolMinEvictableIdleTimeMillis)
		config.setTimeBetweenEvictionRunsMillis(poolTimeBetweenEvictionRunsMillis)
		config.setTestOnBorrow(false)
		config.setTestOnReturn(false)
		config.setTestWhileIdle(false)

		_pool = ConnectionPoolSupport.createGenericObjectPool({ _client.connect() }, config)

		TinyApp.addShutdownHook(RedisShutdownHook())
	}

	fun connect(): StatefulRedisConnection<String, String> {
		val conn = _pool.borrowObject()
		_conn.set(conn)
		return conn
	}

	fun close() {
		val conn = _conn.get()
		if(conn != null){
			_pool.returnObject(conn)
		}
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
