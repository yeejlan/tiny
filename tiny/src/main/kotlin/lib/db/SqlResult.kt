package tiny.lib.db

data class SqlResult <T> (val ex: Throwable?, val data: T)