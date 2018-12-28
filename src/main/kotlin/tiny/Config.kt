package tiny

import com.fasterxml.jackson.module.kotlin.*
import com.fasterxml.jackson.databind.SerializationFeature

import java.util.Properties

class Config{

	var _configMap: Map<String, Any?> = mapOf()
	var _value: Any? = null

	constructor(configFile: String) {
	
	}
}