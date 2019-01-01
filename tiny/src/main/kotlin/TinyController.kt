package tiny

import java.io.File
import java.lang.reflect.Modifier

public val actions: HashMap<String, Class<*>> = HashMap()

class TinyController{

	var view = TinyView()

	fun before(){
		//pass
	}

	fun after(){
		//pass
	}

	/**
	* render a template to string(without .tpl)
	**/
	fun render(tplPath : String): String{
		return view.render(tplPath)
	}

	fun callAction(controller: String, action: String){
		TinyRouter.callAction(controller, action)
	}

	companion object{
		/*
		* add action , for example: addAction("user/info", UserController::Class.java)
		*/
		@JvmStatic fun addAction(action: String, clz: Class<*>){
			actions.put(action, clz)
		}

	}
}