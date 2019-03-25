package tiny.lib.redis

import io.lettuce.core.support.ConnectionPoolSupport
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.support.BoundedPoolConfig
import java.util.concurrent.CompletableFuture
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.support.AsyncPool
import io.lettuce.core.support.AsyncConnectionPoolSupport

import tiny.*
import java.time.Duration

private val connectTimeout = Duration.ofSeconds(3)

class LettuceAsyncRedisPool(): LettuceAsyncDataSource {

	private lateinit var _pool: AsyncPool<StatefulRedisConnection<String, String>>
	private lateinit var _client: RedisClient
	private var _name: String = ""

	fun create(config: LettuceAsyncRedisPoolConfig): LettuceAsyncRedisPool{
		val uri = RedisURI.Builder.redis(config.host, config.port)
					.withDatabase(config.database)
					.withTimeout(connectTimeout)
					.build()

		_name = config.poolName
		_client = RedisClient.create()
		_pool = AsyncConnectionPoolSupport.createBoundedObjectPool({ _client.connectAsync(StringCodec.UTF8, uri) }, config.getConfig())

		TinyApp.addShutdownHook(AsyncRedisShutdownHook())
		return this	
	}

	override fun getConnection(): CompletableFuture<StatefulRedisConnection<String, String>> {
		val conn = _pool.acquire()
		return conn
	}

	override fun toString(): String {
		return this::class.java.getSimpleName() + "[${_name}]"
	}

	private inner class AsyncRedisShutdownHook() : TinyShutdownHook {

		override fun shutdownProcess() {
			_pool.close()
			_client.shutdown()
		}
	}	
}