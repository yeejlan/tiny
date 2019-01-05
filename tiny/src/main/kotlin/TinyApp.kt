package tiny

import tiny.TinyException
import java.util.TimeZone
import javax.servlet.Servlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler

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

	@JvmStatic fun init(strEnv: String, configFile: String){
		val env: Int? = _strEnvMapping.get(strEnv)
		if(env != null){
			_env = env
			_envString = strEnv
		}

		_configFile = configFile
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

	@JvmStatic fun runJetty(clz: Class<out Servlet>, port: Int = 8080){
		val server = Server(port)

		val context = ServletContextHandler()
		context.setContextPath("/")
		context.addServlet(clz, "/*")
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