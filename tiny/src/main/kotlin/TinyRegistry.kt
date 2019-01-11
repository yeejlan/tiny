package tiny

import tiny.TinyException

private val storage = HashMap<String, Any>()

object TinyRegistry{

	@JvmStatic operator fun get(key: String) :Any? {
		val value = storage.get(key)
		if(value == null){
			throw TinyException("Key not found: " + key)
		}
		return value
	}

	@JvmStatic operator fun set(key: String, value: Any) {
		storage.put(key, value)
	}

	@JvmStatic fun delete(key: String){
		storage.remove(key)
	}

	override fun toString(): String {

		return storage.toString()
	}

}