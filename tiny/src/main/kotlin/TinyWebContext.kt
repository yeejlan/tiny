package tiny

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import com.fasterxml.jackson.module.kotlin.*
import tiny.lib.TinySession

class TinyWebContext(req: HttpServletRequest, res: HttpServletResponse) {

	/*HttpServletRequest*/
	var request: HttpServletRequest

	/*HttpServletResponse*/
	var response: HttpServletResponse

	/*hold exception when there is a internal server error*/
	var exception: Throwable? = null

	/*store request params, array params are NOT supported, for those please use request.getParameterValues()*/
	var params: TinyParams

	/*session support*/
	val session = TinySession()

	init{
		request = req
		response = res

		val paramMap: HashMap<String, String> = HashMap()
		var paramKeys = request.getParameterNames()
		for(paramKey in paramKeys){
			val param = request.getParameter(paramKey)
			paramMap.put(paramKey, param)
		}
		params = TinyParams(paramMap)
	}
}

class TinyParams(paramMap: HashMap<String, String>) {
	private var _params: HashMap<String, String>

	init{
		_params = paramMap
	}

	override fun toString(): String {

		if(!_params.isEmpty()){
			val mapper = jacksonObjectMapper()
			try {

				return mapper.writeValueAsString(_params)

			}catch (e: Throwable){
				throw e
			}
		}

		return "{}"
	}

	operator fun set(key: String, value: String) {
		_params.put(key, value)
	}

	operator fun get(key: String): String {
		var value: String?
		
		value = _params.get(key)
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
