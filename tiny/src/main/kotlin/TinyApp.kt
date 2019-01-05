package tiny

import tiny.TinyException
import java.util.TimeZone
import javax.servlet.Servlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.webapp.WebAppContext

object TinyApp {

	val PRODUCTION = 10
	val STAGING = 20
	val TESTING = 30
	val DEVELOPMENT = 40

	private val _envStrMapping: Map<Int, String> = mapOf(
		PRODUCTION to "production",
		STAGING to "staging",
		TESTING to "testing",
		DEVELOPMENT to "development"
	)

	private val _strEnvMapping = _envStrMapping.entries.associateBy({ it.value }) { it.key }
	
	private var _isInit = false
	private var _env = PRODUCTION
	private var _envString = _envStrMapping.get(_env) ?: "production"
	private var _configFile: String = ""
	private lateinit var _config: TinyConfig

	@JvmStatic fun init(strEnv: String, appName: String){
		val env: Int? = _strEnvMapping.get(strEnv)
		if(env != null){
			_env = env
			_envString = strEnv
		}

		_configFile = "config/${_envString}/${appName}.properties"
		_config = TinyConfig(_configFile)
		_settle()
		_isInit = true
	}

	@JvmStatic fun bootstrap(clz: TinyBootstrap) {
		_checkInit()
		clz.bootstrap()
	}

	@JvmStatic fun getEnv(): Int{
		_checkInit()
		return _env
	}

	@JvmStatic fun getEnvString(): String{
		_checkInit()
		return _envString
	}

	@JvmStatic fun getConfig(): TinyConfig{
		_checkInit()
		return _config
	}

	private fun _settle(){
		//set timezone
		if(_config["timezone"].isEmpty()){
			throw TinyException("""Please set "timezone" in config file""")
		}
		TimeZone.setDefault(TimeZone.getTimeZone(_config["timezone"]))

		//set log path
		if(_config["log.path"].isEmpty()){
			throw TinyException("""Please set "log.path" in config file""")
		}
		TinyLog.init(_config["log.path"])

	}

	@JvmStatic fun runJetty(port: Int = 8080){
		val server = Server(port)

		val context = WebAppContext()
		context.setContextPath("/")
		context.setResourceBase("./webcontent")
		context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*/classes/.*")
		context.setConfigurations(arrayOf(
			org.eclipse.jetty.annotations.AnnotationConfiguration(),
			org.eclipse.jetty.webapp.WebInfConfiguration(), 
			org.eclipse.jetty.webapp.WebXmlConfiguration(),
			org.eclipse.jetty.webapp.MetaInfConfiguration(), 
			org.eclipse.jetty.webapp.FragmentConfiguration(), 
			org.eclipse.jetty.plus.webapp.EnvConfiguration(),
			org.eclipse.jetty.plus.webapp.PlusConfiguration(), 
			org.eclipse.jetty.webapp.JettyWebXmlConfiguration()	
		))	
		server.setHandler(context)
		
		server.start()
		server.join()
	}

	private fun _checkInit() {
		if(!_isInit) {
			throw TinyException("App init error, env: ${_envString}, config: ${_configFile}")
		}
	}
}