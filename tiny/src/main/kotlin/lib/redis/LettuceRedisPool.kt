package tiny.lib.redis

import io.lettuce.core.support.ConnectionPoolSupport
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI

import org.apache.commons.pool2.impl.GenericObjectPool

import tiny.*
import java.time.Duration

private val connectTimeout = Duration.ofSeconds(3)

class LettuceRedisPool(): LettuceDataSource {

	private lateinit var _pool: GenericObjectPool<StatefulRedisConnection<String, String>>
	private lateinit var _client: RedisClient
	private var _name: String = ""

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
		return conn
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