package tiny.lib.redis

import io.lettuce.core.support.BoundedPoolConfig
import tiny.TinyConfig

private val poolMaxTotal = 10
private val poolMaxIdle = 10
private val poolMinIdle = 1
private val defaultHost = "127.0.0.1"
private val defaultPort = 6379
private val defaultDatabase = 1

class LettuceAsyncRedisPoolConfig() {
	var host = defaultHost
	var port = defaultPort
	var database = defaultDatabase
	var maxTotal = poolMaxTotal
	var maxIdle = poolMaxIdle
	var minIdle = poolMinIdle
	var poolName: String = ""
	private val _builder = BoundedPoolConfig.builder()

	init{
		_builder.testOnAcquire(false)
		_builder.testOnCreate(false)
		_builder.testOnRelease(false)
	}

	fun getConfig(): BoundedPoolConfig{
		return _builder.build()
	}

	fun doConfig(): LettuceAsyncRedisPoolConfig{
		if(maxIdle > maxTotal){
			maxIdle = maxTotal
		}
		if(minIdle > maxIdle){
			minIdle = maxIdle
		}
		_builder.minIdle(minIdle)
		_builder.maxTotal(maxTotal)
		_builder.maxIdle(maxIdle)
		return this
	}

	fun doConfig(config: TinyConfig, prefix: String): LettuceAsyncRedisPoolConfig{
		poolName = prefix
		host = config["${prefix}.host"]
		port = config.getInt("${prefix}.port", defaultPort)
		database = config.getInt("${prefix}.database", defaultDatabase)
		maxTotal = config.getInt("${prefix}.lettuce.maxTotal", poolMaxTotal)
		maxIdle = config.getInt("${prefix}.lettuce.maxIdle", poolMaxIdle)
		minIdle = config.getInt("${prefix}.lettuce.minIdle", poolMinIdle)
		return doConfig()
	}

	fun doFixedPoolConfig(poolSize: Int): LettuceAsyncRedisPoolConfig{
		_builder.minIdle(1)
		_builder.maxTotal(poolSize)
		_builder.maxIdle(poolSize)
		return this
	}
}
