package tiny

import java.io.File
import java.lang.reflect.Modifier

public val actions: HashMap<String, Any> = HashMap()

/*
* add action , for example: addAction("user/info", UserController::Class.java)
*/
fun addAction(action: String, clz: Class<Any>){
	actions.put(action, clz)
}

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
		* load controller and action from package path
		*/
		@JvmStatic fun loadControllers(pkgName: String) {
			val pkgPath = pkgName.replace('.', '/')
			val classpath = this::class.java.classLoader.getResource(pkgPath)
			if(classpath == null){
				return
			}
			val basePath = classpath.getPath()
			val baseDir = File(basePath)
			if (!baseDir.exists() || !baseDir.isDirectory()) {
				return
			}
			val helperFiles = baseDir.list()
			for(helper in helperFiles){
				if(helper.endsWith("Controller.class")){
					val clzName = helper.replace(".class", "")
					val fullClzName = pkgName + "." + clzName
					val clz = Class.forName(fullClzName)
					val methods = clz.getDeclaredMethods()
					for(method in methods){
						val name = method.getName()
						val modifiers = method.getModifiers()
						if(name.endsWith("Action") && (Modifier.PUBLIC and modifiers) != 0){
							println(clzName + "." + name)

						}
					}
				}
			}
		}
	}
}