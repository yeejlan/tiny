package example.controller

import tiny.annotation.AutoWeave
import tiny.annotation.Controller
import tiny.TinyController

@Controller
class TestController @AutoWeave constructor(): TinyController(){
	
	fun indexAction(): Any {
		return "this is test/index page"
	}

	fun userAction(): Any {
		return "this is test/user page"
	}

	fun infoAction(): Any {
		return "this is test/info page"
	}

	fun sessionAction(): Any {
		ctx.session.set("userid", 123)
		val infoMap: HashMap<String, Any> = hashMapOf(
			"id" to 123,
			"name" to "nana",
			"age" to "22",
			"zipcode" to "200079"
		)
		ctx.session.set("userInfo", infoMap)
		val info = ctx.session.get("userInfo")
		println(info)
		return ctx.session
	}
}