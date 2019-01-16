package example.hotswap

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

import org.hotswap.agent.annotation.FileEvent.CREATE
import org.hotswap.agent.annotation.FileEvent.MODIFY
import org.hotswap.agent.annotation.LoadEvent.DEFINE
import org.hotswap.agent.annotation.LoadEvent.REDEFINE

private val LOGGER = AgentLogger.getLogger(TinyHotswapPlugin::class.java)
private const val TINYAPP_SERVICE = "tiny.TinyApp"
private const val WATCH_PACKAGE = "example"

@Plugin(name = "TinyHotswapPlugin", description = "Hotswap agent plugin for Tiny application.",
	testedVersions = arrayOf("current verion"))
class TinyHotswapPlugin {

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
			var src = PluginManagerInvoker.buildInitializePlugin(TinyHotswapPlugin::class.java)
			src += PluginManagerInvoker.buildCallPluginMethod(TinyHotswapPlugin::class.java, "registerService", "this", "java.lang.Object")
			ctClass.getDeclaredConstructor(emptyArray<CtClass>()).insertAfter(src)

			LOGGER.info(TINYAPP_SERVICE + " has been enhanced.")
		}

	}

	fun registerService(serviceObj: Any) {
		this.tinyService = serviceObj
		LOGGER.info("Plugin {} initialized on service {}", this::class.java, this.tinyService)
	}

	@OnClassLoadEvent(classNameRegexp = ".*", events = arrayOf(REDEFINE))
	fun reloadClass(className: String){
		LOGGER.info(className + " has been reloaded.")
	}

	@OnClassFileEvent(classNameRegexp = WATCH_PACKAGE + ".*", events = arrayOf(MODIFY))
	fun changeClassFile(ctClass: CtClass) {
		LOGGER.info(ctClass.getName() + " has been modified.")
		val clazz = appClassLoader.loadClass(ctClass.getName())
		synchronized (reloadMap, {
			reloadMap.put(clazz, ctClass.toBytecode())
		})
		scheduler.scheduleCommand(hotswapCommand, 500, Scheduler.DuplicateSheduleBehaviour.SKIP)
	}

	inner class MyReloadCommand : Command {
		override fun executeCommand() {
			pluginManager.hotswap(reloadMap)
		}
	}
}