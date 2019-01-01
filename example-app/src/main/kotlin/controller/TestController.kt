package example.controller

import tiny.annotation.AutoWeave
import tiny.annotation.Controller

@Controller
@AutoWeave
class TestController @AutoWeave constructor(){
	
	fun indexAction(): Any {
		return "this is test/index page"
	}

	fun userAction(): Any {
		return "this is test/user page"
	}

	fun infoAction(): Any {
		return "this is test/info page"
	}
}