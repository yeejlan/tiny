package tiny.lib.soa

import tiny.TinyException

val RequestFailed = -1
val InternalError = 1
val BadParam = 2
val BadResult = 3

private val codeMsgMapping: Map<Int, String> = mapOf(
	RequestFailed to "request failed",
	InternalError to "internal error",
	BadParam to "bad param",
	BadResult to "bad result"
)


data class ApiError(val code: Int, val detail: String = "", var message: String = ""){
	init{
		val msg = codeMsgMapping.get(code)
		if(msg == null) {
			throw TinyException("ApiError: Unknown code")
		}
		message = msg
	}
}