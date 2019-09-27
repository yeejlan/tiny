package tiny.lib.db

import tiny.TinyResult

data class SqlResult <T> (val ex: Throwable?, val data: T) {
	fun result(): TinyResult<T> {
		if(this.ex != null) {
			val msg = this.ex.toString() + ": " + Thread.currentThread().getStackTrace()[2]
			return TinyResult<T>("Sql query error",  msg)
		}

		return TinyResult<T>(null, this.data)
	}
}