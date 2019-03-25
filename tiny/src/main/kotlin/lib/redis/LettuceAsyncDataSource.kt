package tiny.lib.redis

import io.lettuce.core.api.StatefulRedisConnection
import java.util.concurrent.CompletableFuture

interface LettuceAsyncDataSource {

	fun getConnection(): CompletableFuture<StatefulRedisConnection<String, String>> 
}