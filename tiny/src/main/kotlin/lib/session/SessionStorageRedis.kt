package tiny.lib.session

import tiny.*
import tiny.lib.TinyRedis
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(SessionStorageRedis::class.java)
private val sessionExpire = TinyApp.getConfig().getLong("session.expire.seconds", 3600)
private val storageProvider = TinyApp.getConfig()["session.storage.provider"]

class SessionStorageRedis: ISessionStorage {
	private var _tinyRedis: TinyRedis? = null

	init {
		val redisName = storageProvider
		try{
			_tinyRedis = TinyRegistry.get(redisName) as TinyRedis
		}catch(e: TinyException){
			logger.warn("${this::class.java.getSimpleName()} init error: can not found ${redisName} in TinyRegistry, please make sure Redis config[\"${redisName}\"] exists")
		}
	}

	override fun load(sessionId: String): String{
		return _tinyRedis?.get(sessionId) ?: ""
	}

	override fun save(sessionId: String, data: String) {
		_tinyRedis?.set(sessionId, data, sessionExpire)
	}
}