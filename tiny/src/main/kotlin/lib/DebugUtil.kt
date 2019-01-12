package tiny.lib

import com.fasterxml.jackson.module.kotlin.*
import com.fasterxml.jackson.databind.SerializationFeature
import kotlin.io.print as p
import kotlin.io.println as pn

private val objectMapper = jacksonObjectMapper()

object DebugUtil {
	private val tab = "    "

	init{
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT)
		objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
	}

	@JvmStatic fun pprint(obj: Any) {
		pn(obj::class.java.getName() + " "+ objectMapper.writeValueAsString(obj))
	}

	@JvmStatic fun inspect(value: Any?) {
		print(value, inspect = true)
	}

	@JvmStatic fun inspect(map: Map<*, *>) {
		print(map, inspect = true)
	}

	@JvmStatic fun inspect(list: List<*>) {
		print(list, inspect = true)
	}

	@JvmStatic fun inspect(set: Set<*>) {
		print(set, inspect = true)
	}

	@JvmStatic fun print(value: Any?, inspect: Boolean = false) {
		if(inspect){
			pn("$value - " + value?.let{it::class.java.getName()})
		}else{
			pn(value)
		}
	}

	@JvmStatic fun print(map: Map<*, *>, i: Int = 0, inspect: Boolean = false) {
		if(inspect){
			pn(map::class.java.getName())
		}else{
			pn(map::class.java.getSimpleName())
		}
		pn("${tab.repeat(i)}{")
		map.forEach { (key, value) -> 
			p("${tab.repeat(i+1)}[$key] => ")
			if(value is Map<*, *>){
				print(value, i+1, inspect)
			}
			else if(value is List<*>){
				print(value, i+1, inspect)
			}
			else if(value is Set<*>){
				print(value, i+1, inspect)
			}
			else{
				print(value, inspect)
			}
		}
		pn("${tab.repeat(i)}}")
	}

	@JvmStatic fun print(list: List<*>, i: Int = 0, inspect: Boolean = false) {
		if(inspect){
			pn(list::class.java.getName())
		}else{
			pn(list::class.java.getSimpleName())
		}
		pn("${tab.repeat(i)}{")
		list.forEachIndexed { idx, value -> 
			p("${tab.repeat(i+1)}[${idx}] => ") 
			if(value is Map<*, *>){
				print(value, i+1, inspect)
			}
			else if(value is List<*>){
				print(value, i+1, inspect)
			}
			else if(value is Set<*>){
				print(value, i+1, inspect)
			}
			else{
				print(value, inspect)
			}
		}
		pn("${tab.repeat(i)}}")
	}

	@JvmStatic fun print(set: Set<*>, i: Int = 0, inspect: Boolean = false) {
		if(inspect){
			pn(set::class.java.getName())
		}else{
			pn(set::class.java.getSimpleName())
		}
		pn("${tab.repeat(i)}{")
		set.forEachIndexed { idx, value -> 
			p("${tab.repeat(i+1)}[${idx}] => ") 
			if(value is Map<*, *>){
				print(value, i+1, inspect)
			}
			else if(value is List<*>){
				print(value, i+1, inspect)
			}
			else if(value is Set<*>){
				print(value, i+1, inspect)
			}
			else{
				print(value, inspect)
			}
		}
		pn("${tab.repeat(i)}}")
	}

}