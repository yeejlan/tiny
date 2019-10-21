package tiny.async

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.Cookie
import javax.servlet.AsyncContext
import org.slf4j.LoggerFactory
import tiny.lib.*
import tiny.*

private val logger = LoggerFactory.getLogger(AsyncWebContext::class.java)
class AsyncWebContext(aCtx: AsyncContext, req: HttpServletRequest, res: HttpServletResponse): TinyWebContext(req, res) {

	/*async context*/
	var actx: AsyncContext

	init{
		actx = aCtx
	}

	fun complete() {
		actx.complete()
	}
}