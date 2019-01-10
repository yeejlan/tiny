package tiny

import tiny.TinyException
import java.util.TimeZone
import javax.servlet.Servlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.webapp.WebAppContext
import java.util.concurrent.ConcurrentLinkedQueue
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.util.StatusPrinter
import java.io.File

import tiny.lib.TinyResourceLoader

private val shutdownQueue = ConcurrentLinkedQueue<TinyShutdownHook>()

private val logger = LoggerFactory.getLogger(TinyApp::class.java)

interface TinyShutdownHook{
	fun shutdownProcess()
}

object TinyApp {

	@JvmField val PRODUCTION = 10
	@JvmField val STAGING = 20
	@JvmField val TESTING = 30
	@JvmField val DEVELOPMENT = 40

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
	private lateinit var _appName: String

	@JvmStatic fun init(strEnv: String, appName: String){
		_appName = appName
		val env: Int? = _strEnvMapping.get(strEnv)
		if(env != null){
			_env = env
			_envString = strEnv
		}
		_logbackInit()
		if(env == null){
			logger.warn("Unknown env: ${strEnv}, fallback to ${_envString}")
		}

		_configFile = "config/${_envString}/${appName}.ini"
		_config = TinyConfig(_configFile)
		_settle()
		logger.info("App[${_appName}] starting with env=${_envString} and config=${_configFile}")
		_isInit = true
		TinyResourceLoader().load()
	}

	@JvmStatic fun shutdown(){
		var hookObject =  shutdownQueue.poll()
		var hookCount = 0
		while(hookObject != null){
			try {
				hookCount++
				hookObject.shutdownProcess()
			} catch (e: Throwable) {
				logger.warn("Shutdown process error: " + e)
			}
			hookObject = shutdownQueue.poll()
		}
		try {
			Thread.sleep(1000) //wait shutdown cleanup
		} catch (e: InterruptedException) {
			//pass
		}
		logger.info("App[${_appName}] stoped with ${hookCount} shutdown hook[s] processed")
	}

	@JvmStatic fun addShutdownHook(hook: TinyShutdownHook){
		shutdownQueue.add(hook)
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

	@JvmStatic fun getAppName(): String{
		_checkInit()
		return _appName
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

	private fun _logbackInit(){
		val context = LoggerFactory.getILoggerFactory() as LoggerContext
		try {
			val configurator = JoranConfigurator()
			configurator.setContext(context)
			context.reset()
			val configFile = "config/${_envString}/logback.xml"
			val resourceUrl = this::class.java.classLoader.getResource(configFile)
			if(resourceUrl == null){
				throw TinyException("Logback config is missing: " + configFile)
			}
			configurator.doConfigure(File(resourceUrl.toURI()))
		}catch (e: JoranException){
			//pass
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context)
	}

	@JvmStatic fun runJetty(port: Int = 8080){
		val server = Server(port)

		val context = WebAppContext()
		context.setContextPath("/")
		context.setResourceBase("./src/main")
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