package tiny

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

	private fun _addCause(error: String) {
		cause.add(error + ": " + Thread.currentThread().getStackTrace()[3])
	}

	private fun _addCause(tr: TinyResult<*>) {
		cause.addAll(tr.cause)
	}
}