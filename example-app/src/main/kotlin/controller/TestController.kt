package example.controller

import tiny.annotation.AutoWeave
import tiny.annotation.Controller
import tiny.TinyController
import tiny.*
import tiny.lib.*
import tiny.lib.db.*
import tiny.lib.soa.*

@Controller
class TestController @AutoWeave constructor(): TinyController(){
	
	fun indexAction(): Any {
		return "this is test/index page"
	}

	fun userAction(): Any {
		return "11123this is test/user page"
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

	fun uploadAction(): Any {
		println(ctx.params)
		println(ctx.files)
		val file1 = ctx.files["file1"]
		if(file1 == null || file1.size == 0L){
			return "no file uploaded."
		}
		val result = UploadUtil.SaveUploadFile(file1, "d:/data/temp/a.txt")
		if(result.error()){
			return "upload error: " + result.error
		}else{
			return "upload done."
		}
	}

	fun selectAction() : Any {
		val jdbc = TinyRegistry["db.account"] as TinyJdbc
		val users = jdbc.queryForList("select id,name from user where id < :id order by id desc limit 5", mapOf(
				"id" to 1002,
				"name" to "note.gif"
			))
		if(users.ex != null){
			return "query error: " + users.ex
		}
		val apiReturn = callApi()
		return "users = " + users.data + " api return = " + apiReturn
	}

	fun apiAction(): Any {
		return "request params :" + ctx.params
	}

	fun callApi(): ApiResult {
		val client = ApiClient("127.0.0.1", 8080)
		val paramsMap = mapOf<String, Any>(
			"id" to 123,
			"name" to "Nana",
			"zipcode" to "6702931"
		)
		val result = client.method("/test/api").request(paramsMap).call()
		return result
	}
}