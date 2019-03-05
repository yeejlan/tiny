package tiny

import java.io.File
import java.lang.reflect.Modifier
import groovy.lang.Writable

data class ActionPair(val first:Class<*>, val second: String)

private val actions: HashMap<String, ActionPair> = HashMap()

open class TinyController{

	var view = TinyView()
	lateinit var ctx: TinyWebContext

	open fun before() {
		//pass
	}

	open fun after() {
		//pass
	}

	/**
	* render a template (without .tpl)
	**/
	fun render(tplPath : String): Writable{
		return view.render(tplPath)
	}

	fun callAction(controller: String, action: String){
		TinyRouter.callAction(ctx, controller, action)
	}

	companion object{
		/*
		* add action , for example: addAction("user/info", UserController::Class.java)
		*/
		@JvmStatic fun addAction(actionKey: String, action: ActionPair){
			actions.put(actionKey, action)
		}
		
		fun getActions(): HashMap<String, ActionPair>{
			return actions
		}
	}
}