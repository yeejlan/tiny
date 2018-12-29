package tiny

import groovy.text.StreamingTemplateEngine
import java.util.concurrent.ConcurrentHashMap

private val tplBasePath = "templates/"
private val tplSuffix = ".tpl"
private val tplCache: ConcurrentHashMap<String, String> = ConcurrentHashMap()

class TinyView{

	val _model: Map<String, Any> = mapOf()

	fun render(tplPath : String): String{
		return ""
	}
}