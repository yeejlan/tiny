package tiny.lib.redis

import java.lang.AutoCloseable
import io.lettuce.core.api.StatefulRedisConnection

interface LettuceDataSource : AutoCloseable{

	fun getConnection(): StatefulRedisConnection<String, String>

	override fun close()
}