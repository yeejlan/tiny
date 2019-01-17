package tiny.hotswap

import org.hotswap.agent.annotation.*
import org.hotswap.agent.command.ReflectionCommand
import org.hotswap.agent.command.Command
import org.hotswap.agent.command.Scheduler
import org.hotswap.agent.javassist.CannotCompileException
import org.hotswap.agent.javassist.CtClass
import org.hotswap.agent.javassist.NotFoundException
import org.hotswap.agent.logging.AgentLogger
import org.hotswap.agent.util.IOUtils
import org.hotswap.agent.util.PluginManagerInvoker
import org.hotswap.agent.config.PluginManager
import org.hotswap.agent.util.classloader.*

import org.hotswap.agent.annotation.FileEvent.CREATE
import org.hotswap.agent.annotation.FileEvent.MODIFY
import org.hotswap.agent.annotation.LoadEvent.DEFINE
import org.hotswap.agent.annotation.LoadEvent.REDEFINE
import tiny.ActionPair
import tiny.HelperPair

private val LOGGER = AgentLogger.getLogger(TinyHotSwap::class.java)
private const val TINYAPP_SERVICE = "tiny.TinyApp"

@Plugin(name = "TinyHotSwap", description = "Hotswap agent plugin for Tiny application.",
	testedVersions = arrayOf("current verion"))
class TinyHotSwap {

	@Init
	lateinit var appClassLoader: ClassLoader

	@Init
	lateinit var scheduler: Scheduler

	@Init
	lateinit var pluginManager: PluginManager

	lateinit var tinyService: Any

	val hotswapCommand = MyReloadCommand()
	val reloadMap: HashMap<Class<*>, ByteArray> = HashMap()

	companion object{

		@OnClassLoadEvent(classNameRegexp = TINYAPP_SERVICE)
		@JvmStatic fun transformTinyEntityService(ctClass: CtClass) {
			var src = PluginManagerInvoker.buildInitializePlugin(TinyHotSwap::class.java)
			src += PluginManagerInvoker.buildCallPluginMethod(TinyHotSwap::class.java, "registerService", "this", "java.lang.Object")
			ctClass.getDeclaredConstructor(emptyArray<CtClass>()).insertAfter(src)

			LOGGER.info(TINYAPP_SERVICE + " has been enhanced.")
		}

	}

	fun registerService(serviceObj: Any) {
		this.tinyService = serviceObj
		LOGGER.info("Plugin {} initialized on service {}", this::class.java, this.tinyService)
	}

	@OnClassLoadEvent(classNameRegexp = ".*", events = arrayOf(REDEFINE))
	fun reloadClass(ctClass: CtClass){
		val className = ctClass.getName()
		val pkgName = ctClass.getPackageName()
		if(className.endsWith("Controller") && pkgName.endsWith(".controller")){
			for(method in ctClass.getDeclaredMethods()){
				val methodName = method.getName()
				if(methodName.endsWith("Action")){
					val action = ActionPair(Class.forName(className), methodName)
					val controllerName = className.substring(pkgName.length+1, className.length-"Controller".length)
					val actionName = methodName.substring(0, methodName.length-"Action".length)
					val actionKey = "$controllerName/$actionName".toLowerCase()
					scheduler.scheduleCommand(
						ReflectionCommand(tinyService, "addAction", actionKey, action)
					)
				}
			}
		}

		if(pkgName.endsWith(".helper")){
			val helperName = className.substring(pkgName.length+1)
			val helperInstance = Class.forName(className).newInstance()
			val helperPair = HelperPair(helperName, helperInstance)
			scheduler.scheduleCommand(
				ReflectionCommand(tinyService, "addHelper", helperPair)
			)
		}
	}

	@OnClassFileEvent(classNameRegexp = ".*", events = arrayOf(CREATE, MODIFY))
	fun changeClassFile(ctClass: CtClass) {
		if (!ClassLoaderHelper.isClassLoaded(appClassLoader, ctClass.getName())){
			return
		}
		
		try{
			val clazz = appClassLoader.loadClass(ctClass.getName())
			synchronized (reloadMap, {
				reloadMap.put(clazz, ctClass.toBytecode())
			})
			scheduler.scheduleCommand(hotswapCommand, 100, Scheduler.DuplicateSheduleBehaviour.SKIP)
		}catch (e: ClassNotFoundException ) {
			LOGGER.warning("Reload error: class not found: " + ctClass.getName())
		}
	}

	inner class MyReloadCommand : Command {
		override fun executeCommand() {
			pluginManager.hotswap(reloadMap)
		}
	}
}