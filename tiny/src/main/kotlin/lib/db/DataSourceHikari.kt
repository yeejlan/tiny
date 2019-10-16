package tiny.lib.db

import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariConfig
import tiny.*

private val defauleConnectionTimeout = 3000L
private val defaultMaximumPoolSize = 10
private val defauleMinimumIdle = 1

class DataSourceHikariConfig() {
	var driver: String = ""
	lateinit var url: String
	lateinit var username: String
	lateinit var password: String
	var poolName: String = ""
	var maximumPoolSize = defaultMaximumPoolSize
	var minimumIdle = defauleMinimumIdle

	private val _config = HikariConfig()

	init{
		_config.setConnectionTimeout(defauleConnectionTimeout)
	}

	fun getConfig(): HikariConfig{
		return _config
	}

	fun doConfig(): DataSourceHikariConfig{
		if(minimumIdle > maximumPoolSize){
			minimumIdle = maximumPoolSize
		}
		if(!driver.isEmpty()){
			_config.setDriverClassName(driver)
		}
		if(!poolName.isEmpty()){
			_config.setPoolName(poolName)
		}
		_config.setJdbcUrl(url)
		_config.setUsername(username)
		_config.setPassword(password)
		_config.setMaximumPoolSize(maximumPoolSize)
		_config.setMinimumIdle(minimumIdle)
		return this
	}

	fun doConfig(config: TinyConfig, prefix: String): DataSourceHikariConfig{
		driver = config["${prefix}.driver"]
		url = config["${prefix}.url"]
		username = config["${prefix}.username"]
		password = config["${prefix}.password"]
		poolName = prefix

		maximumPoolSize = config.getInt("${prefix}.hikari.maximumPoolSize", defaultMaximumPoolSize)
		minimumIdle = config.getInt("${prefix}.hikari.minimumIdle", defauleMinimumIdle)

		return doConfig()
	}

	fun doFixedPoolConfig(poolSize: Int): DataSourceHikariConfig{
		_config.setMaximumPoolSize(poolSize)
		_config.setMinimumIdle(poolSize)
		return this
	}
}

class DatasourceHiKari{
	private lateinit var _datasource: HikariDataSource

	fun create(config: DataSourceHikariConfig): HikariDataSource {
		_datasource = HikariDataSource(config.getConfig())

		TinyApp.addShutdownHook(DataSourceShutdownHook())
		return _datasource
	}

	private inner class DataSourceShutdownHook() : TinyShutdownHook {

		override fun shutdownProcess() {
			_datasource.close()
		}
	}
}