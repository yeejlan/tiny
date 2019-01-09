package tiny.lib

import com.fasterxml.jackson.module.kotlin.*
import tiny.*
import org.slf4j.LoggerFactory

private val objectMapper = jacksonObjectMapper()
private val logger = LoggerFactory.getLogger(TinySession::class.java)

class TinySession {
	private var _map = HashMap<String, String>()
	private var _changed = false
	var sessionId: String = ""

	companion object{
		private val _sessionStorage = SessionStorage.get()
	}

	operator fun set(key: String, value: Any) {
		_changed = true
		try {
			val valueStr =  objectMapper.writeValueAsString(value)
			_map.put(key, valueStr)

		}catch (e: Throwable){
			throw e
		}
	}

	operator fun get(key: String): String {
		var value: String?
		
		value = _map.get(key)
		if(value == null){
			return ""
		}
		return value
	}

	fun delete(key: String) {
		_changed = true
		_map.remove(key)	
	}

	fun destroy() {
		_changed = true
		_map.clear()
		save()
	}

	fun load() {
		if(_sessionStorage != null){
			_map = _sessionStorage.load(sessionId)
		}		
	}

	fun save() {
		if(!_changed){
			return
		}
		if(sessionId.isEmpty()){
			return
		}
		if(_sessionStorage != null){
			_sessionStorage.save(sessionId, _map)
		}
	}

	fun touch(){
		if(_sessionStorage != null){
			_sessionStorage.touch(sessionId, _map)
		}
	}	

	fun <T> get(key: String, valueType: Class<T> ): T? {
		try {
			val value = _map.get(key)
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
		return _map.toString()
	}
}