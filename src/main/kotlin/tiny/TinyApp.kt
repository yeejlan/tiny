package tiny

import tiny.exception.TinyException
import java.util.TimeZone

object TinyApp{

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
	private lateinit var _config: TinyConfig

	fun init(strEnv: String, configFile: String){
		val env: Int? = _strEnvMapping.get(strEnv)
		if(env != null){
			_env = env
			_envString = strEnv
		}

		_config = TinyConfig(configFile)
		_isInit = true
	}

	fun getEnv(): Int{
		checkInit()
		return _env
	}

	fun getEnvString(): String{
		checkInit()
		return _envString
	}

	fun getConfig(): TinyConfig{
		checkInit()
		return _config
	}

	fun bootstrap(){
		checkInit()

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

	fun checkInit() {
		if(!_isInit) {
			throw TinyException("Please call TinyApp.init first")
		}
	}
}