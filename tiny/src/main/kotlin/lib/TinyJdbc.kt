package tiny.lib.db

import tiny.*
import tiny.lib.db.*
import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException
import java.sql.PreparedStatement
import java.sql.ResultSet
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(TinyJdbc::class.java)

class TinyJdbc(ds: DataSource) {
	private var _datasource: DataSource
	private val _sql = ThreadLocal<String>()

	init{
		_datasource = ds
	}

	fun <T> exec(body: (Connection) -> T?): T? {
		var value: T? = null
		var conn = _datasource.getConnection()
		conn.use{
			try{
				value = body(conn)
			}catch(e: SQLException){
				logger.error("SQL error on ${this} " + e)
				throw e
			}
		}
		return value
	}

	fun <T> query(sql: String, paramMap: Map<String, Any>?, body: (ResultSet) -> T?) : T? {
		_sql.set(sql)

		val exeValue = exec<T>({ conn ->
			var stmt = conn.prepareStatement(sql)
			var rs: ResultSet? = null
			stmt.use{
				rs = stmt.executeQuery()
			}
			if(rs == null){
				return@exec null
			}
			println("===333")
			var value: T? = null
			rs.use{
				value = body(rs as ResultSet)
				println("===2222")
			}
			value
		})

		return exeValue
	}

	fun queryForList(sql: String, paramMap: Map<String, Any>? = null): List<Map<String, Any>> {

		val value = query<List<Map<String, Any>>>(sql, paramMap, { rs ->
			val value = JdbcUtil.rsToList(rs)
			println("===1111")
			return@query value
		})
		return value ?: listOf()
	}

	fun queryForMap(sql: String, paramMap: Map<String, Any>? = null): Map<String, Any> {

		val value = query<Map<String, Any>>(sql, paramMap, { rs ->
			JdbcUtil.rsToMap(rs)
		})
		return value ?: mapOf()
	}

	override fun toString(): String {
		return this::class.java.getSimpleName()+"[${_sql.get()} , ${_datasource}]"
	}
}

