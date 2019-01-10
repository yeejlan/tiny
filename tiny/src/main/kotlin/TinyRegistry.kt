package tiny

import tiny.TinyException

private val storage = HashMap<String, Any>()

object TinyRegistry{

	@JvmStatic operator fun get(key: String) :Any {
		val v = storage.get(key)
		if(v == null){
			throw TinyException("Value not found for key: " + key)
		}
		return v
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