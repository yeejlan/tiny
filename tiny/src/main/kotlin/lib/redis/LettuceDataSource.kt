package tiny.lib.redis

import io.lettuce.core.api.StatefulRedisConnection

interface LettuceDataSource {

	fun getConnection(): StatefulRedisConnection<String, String>
}