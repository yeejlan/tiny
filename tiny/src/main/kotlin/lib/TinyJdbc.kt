package tiny.lib

import tiny.*
import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException
import java.sql.PreparedStatement
import java.sql.ResultSet
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(TinyJdbc::class.java)

class TinyJdbc {
	private val _datasource = ThreadLocal<DataSource?>()
	private val _sql = ThreadLocal<String>()

	fun use(dsName: String) {
		var ds: DataSource? = null
		try{
			ds = TinyRegistry["datasource." + dsName] as? DataSource
		}catch(e: TinyException){
			//pass
		}
		if(ds != null){
			setDataSource(ds)
		}
	}

	fun setDataSource(dataSource: DataSource) {
		_datasource.set(dataSource)
	}

	fun exec(body: (Connection) -> Any?): Any? {
		val ds = _datasource.get()
		if(ds == null){
			throw SQLException("DataSource is null")
		}
		var value: Any?
		var conn: Connection? = null
		try{
			conn = ds.getConnection()
			value = body(conn)
		}catch(e: Throwable){
			logger.error("Sql exec error on ${this}", e)
			throw e
		}finally{
			try{
				conn?.close()
			}catch(e: Throwable){
				logger.error("connnection close error", e)
			}
		}
		return value
	}

	fun queryForList(sql: String, paramMap: Map<String, *>?): List<Map<String, Any>>? {
		_sql.set(sql)
		val value = exec({ conn ->
			var stmt: PreparedStatement? = null
			var rs: ResultSet? = null
			try {
				stmt = conn.prepareStatement(sql)
				if(stmt == null){
					return@exec null
				}
				rs = stmt.executeQuery()
				if(rs == null){
					return@exec null
				}
				val resultList: MutableList<HashMap<String, Any>> = mutableListOf()
				val metaData = rs.getMetaData()
				val columnCount = metaData.getColumnCount()
				while(rs.next()){
					val resultMap = HashMap<String, Any>()
					for(i in 1..columnCount){
						resultMap.put(metaData.getColumnName(i), JdbcUtil.getRsValue(rs, i))
					}
					resultList.add(resultMap)
				}
				return@exec resultList
			}finally {
				try{
					rs?.close()
					stmt?.close()
				}catch(e: Throwable){
					logger.error("resource close error", e)
				}
			}
		})
		if(value == null){
			return listOf()
		}
		@Suppress("UNCHECKED_CAST")
		return value as List<Map<String, Any>>
	}

	fun queryForMap(sql: String, paramMap: Map<String, *>?): Map<String, Any> {
		_sql.set(sql)
		val value = exec({ conn ->
			var stmt: PreparedStatement? = null
			var rs: ResultSet? = null
			try {
				stmt = conn.prepareStatement(sql)
				if(stmt == null){
					return@exec null
				}
				rs = stmt.executeQuery()
				if(rs == null){
					return@exec null
				}
				val resultMap = HashMap<String, Any>()
				val metaData = rs.getMetaData()
				val columnCount = metaData.getColumnCount()
				while(rs.next()){
					for(i in 1..columnCount){
						resultMap.put(metaData.getColumnName(i), JdbcUtil.getRsValue(rs, i))
					}
					break
				}
				return@exec resultMap
			}finally {
				try{
					rs?.close()
					stmt?.close()
				}catch(e: Throwable){
					logger.error("resource close error", e)
				}
			}
		})
		if(value == null){
			return mapOf()
		}
		@Suppress("UNCHECKED_CAST")
		return value as Map<String, Any>
	}

	override fun toString(): String {
		return this::class.java.getSimpleName()+"[${_datasource.get()}, sql=${_sql.get()}]"
	}
}

