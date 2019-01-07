package tiny.lib

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.RedisURI

import java.time.Duration

private val connectTimeout = Duration.ofSeconds(3)

class TinyRedisSolo(host: String, port:Int = 6379, database: Int = 1, timeout: Duration = connectTimeout) {
	private val _host: String
	private val _port: Int
	private val _database: Int
	private val _timeout: Duration
	private lateinit var _client: RedisClient
	private var _conn: StatefulRedisConnection<String, String>? = null
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
	}

	fun connect(): StatefulRedisConnection<String, String> {
		val conn = _client.connect()
		_conn = conn
		return conn
	}

	fun close() {
		_conn?.close()
		_client.shutdown()
	}

}
