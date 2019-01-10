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

	fun execWithConn(body: (Connection) -> Any?): Any? {
		val ds = _datasource.get()
		if(ds == null){
			throw SQLException("DataSource is null")
		}
		var value: Any?
		var conn: Connection? = null
		try{
			conn = ds.getConnection()
			value = body(conn)
		}finally{
			try{
				conn?.close()
			}catch(e: Throwable){
				logger.error("conn close error", e)
			}
		}
		return value
	}

	fun exec(sql: String): Any? {
		_sql.set(sql)
		val value = execWithConn({ conn ->
			lateinit var stmt: PreparedStatement
			var rs: ResultSet? = null
			try {
				stmt = conn.prepareStatement(sql)
				rs = stmt.executeQuery()
				println("===rs = " + rs)
				while (rs?.next() ?: false){
					println(rs)
				}
			}finally {
				try{
					rs?.close()
					stmt.close()
				}catch(e: Throwable){
					logger.error("resource close error", e)
				}
			}
		})
		return value
	}

	fun queryForMap(sql: String, paramMap: Map<String, *>?): Map<String, Any> {
		_sql.set(sql)
		val value = execWithConn({ conn ->
			lateinit var stmt: PreparedStatement
			lateinit var rs: ResultSet
			try {
				stmt = conn.prepareStatement(sql)
				rs = stmt.executeQuery()
				val resultList: MutableList<HashMap<String, String>> = mutableListOf()
				val resultMap = HashMap<String, String>()
				val metaData = rs.getMetaData()
				val columnCount = metaData.getColumnCount()
				while(rs.next()){
					for(i in 1..columnCount){
						resultMap.put(metaData.getColumnName(i), rs.getString(i))
					}
					resultList.add(resultMap)
				}
				println(resultList)
				resultMap
			}finally {
				try{
					rs?.close()
					stmt.close()
				}catch(e: Throwable){
					logger.error("resource close error", e)
				}
			}
		})
		return value as Map<String, Any>
	}

	override fun toString(): String {
		return this::class.java.getSimpleName()+"[$_datasource, sql=$_sql]"
	}
}