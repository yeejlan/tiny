package tiny.lib

import com.fasterxml.jackson.module.kotlin.*
import tiny.*
import org.slf4j.LoggerFactory

private val objectMapper = jacksonObjectMapper()
private val logger = LoggerFactory.getLogger(TinySession::class.java)

class TinySession {
	private var _map = HashMap<String, Any>()
	private var _changed = false
	var sessionId: String = ""

	companion object{
		private val _sessionStorage = SessionStorage.get()
	}

	operator fun set(key: String, value: Any) {
		changed()
		_map.put(key, value)
	}

	operator fun get(key: String): Any? {
		return _map.get(key)
	}

	fun delete(key: String) {
		changed()
		_map.remove(key)	
	}

	fun destroy() {
		changed()
		_map.clear()
		save()
	}

	fun load() {
		if(sessionId.isEmpty()){
			return
		}		
		if(_sessionStorage != null){
			val valueStr = _sessionStorage.load(sessionId)
			if(valueStr.isEmpty()){
				return
			}
			try{
				_map = objectMapper.readValue<HashMap<String, Any>>(valueStr)
			}catch(e: Throwable){
				throw e
			}
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
			val valueStr =  objectMapper.writeValueAsString(_map)
			_sessionStorage.save(sessionId, valueStr)
		}
	}

	fun touch(){
		if(sessionId.isEmpty()){
			return
		}		
		if(_sessionStorage != null){
			val valueStr =  objectMapper.writeValueAsString(_map)
			_sessionStorage.touch(sessionId, valueStr)
		}
	}

	fun changed(){
		_changed = true
	}

	override fun toString(): String {
		return _map.toString()
	}
}