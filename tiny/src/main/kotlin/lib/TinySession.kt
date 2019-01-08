package tiny.lib

import com.fasterxml.jackson.module.kotlin.*

private val supportedStorage = arrayOf("redis")
private val objectMapper = jacksonObjectMapper()
private lateinit var sessionStorage: SessionStorage

class TinySession {
	private val _storage = HashMap<String, String>()



	operator fun set(key: String, value: Any) {

		try {
			val valueStr =  objectMapper.writeValueAsString(value)
			_storage.put(key, valueStr)

		}catch (e: Throwable){
			throw e
		}
	}

	operator fun get(key: String): String {
		var value: String?
		
		value = _storage.get(key)
		if(value == null){
			return ""
		}
		return value
	}

	fun <T> get(key: String, valueType: Class<T> ): T? {
		try {
			val value = _storage.get(key)
			if(value == null){
				return null
			}
			return objectMapper.readValue(value, valueType)

		}catch (e: Throwable){
			throw e
		}
	}

	fun getInt(key: String, default: Int = 0): Int {

		if(key.isEmpty()){
			return default
		}
		return this.get(key).toIntOrNull() ?: default
	}

	fun getLong(key: String, default: Long = 0): Long {

		if(key.isEmpty()){
			return default
		}		
		return this.get(key).toLongOrNull() ?: default
	}

	fun getBoolean(key: String): Boolean {
		if(key.isEmpty()){
			return false
		}		
		return this.get(key).toBoolean()
	}

	override fun toString(): String {
		return _storage.toString()
	}
}