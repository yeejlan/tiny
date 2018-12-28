package tiny

import com.fasterxml.jackson.module.kotlin.*
import com.fasterxml.jackson.databind.SerializationFeature

private val storage = HashMap<String, Any?>()

object Registry{

	operator fun get(key: String) :Any? {
		return storage.get(key)
	}

	operator fun set(key: String, value: Any?) {
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