package tiny

import tiny.exception.TinyException
import groovy.text.StreamingTemplateEngine
import groovy.text.Template
import org.apache.commons.io.IOUtils
import java.util.concurrent.ConcurrentHashMap

private val tplBasePath = "templates/"
private val tplSuffix = ".tpl"
private val tplCache: ConcurrentHashMap<String, Template> = ConcurrentHashMap()

private val helpers: HashMap<String, Any> = HashMap()

class TinyView{

	var _model: HashMap<String, Any>
	private var _useCache = true

	constructor() {
		val env = TinyApp.getEnv()
		if(env == TinyApp.DEVELOPMENT) {
			_useCache = false
		}

		_model = helpers
		_model.put("view", this)
	}

	operator fun set(key: String, value: Any) {
		_model.put(key, value)
	}

	operator fun get(key: String): Any {
		return _model.get(key) ?: Any()
	}

	/*
	* render a template, for example render('common/header') will lookup /templates/common/header.tpl
	*/
	fun render(tplPath : String): String{
		var template: Template?
		if(_useCache && tplCache.containsKey(tplPath)){
			template = tplCache.get(tplPath)
			if(template != null){
				val make = template.make(_model)
				return make.toString()
			}
		}

		val tplFile = tplBasePath + tplPath + tplSuffix
		val inputStream = this::class.java.classLoader.getResourceAsStream(tplFile)
		if(inputStream == null){
			throw TinyException("Read template file failed: " + tplFile)
		}
		val text = IOUtils.toString(inputStream, "UTF-8")
		template = StreamingTemplateEngine().createTemplate(text)
		if(template != null){
			tplCache.put(tplPath, template)
		}
		val make = template.make(_model)
		return make.toString()
	}

	companion object{
		/*
		* load helper from package path
		*/
		fun loadHelpers(pkg: String) {

		}
	}
}

/*
val fullClassName = "com.somepackage.ApiService"
val cls = Class.forName(fullClassName)
val kotlinClass = cls.kotlin
cls.getMethod("getSomeApi").invoke(kotlinClass.objectInstance)
*/