package tiny

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.Cookie
import tiny.lib.*

private val sessionName = TinyApp.getConfig()["session.name"]
private val cookieDomain = TinyApp.getConfig()["cookie.domain"]
private val sessionExpire = TinyApp.getConfig().getInt("session.expire.seconds", 3600)
private val sessionEnable = TinyApp.getConfig().getBoolean("session.enable")

class TinyWebContext(req: HttpServletRequest, res: HttpServletResponse) {

	/*HttpServletRequest*/
	var request: HttpServletRequest

	/*HttpServletResponse*/
	var response: HttpServletResponse

	/*hold exception when there is a internal server error*/
	var exception: Throwable? = null

	/*store request params, array params are NOT supported, for those please use request.getParameterValues()*/
	lateinit var params: TinyParams

	/*session support*/
	val session = TinySession()

	/*cookies storage*/
	lateinit var cookies: TinyCookies

	init{
		request = req
		response = res

		_initParams()
		_initCookies()
	}

	fun setCookie(name: String, value: String, maxAge: Int = 0, domain: String = "", 
		path: String = "/", secure: Boolean = false, httponly: Boolean = false) {
		val c = Cookie(name, value)
		c.setMaxAge(maxAge)
		c.setDomain(domain)
		c.setPath(path)
		c.setSecure(secure)
		c.setHttpOnly(httponly)
		response.addCookie(c)
	}

	fun newSession(){
		if(!sessionEnable){
			return
		}
		session.destroy()
		session.sessionId = UniqueIdUtil.getUniqueId()
		setCookie(sessionName, session.sessionId, domain=cookieDomain, maxAge=sessionExpire)
	}

	fun loadSession(){
		if(!sessionEnable){
			return
		}
		val sessionId = cookies[sessionName]
		if(sessionId.isEmpty()){
			newSession()
		}else{
			session.sessionId = sessionId
			session.load()
		}
	}

	private fun _initParams(){
		val paramMap: HashMap<String, String> = HashMap()
		var paramKeys = request.getParameterNames()
		for(paramKey in paramKeys){
			val param = request.getParameter(paramKey)
			paramMap.put(paramKey, param)
		}
		params = TinyParams(paramMap)
	}

	private fun _initCookies(){
		val cookieMap: HashMap<String, String> = HashMap()
		val _cookies = request.getCookies()
		if(_cookies != null){
			for(c in _cookies){
				cookieMap.put(c.getName(), c.getValue())
			}
		}
		cookies = TinyCookies(cookieMap)
	}

}