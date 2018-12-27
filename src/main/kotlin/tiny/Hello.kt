package tiny

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.annotation.WebServlet

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHandler


@WebServlet(name="mytest",urlPatterns=arrayOf("/"))

class Hello() : HttpServlet() {
  val message = "hello~"
  
  override fun init() {
     //pass
  }

  override fun doGet(request: HttpServletRequest, response: HttpServletResponse){

      response.setContentType("text/html")

      val out = response.getWriter()
      out.println(message)
  }

  override fun destroy(){
      //pass
  }
}

fun main(args: Array<String>) {
	val server = Server(8080)
	val handler = ServletHandler()
    server.setHandler(handler)
    handler.addServletWithMapping(Hello::class.java, "/*")
    server.start()
    server.join()
}