package tiny.lib

import com.fasterxml.jackson.module.kotlin.*
import com.fasterxml.jackson.databind.SerializationFeature

private val objectMapper = jacksonObjectMapper()

object TinyProfiler {
	private val _profile = ThreadLocal<Boolean>()
	private val _profileMap = ThreadLocal<MutableList<MethodProfiler>>()

	init{
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT)
		objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
	}

	/*profile enabled or not*/
	fun enabled(): Boolean{
		val p = _profile.get()
		if(p == null || p == false){
			return false
		}
		return true
	}

	/*enable profile*/
	fun enable() {
		_profile.set(true)
	}

	/*disable profile*/
	fun disable() {
		_profile.set(false)
	}

	/*add a MethodProfiler to profileMap*/
	fun add(mp: MethodProfiler) {
		val map = _profileMap.get()
		if(map == null) {
			return
		}
		map.add(mp)
	}

	/*get profileMap*/
	fun get(): MutableList<MethodProfiler>? {
		return _profileMap.get()
	}

	/*initial profileMap*/
	fun init() {
		_profileMap.set(mutableListOf<MethodProfiler>())
	}

	override fun toString(): String {
		return objectMapper.writeValueAsString(_profileMap.get())
	}
}

data class MethodProfiler(val method: String, val params: HashMap<String, Any?>, val costMilliseconds: Long)