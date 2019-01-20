package tiny

class TinyResult <T> constructor(error: String?, data: T?) {
	var error: String? = null
	var data: T? = null
	val cause = mutableListOf<String>()

	init {
		this.error = error
		this.data = data
		if(error != null){
			_addCause(error)
		}
	}

	constructor(error: String, cause: String) : this(null, null) {
		_addCause(error)
		this.cause.add(cause)
	}

	constructor(error: String, tr: TinyResult<*>) : this(null, null) {
		_addCause(error)
		_addCause(tr)
	}

	private fun _addCause(error: String) {
		cause.add(error + ": " + Thread.currentThread().getStackTrace()[3])
	}

	private fun _addCause(tr: TinyResult<*>) {
		cause.addAll(tr.cause)
	}
}