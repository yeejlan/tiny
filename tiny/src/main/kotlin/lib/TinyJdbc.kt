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
			var stmt = JdbcUtil.createPreparedStatement(conn, sql, paramMap ?: mapOf())

			var rs: ResultSet?
			stmt.use{
				rs = stmt.executeQuery()
				var value: T? = null
				rs.use{
					value = body(rs as ResultSet)
				}
				return@exec value
			}
		})

		return exeValue
	}

	fun insert(sql: String, paramMap: Map<String, Any>, returnAutoGenKey: Boolean = false) : Long {
		_sql.set(sql)
		val insertId = exec<Long>({ conn ->
			var stmt = JdbcUtil.createPreparedStatement(conn, sql, paramMap, returnAutoGenKey)
			var generatedId = 0L
			stmt.use{
				stmt.executeUpdate()
				if(returnAutoGenKey){
					val rs = stmt.getGeneratedKeys()
					rs.use{
						if(rs.next()){
							generatedId = rs.getLong(1)
						}
					}
				}
				return@exec generatedId
			}
		})
		return insertId ?: 0L
	}

	fun update(sql: String, paramMap: Map<String, Any>) : Int {
		_sql.set(sql)
		val updateRows = exec<Int>({ conn ->
			var stmt = JdbcUtil.createPreparedStatement(conn, sql, paramMap)
			var rows = 0
			stmt.use{
				rows = stmt.executeUpdate()
			}
			return@exec rows
		})
		return updateRows ?: 0
	}

	fun batchQuery (sql: String, paramList: List<Map<String, Any>>): Unit {
		_sql.set(sql)
		exec<Any?>({ conn ->
			var stmt = JdbcUtil.createBatchPreparedStatement(conn, sql, paramList)
			stmt.use{
				stmt.executeBatch()
			}
		})
	}

	fun queryForList(sql: String, paramMap: Map<String, Any>? = null): List<Map<String, Any>> {

		val value = query<List<Map<String, Any>>>(sql, paramMap, { rs ->
			val value = JdbcUtil.rsToList(rs)
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

