package tiny.lib.redis

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import tiny.TinyConfig

private val poolMaxTotal = 10
private val poolMaxIdle = 10
private val poolMinIdle = 1
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

	fun doFixedPoolConfig(poolSize: Int): LettuceRedisPoolConfig{
		_config.setMinIdle(1)
		_config.setMaxTotal(poolSize)
		_config.setMaxIdle(poolSize)
		return this
	}
}
