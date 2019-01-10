package tiny.lib

import com.fasterxml.jackson.module.kotlin.*

interface ISessionStorage {

	fun load(sessionId: String): String
	fun save(sessionId: String, data: String)
}

object SessionStorage {
	private val _storageSupported = arrayOf("redis")
	private var _sessionStorage: ISessionStorage? = null

	@JvmStatic fun set(storage: ISessionStorage) {
		_sessionStorage = storage
	}

	@JvmStatic fun get(): ISessionStorage? {
		return _sessionStorage
	}

	@JvmStatic fun getStorageSupported(): Array<String>{
		return _storageSupported
	}
}