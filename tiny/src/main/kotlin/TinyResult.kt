package tiny

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

import tiny.lib.db.SqlResult

class TinyResult <T:Any> constructor(error: String?, data: T?) {
	var error: String? = null
	lateinit var data: T
	val cause = mutableListOf<String>()

	init {
		this.error = error
		if(data != null){
			this.data = data
		}
		if(error != null){
			_addCause(error)
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
			throw TinyException(this::class.java.getSimpleName() + " error: " + cause)
		}
		return this.data
	}

	companion object{
		@JvmStatic fun <T: Any> fromMap(sr: SqlResult<Map<String, Any>>, clazz: KClass<T>): TinyResult<T> {
			if(sr.ex != null) {
				val msg = sr.ex.toString() + ": " + Thread.currentThread().getStackTrace()[2]
				return TinyResult<T>("Sql query error",  msg)
			}

			val con = clazz.primaryConstructor!!
			return TinyResult<T>(null, con.call(sr.data))
		}

		@JvmStatic fun <T: Any> fromList(sr: SqlResult<List<Map<String, Any>>>, clazz: KClass<T>): TinyResult<List<T>> {
			if(sr.ex != null) {
				val msg = sr.ex.toString() + ": " + Thread.currentThread().getStackTrace()[2]
				return TinyResult<List<T>>("Sql query error",  msg)
			}

			val con = clazz.primaryConstructor!!
			return TinyResult<List<T>>(null, sr.data.map{con.call(it)})
		}
	}

	private fun _addCause(error: String) {
		cause.add(error + ": " + Thread.currentThread().getStackTrace()[3])
	}

	private fun _addCause(tr: TinyResult<*>) {
		cause.addAll(tr.cause)
	}
}