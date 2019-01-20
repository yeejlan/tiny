package tiny

class TinyResult <T:Any> constructor(error: String?, data: Any?) {
	var error: String? = null
	lateinit var data: T
	val cause = mutableListOf<String>()

	init {
		this.error = error
		if(data != null){
			@Suppress("UNCHECKED_CAST")
			this.data = data as T
		}
		if(error != null){
			_addCause(error)
			if(data != null){
				this.cause.add(data.toString())
			}
		}
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