package tiny


import tiny.TinyException
import java.io.File
import groovy.text.StreamingTemplateEngine
import groovy.text.Template
import org.apache.commons.io.IOUtils
import java.util.concurrent.ConcurrentHashMap

private val tplBasePath = "templates/"
private val tplSuffix = ".tpl"
private val tplCache: ConcurrentHashMap<String, Template> = ConcurrentHashMap()

private val helpers: HashMap<String, Any> = HashMap()

fun addHelper(helper: String, clz: Class<Any>){
	helpers.put(helper, clz)
}

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
		if(_useCache){
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
		@JvmStatic fun loadHelpers(pkgName: String) {
			val pkgPath = pkgName.replace('.', '/')
			val classpath = this::class.java.classLoader.getResource(pkgPath)
			if(classpath == null){
				return
			}
			val basePath = classpath.getPath()
			val baseDir = File(basePath)
			if (!baseDir.exists() || !baseDir.isDirectory()) {
				return
			}
			val helperFiles = baseDir.list()
			for(helper in helperFiles){
				if(helper.endsWith("Helper.class")){
					val clzName = helper.replace(".class", "")
					val fullClzName = pkgName + "." + clzName
					val clz = Class.forName(fullClzName)
					helpers.put(clzName, clz.newInstance())
				}
			}
		}
	}
}
