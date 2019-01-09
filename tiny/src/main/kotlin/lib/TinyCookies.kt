package tiny.lib

import com.fasterxml.jackson.module.kotlin.*
import tiny.*
import org.slf4j.LoggerFactory

private val objectMapper = jacksonObjectMapper()
private val logger = LoggerFactory.getLogger(TinySession::class.java)
private val cookieDomain = TinyApp.getConfig()["cookie.domain"]

class TinyCookies(cookieMap: HashMap<String, String>) {
	private var _cookies: HashMap<String, String>

	init{
		_cookies = cookieMap
	}

	override fun toString(): String {

		if(!_cookies.isEmpty()){
			val mapper = jacksonObjectMapper()
			try {

				return mapper.writeValueAsString(_cookies)

			}catch (e: Throwable){
				throw e
			}
		}

		return "{}"
	}

	operator fun get(key: String): String {
		var value: String?
		
		value = _cookies.get(key)
		if(value == null){
			return ""
		}
		return value
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

}