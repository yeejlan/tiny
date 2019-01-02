package tiny

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class TinyWebContext(req: HttpServletRequest, res: HttpServletResponse) {

	/*HttpServletRequest*/
	var request: HttpServletRequest

	/*HttpServletResponse*/
	var response: HttpServletResponse

	/*hold error object when there is a internal server error*/
	lateinit var error: Throwable

	init{
		request = req
		response = res
	}
}