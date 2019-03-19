package tiny

import tiny.TinyException

object TinyRegistry{

	private val storage = HashMap<String, Any>()

	@JvmStatic operator fun <T> get(key: String): T {
		val value = storage.get(key)
		if(value == null){
			throw TinyException("[TinyRegistry] key not found: " + key)
		}
		@Suppress("UNCHECKED_CAST")
		return value as T
	}

	@JvmStatic operator fun set(key: String, value: Any) {
		storage.put(key, value)
	}

	@JvmStatic fun delete(key: String){
		storage.remove(key)
	}

	@JvmStatic fun getStorage() : Map<String, Any>{
		return storage.toSortedMap(compareBy { it })
	}

	override fun toString(): String {

		return getStorage().toString()
	}

}