package tiny.lib

import tiny.*

class SessionStorageRedis: SessionStorage {
	private var _storage = HashMap<String, String>()
	private var _redis: TinyRedis
	private var _changed = false

	init {
		val redisName = "redis.aadefault"
		try{
			_redis = TinyRegistry.get(redisName) as TinyRedis
		}catch(e: TinyException){
			throw TinyException("${this::class.java.getSimpleName()} init error: can not found ${redisName} in TinyRegistry")
		}
	}

	override fun load() {

	}

	override fun save() {
		if(!_changed){
			return
		}

	}

	override fun get(key: String): String? {
		return _storage.get(key)
	}

	override fun set(key: String, value: String) {
		_storage.put(key, value)
	}

	override fun delete(key: String) {
		_storage.remove(key)
	}

	override fun clean() {
		_storage = HashMap<String, String>()
		save()
	}
}