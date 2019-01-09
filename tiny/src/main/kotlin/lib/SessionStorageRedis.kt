package tiny.lib

import tiny.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(SessionStorageRedis::class.java)
private val sessionExpire = TinyApp.getConfig()["session.expire.seconds"]

class SessionStorageRedis: ISessionStorage {
	private var _storage = HashMap<String, String>()
	private var _redis: TinyRedis? = null
	private var _changed = false

	init {
		val redisName = "redis.default"
		try{
			_redis = TinyRegistry.get(redisName) as TinyRedis
		}catch(e: TinyException){
			logger.warn("${this::class.java.getSimpleName()} init error: can not found ${redisName} in TinyRegistry, please make sure Redis config[\"${redisName}\"] exists")
		}
	}

	override fun load(sessionId: String): HashMap<String, String>{
		if(_redis == null){
			return HashMap<String, String>()
		}
		return HashMap<String, String>()
	}

	override fun save(sessionId: String, data: HashMap<String, String>) {	
		if(_redis == null){
			return
		}
	}

	override fun touch(sessionId: String, data: HashMap<String, String>) {	
		if(_redis == null){
			return
		}
	}
}