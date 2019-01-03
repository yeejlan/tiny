package tiny

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class TinyWebContext(req: HttpServletRequest, res: HttpServletResponse) {

	/*HttpServletRequest*/
	var request: HttpServletRequest

	/*HttpServletResponse*/
	var response: HttpServletResponse

	/*hold exception when there is a internal server error*/
	lateinit var exception: Throwable

	init{
		request = req
		response = res
	}
}