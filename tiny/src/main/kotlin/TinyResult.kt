package tiny

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import org.slf4j.LoggerFactory
import tiny.lib.db.SqlResult

private val logger = LoggerFactory.getLogger(TinyResult::class.java)

data class TinyResult <T:Any?> constructor(var error: String?, var data: T?) {
	val cause = mutableListOf<String>()

	init {
		if(error != null){
			_addCause(error as String)
			if(data != null){
				this.cause.add(data.toString())
			}
		}
	}

	constructor(error: String, cause: String) : this(null, null) {
		this.error = error
		_addCause(error)
		this.cause.add(cause)
	}

	constructor(error: String, tr: TinyResult<*>) : this(null, null) {
		_addCause(error)
		_addCause(tr)
	}

	constructor(sr: SqlResult<T>) : this(null, null) {
		if(sr.ex != null){
			val exStr = sr.ex.toString()
			this.error = exStr
			_addCause(exStr)
		}else{
			this.data = sr.data
		}
	}

	/*check if there is an error*/
	fun error(): Boolean {

		if(this.error != null){
			return true
		}
		return false
	}

	/*A safe way to get this.data*/
	fun data(): T {

		if(this.error()){
			val msg = "Bad TinyResult: " + Throwable().getStackTrace()[1] + cause()
			logger.error(msg)
			throw TinyResultException("Bad TinyResult" + cause())
		}
		@Suppress("UNCHECKED_CAST")
		return this.data as T
	}

	/*convert data to object*/
	fun <T: Any> toObj(clazz: KClass<T>): T? {
		if(this.error()){
			val msg = "Bad TinyResult: " + Throwable().getStackTrace()[1] + cause()
			logger.error(msg)
			throw TinyResultException("Bad TinyResult" + cause())
		}
		if(this.data is Map<*,*>) {
			val con = clazz.primaryConstructor!!
			return con.call(this.data)
		}
		@Suppress("UNCHECKED_CAST")
		return this.data as T?
	}

	/*convert data to list<Object>*/
	fun <T: Any> toList(clazz: KClass<T>): List<T> {
		if(this.error()){
			val msg = "Bad TinyResult: " + Throwable().getStackTrace()[1] + cause()
			logger.error(msg)
			throw TinyResultException("Bad TinyResult" + cause())
		}
		val con = clazz.primaryConstructor!!
		val theList = this.data as List<*>
		val newList: MutableList<T> = mutableListOf()
		theList.forEach{
			if(it is Map<*,*>) {
				newList.add(con.call(it))
			}else{
				@Suppress("UNCHECKED_CAST")
				return theList as List<T>
			}
		}
		return newList
	}

	/*return this.cause as String*/
	fun cause(): String {
		return this.cause.toString()
	}

	/*Throw exception if there is error*/
	fun mayThrow(message: String = ""): Unit {
		if(error()) {
			val msg = "${message}: " + Throwable().getStackTrace()[1] + cause()
			logger.error(msg)
			throw TinyResultException(message + cause())
		}
	}

	companion object{
		@JvmStatic fun <T: Any> fromMap(sr: SqlResult<HashMap<String, Any?>>, clazz: KClass<T>): TinyResult<T?> {
			if(sr.ex != null) {
				val msg = sr.ex.toString() + ": " + Throwable().getStackTrace()[1]
				return TinyResult<T?>("Sql query error",  msg)
			}
			if(sr.data.isEmpty()) {
				return TinyResult<T?>(null, null as T?)
			}
			val con = clazz.primaryConstructor!!
			return TinyResult<T?>(null, con.call(sr.data))
		}

		@JvmStatic fun <T: Any> fromList(sr: SqlResult<List<HashMap<String, Any?>>>, clazz: KClass<T>): TinyResult<List<T>> {
			if(sr.ex != null) {
				val msg = sr.ex.toString() + ": " + Throwable().getStackTrace()[1]
				return TinyResult<List<T>>("Sql query error",  msg)
			}

			val con = clazz.primaryConstructor!!
			return TinyResult<List<T>>(null, sr.data.map{con.call(it)})
		}
	}

	private fun _addCause(error: String) {
		cause.add(error + ": " + Throwable().getStackTrace()[2])
	}

	private fun _addCause(tr: TinyResult<*>) {
		cause.addAll(tr.cause)
	}
}

class TinyResultException(message: String?) : Throwable(message)