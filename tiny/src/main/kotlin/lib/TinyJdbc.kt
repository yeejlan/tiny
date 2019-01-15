package tiny.lib.db

import tiny.*
import tiny.lib.db.*
import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException
import java.sql.PreparedStatement
import java.sql.Statement
import java.sql.ResultSet
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(TinyJdbc::class.java)

class TinyJdbc(ds: DataSource) {
	private var _datasource: DataSource
	private val _sql = ThreadLocal<String>()
	private val _namedRegex = ":([_a-zA-Z0-9]+)".toRegex()

	init{
		_datasource = ds
	}

	/*insert a record*/
	fun insert(sql: String, paramMap: Map<String, Any>, returnAutoGenKey: Boolean = false) : SqlResult<Long> {
		try{
			return SqlResult<Long>(null, _insert(sql, paramMap, returnAutoGenKey))
		}catch(e: SQLException){
			return SqlResult<Long>(e, 0L)
		}
	}

	/*delete or update records*/
	fun update(sql: String, paramMap: Map<String, Any>) : SqlResult<Int> {
		try{
			return SqlResult<Int>(null, _update(sql, paramMap))
		}catch(e: SQLException){
			return SqlResult<Int>(e, -1)
		}
	}

	/*run batch query*/
	fun batchQuery(sql: String, paramList: List<Map<String, Any>>): SqlResult<Boolean> {
		try{
			_batchQuery(sql, paramList)
			return SqlResult<Boolean>(null, true)
		}catch(e: SQLException){
			return SqlResult<Boolean>(e, false)
		}
	}

	/*run sql query via Connection*/
	fun <T> execute(body: (Connection) -> T?): SqlResult<T?> {
		var value: T?
		try{
			value = exec(body)
		}catch(e: SQLException){
			return SqlResult<T?>(e, null)
		}
		return SqlResult<T?>(null, value)
	}

	private fun <T> exec(body: (Connection) -> T?): T? {
		var value: T? = null
		var conn = _datasource.getConnection()
		conn.use{
			try{
				value = body(conn)
			}catch(e: SQLException){
				logger.error("Exec error on ${this} " + e)
				throw e
			}
		}
		return value
	}

	/*select multiple records*/
	fun queryForList(sql: String, paramMap: Map<String, Any>? = null): SqlResult<List<Map<String, Any>>> {
		try{
			return SqlResult<List<Map<String, Any>>>(null, _queryForList(sql, paramMap))
		}catch(e: SQLException){
			return SqlResult<List<Map<String, Any>>>(e, listOf())
		}
	}

	/*select one record*/
	fun queryForMap(sql: String, paramMap: Map<String, Any>? = null): SqlResult<Map<String, Any>> {
		try{
			return SqlResult<Map<String, Any>>(null, _queryForMap(sql, paramMap))
		}catch(e: SQLException){
			return SqlResult<Map<String, Any>>(e, mapOf())
		}
	}

	/*set current sql*/
	fun setSql(sql: String) {
		_sql.set(sql)
	}

	/*create PreparedStatement*/
	fun createPreparedStatement(conn: Connection, namedSql: String, paramMap: Map<String, Any>, returnAutoGenKey: Boolean = false): PreparedStatement {
		setSql(namedSql)
		val matchList = _namedRegex.findAll(namedSql).toList().map{it.groupValues}
		if(matchList.isEmpty()){
			if(returnAutoGenKey){
				return conn.prepareStatement(namedSql, Statement.RETURN_GENERATED_KEYS)
			}else{
				return conn.prepareStatement(namedSql)
			}
		}

		val bindList: ArrayList<Any> = ArrayList(matchList.size)
		matchList.onEach{
			val bindName = it[1]
			val bindValue = paramMap.get(bindName)
			if(bindValue == null) {
				throw SQLException("bind param missing: [$bindName]")
			}
			bindList.add(bindValue)
		}

		val sql = namedSql.replace(_namedRegex ,"?")
		var stmt: PreparedStatement
		if(returnAutoGenKey){
			stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
		}else{
			stmt = conn.prepareStatement(sql)
		}
		try{
			bindList.forEachIndexed { idx, bindValue ->
				stmt.setObject(idx+1, bindValue)
			}
		}catch(e: SQLException){
			stmt.close()
			throw e
		}

		return stmt
	}

	/*create batch PreparedStatement*/
	fun createBatchPreparedStatement(conn: Connection, namedSql: String, paramList: List<Map<String, Any>>): PreparedStatement {
		setSql(namedSql)
		val matchList = _namedRegex.findAll(namedSql).toList().map{it.groupValues}
		if(matchList.isEmpty()){
			return conn.prepareStatement(namedSql)
		}

		val bindArr: ArrayList<ArrayList<Any>> = ArrayList(paramList.size)
		val matchCnt = matchList.size
		for(paramMap in paramList){
			val bindList: ArrayList<Any> = ArrayList(matchCnt)
			matchList.onEach{
				val bindName = it[1]
				val bindValue = paramMap.get(bindName)
				if(bindValue == null) {
					throw SQLException("bind param missing: [$bindName]")
				}
				bindList.add(bindValue)
			}
			bindArr.add(bindList)

		}

		val sql = namedSql.replace(_namedRegex ,"?")
		val stmt = conn.prepareStatement(sql)
		try{
			for(bindList in bindArr){
				bindList.forEachIndexed { idx, bindValue ->
					stmt.setObject(idx+1, bindValue)
				}
				stmt.addBatch()
			}
		}catch(e: SQLException){
			stmt.close()
			throw e
		}
		return stmt
	}

	private fun <T> query(sql: String, paramMap: Map<String, Any>?, body: (ResultSet) -> T?) : T? {

		val exeValue = exec<T>({ conn ->
			var stmt = createPreparedStatement(conn, sql, paramMap ?: mapOf())

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

	private fun _insert(sql: String, paramMap: Map<String, Any>, returnAutoGenKey: Boolean = false) : Long {

		val insertId = exec<Long>({ conn ->
			var stmt = createPreparedStatement(conn, sql, paramMap, returnAutoGenKey)
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

	private fun _update(sql: String, paramMap: Map<String, Any>) : Int {

		val updateRows = exec<Int>({ conn ->
			var stmt = createPreparedStatement(conn, sql, paramMap)
			var rows = 0
			stmt.use{
				rows = stmt.executeUpdate()
			}
			return@exec rows
		})
		return updateRows ?: 0
	}

	private fun _batchQuery(sql: String, paramList: List<Map<String, Any>>): Unit {

		exec<Any?>({ conn ->
			var stmt = createBatchPreparedStatement(conn, sql, paramList)
			stmt.use{
				stmt.executeBatch()
			}
		})
	}

	private fun _queryForList(sql: String, paramMap: Map<String, Any>? = null): List<Map<String, Any>> {

		val value = query<List<Map<String, Any>>>(sql, paramMap, { rs ->
			val value = JdbcUtil.rsToList(rs)
			return@query value
		})
		return value ?: listOf()
	}

	private fun _queryForMap(sql: String, paramMap: Map<String, Any>? = null): Map<String, Any> {

		val value = query<Map<String, Any>>(sql, paramMap, { rs ->
			JdbcUtil.rsToMap(rs)
		})
		return value ?: mapOf()
	}

	override fun toString(): String {
		return this::class.java.getSimpleName()+"[${_sql.get()} , ${_datasource}]"
	}
}

