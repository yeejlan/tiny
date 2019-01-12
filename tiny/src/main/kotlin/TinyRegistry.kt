package tiny

import tiny.TinyException

object TinyRegistry{

	private val storage = HashMap<String, Any>()

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

	@JvmStatic fun getStorage() : Map<String, Any>{
		return storage
	}

	override fun toString(): String {

		return storage.toString()
	}

}