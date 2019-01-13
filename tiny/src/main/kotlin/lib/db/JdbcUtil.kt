package tiny.lib.db

import java.sql.ResultSet
import java.sql.Blob
import java.sql.Clob
import java.sql.Connection
import java.sql.SQLException
import java.sql.PreparedStatement
import java.sql.Statement

import tiny.lib.DebugUtil

object JdbcUtil {
	private val _namedRegex = ":([_a-zA-Z0-9]+)".toRegex()

	/*Convert ResultSet to Map<String, Any>*/
	fun rsToMap(rs: ResultSet): Map<String, Any>{
		val resultMap = HashMap<String, Any>()
		val metaData = rs.getMetaData()
		val columnCount = metaData.getColumnCount()
		while(rs.next()){
			for(i in 1..columnCount){
				resultMap.put(metaData.getColumnName(i), JdbcUtil.getRsValue(rs, i))
			}
			break
		}
		return resultMap
	}

	/*Convert ResultSet to List<Map<String, Any>>*/
	fun rsToList(rs: ResultSet): List<Map<String, Any>>{
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
		return resultList
	}

	/*copy from org.springframework.jdbc.support.JdbcUtils*/
	fun getRsValue(rs: ResultSet, index: Int): Any{
		var obj = rs.getObject(index)
		var className: String = ""
		if (obj != null) {
			className = obj::class.java.getName()
		}
		if (obj is Blob) {
			obj = obj.getBytes(1, obj.length().toInt())
		}
		else if (obj is Clob) {
			obj = obj.getSubString(1, obj.length().toInt())
		}
		else if ("oracle.sql.TIMESTAMP" == className || "oracle.sql.TIMESTAMPTZ" == className) {
			obj = rs.getTimestamp(index)
		}
		else if (!className.isEmpty() && className.startsWith("oracle.sql.DATE")) {
			val metaDataClassName = rs.getMetaData().getColumnClassName(index)
			if ("java.sql.Timestamp" == metaDataClassName || "oracle.sql.TIMESTAMP" == metaDataClassName) {
				obj = rs.getTimestamp(index)
			}
			else {
				obj = rs.getDate(index)
			}
		}
		else if (obj is java.sql.Date) {
			if ("java.sql.Timestamp" == rs.getMetaData().getColumnClassName(index)) {
				obj = rs.getTimestamp(index)
			}
		}
		return obj
	}

	/*create PreparedStatement*/
	fun createPreparedStatement(conn: Connection, namedSql: String, paramMap: Map<String, Any>, returnAutoGenKey: Boolean = false): PreparedStatement {

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
}