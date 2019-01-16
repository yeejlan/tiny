package example.controller

import tiny.annotation.AutoWeave
import tiny.annotation.Controller
import tiny.TinyController
import tiny.lib.*

@Controller
class TestController @AutoWeave constructor(): TinyController(){
	
	fun indexAction(): Any {
		return "this is test/index page"
	}

	fun userAction(): Any {
		return "3this is test/user page"
	}

	fun infoAction(): Any {
		return "this is test/info page"
	}

	fun setSessionAction() : Any{
		ctx.session["userid"] = 123
		val infoMap: HashMap<String, Any> = hashMapOf(
			"id" to 123,
			"name" to "nana",
			"age" to "22",
			"zipcode" to "200079"
		)
		ctx.session["userInfo"] = infoMap
		ctx.session["randid"] = UniqueIdUtil.getUniqueId()
		return ctx.session
	}

	fun getSessionAction(): Any {

		val info = ctx.session["userInfo"]
		println(info)
		return ctx.session
	}
}