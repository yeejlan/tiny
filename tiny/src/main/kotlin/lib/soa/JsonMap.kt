package tiny.lib.soa

import com.fasterxml.jackson.module.kotlin.*

class JsonMap {

	var _jsonMap: Map<String, Any?> = mapOf()
	var _value: Any? = null

	constructor(jsonString: String?) {
		if(jsonString == null) {
			return
		}
		try{
			val mapper = jacksonObjectMapper()
			_jsonMap = mapper.readValue<HashMap<String, Any?>>(jsonString)

		}catch(e: Throwable) {
			//pass
		}
	}

	constructor(map: Map<String, Any?>) {
		_jsonMap = map
	}

	constructor(any: Any?) {
		_value = any
	}

	fun getMap(): Map<String, Any?> {
		return _jsonMap
	}


	operator fun get(pathStr: String): JsonMap {
		var value: Any? = null
		var key: String

		val keyList = pathStr.split(".")

		if(keyList.size < 1) {
			return JsonMap(value)
		}

		value = _jsonMap.get(keyList[0])
		for(i in keyList.indices) {
			if(i == 0){
				continue
			}
			key = keyList[i]
			if(value is LinkedHashMap<*, *>){
				value = value.get(key)
			}else{
				value = null
			}
			if(value == null) {
				break
			}
		}

		if(value != null && value is Map<*, *>){
			@Suppress("UNCHECKED_CAST")
			val map = value as Map<String, Any?>
			return JsonMap(map)
		}

		return JsonMap(value)
	}


	override fun toString(): String {

		if(!_jsonMap.isEmpty()){
			val mapper = jacksonObjectMapper()
			try {

				return mapper.writeValueAsString(_jsonMap)

			}catch (e: Throwable){
				return ""
			}
		}

		return _value.toString()
	}

	fun toInt(default: Int = 0): Int {
		return _value?.toString()?.toIntOrNull() ?: default
	}

	fun toLong(default: Long = 0): Long {
		return _value?.toString()?.toLongOrNull() ?: default
	}
}