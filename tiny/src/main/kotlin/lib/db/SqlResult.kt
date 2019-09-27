package tiny.lib.db

import tiny.TinyResult

data class SqlResult <T> (val ex: Throwable?, val data: T)