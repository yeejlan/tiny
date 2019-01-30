package tiny

import tiny.TinyException

object TinyRegistry{

	val storage = HashMap<String, Any>()

	inline operator fun <reified T: Any> get(key: String): T {
		val value = storage.get(key)
		if(value == null){
			throw TinyException("[TinyRegistry] key not found: " + key)
		}
		return value as T
	}

	@JvmStatic fun <T: Any> get(key: String, valueType: Class<T> ): T {
		val value = storage.get(key)
		if(value == null){
			throw TinyException("[TinyRegistry] key not found: " + key)
		}
		return valueType.cast(value)
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