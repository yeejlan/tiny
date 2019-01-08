package tiny


import tiny.TinyException
import java.io.File
import groovy.text.StreamingTemplateEngine
import groovy.text.Template
import org.apache.commons.io.IOUtils
import java.util.concurrent.ConcurrentHashMap
import groovy.lang.Writable
import java.io.InputStream
import java.io.FileInputStream

private val tplBasePath = "templates/"
private val tplSuffix = ".tpl"
private val tplCache: ConcurrentHashMap<String, Template> = ConcurrentHashMap()
private val devResourcesDir = TinyApp.getConfig()["dev.resources.dir"]
private val workDirectory = System.getProperty("user.dir")
private val helpers: HashMap<String, Any> = HashMap()

class TinyView{

	private var _model: HashMap<String, Any>
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
	fun render(tplPath : String): Writable{
		var template: Template?
		if(_useCache){
			template = tplCache.get(tplPath)
			if(template != null){
				val writable = template.make(_model)
				return writable
			}
		}

		var inputStream: InputStream?
		if(_useCache || devResourcesDir.isEmpty()){
			val tplFile = tplBasePath + tplPath + tplSuffix
			inputStream = this::class.java.classLoader.getResourceAsStream(tplFile)
			if(inputStream == null){
				throw TinyException("Read template file failed: " + tplFile)
			}
		}else{//development environment and devResourcesDir is set
			val tplFile = workDirectory + "/" + devResourcesDir + "/" + tplBasePath + tplPath + tplSuffix
			inputStream = FileInputStream(File(tplFile))
			if(inputStream == null){
				throw TinyException("Read template file failed: " + tplFile)
			}			
		}
		val text = IOUtils.toString(inputStream, "UTF-8")
		template = StreamingTemplateEngine().createTemplate(text)
		if(template != null){
			tplCache.put(tplPath, template)
		}
		val writable = template.make(_model)
		return writable
	}

	companion object{

		@JvmStatic fun addHelper(helper: String, clz: Any){
			helpers.put(helper, clz)
		}
	}
}
