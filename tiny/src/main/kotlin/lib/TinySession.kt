package tiny.lib

import com.fasterxml.jackson.module.kotlin.*
import tiny.*
import tiny.lib.session.*
import org.slf4j.LoggerFactory
import com.fasterxml.jackson.databind.DeserializationFeature

private val objectMapper = jacksonObjectMapper()
private val logger = LoggerFactory.getLogger(TinySession::class.java)

private val sessionEnable = TinyApp.getConfig().getBoolean("session.enable")

class TinySession {
	private var _map = HashMap<String, Any>()
	private var _changed = false
	var sessionId: String = ""

	companion object{
		private val _sessionStorage = SessionStorage.get()
	}

	init{
		objectMapper.enable(DeserializationFeature.USE_LONG_FOR_INTS)
	}

	operator fun set(key: String, value: Any) {
		changed()
		_map.put(key, value)
	}

	operator fun <T> get(key: String): T {
		@Suppress("UNCHECKED_CAST")
		return _map.get(key) as T
	}

	fun delete(key: String) {
		changed()
		_map.remove(key)	
	}

	fun touch(){
		changed()
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
		if(sessionEnable && _sessionStorage != null){
			val valueStr = _sessionStorage.load(sessionId)
			if(valueStr.isEmpty()){
				return
			}
			try{
				_map = objectMapper.readValue<HashMap<String, Any>>(valueStr)
			}catch(e: Throwable){
				//pass
			}
		}
	}

	fun save() {
		if(!_changed){
			return
		}
		_changed = false
		if(sessionId.isEmpty()){
			return
		}
		if(sessionEnable && _sessionStorage != null){
			val valueStr =  objectMapper.writeValueAsString(_map)
			_sessionStorage.save(sessionId, valueStr)
		}
	}

	private fun changed(){
		_changed = true
	}

	override fun toString(): String {
		return _map.toString()
	}
}