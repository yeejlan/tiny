package tiny

import com.fasterxml.jackson.module.kotlin.*
import com.fasterxml.jackson.databind.SerializationFeature

import tiny.TinyException

private val storage = HashMap<String, Any>()

object TinyRegistry{

	operator fun get(key: String) :Any {
		val v = storage.get(key)
		if(v == null){
			throw TinyException("Value not found for key: " + key)
		}
		return v
	}

	operator fun set(key: String, value: Any) {
		storage.put(key, value)
	}

	fun delete(key: String){
        storage.remove(key)
    }

	override fun toString(): String {

		val mapper = jacksonObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
		try {
			return mapper.writeValueAsString(storage);
		}catch (e: Throwable){
			return "";
		}
	}

}