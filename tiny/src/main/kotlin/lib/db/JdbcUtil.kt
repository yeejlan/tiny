package tiny.lib.db

import java.sql.ResultSet
import java.sql.Blob
import java.sql.Clob

object JdbcUtil {

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
}