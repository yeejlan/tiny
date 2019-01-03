package tiny

import com.fasterxml.jackson.module.kotlin.*
import com.fasterxml.jackson.databind.SerializationFeature

import java.util.Properties
import java.io.InputStreamReader
import tiny.TinyException

class TinyConfig{

	var _configMap: Map<String, String> = mapOf()

	constructor(configFile: String) {

		val properties = Properties()
		val inputStream = this::class.java.classLoader.getResourceAsStream(configFile)
		if(inputStream == null){
			throw TinyException("Read config file failed: " + configFile)
		}
		val reader = InputStreamReader(inputStream, "UTF-8")
		properties.load(reader)

		@Suppress("UNCHECKED_CAST")
		_configMap = properties as Map<String, String>

	}

	override fun toString(): String {

		if(!_configMap.isEmpty()){
			val mapper = jacksonObjectMapper()
			try {

				return mapper.writeValueAsString(_configMap)

			}catch (e: Throwable){
				throw e
			}
		}

		return "{}"
	}

	operator fun get(pathStr: String): String {
		var value: String?
		
		value = _configMap.get(pathStr)
		if(value == null){
			return ""
		}
		return value
	}

	fun getInt(pathStr: String, default: Int = 0): Int {

		if(pathStr.isEmpty()){
			return default
		}
		return this.get(pathStr).toIntOrNull() ?: default
	}

	fun getLong(pathStr: String, default: Long = 0): Long {

		if(pathStr.isEmpty()){
			return default
		}		
		return this.get(pathStr).toLongOrNull() ?: default
	}

	fun getBoolean(pathStr: String): Boolean {
		if(pathStr.isEmpty()){
			return false
		}		
		return this.get(pathStr).toBoolean()
	}

}