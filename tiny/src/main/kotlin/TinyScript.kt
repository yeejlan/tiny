package tiny

import java.lang.reflect.InvocationTargetException
import org.slf4j.LoggerFactory

object TinyScript {
	private val logger = LoggerFactory.getLogger(this::class.java)

	@JvmStatic fun run(env: String, appName: String, clzName: String) {

		logger.info("TinyScript[$clzName] running...")
		val startTime = System.currentTimeMillis()/1000L
		try{

			TinyApp.init(env, appName, runAsScript = true)
			val clz = Class.forName(clzName)
			val obj = clz.newInstance()
			val method = clz.getMethod("run")
			if(method == null){
				throw TinyException("run() method not found in " + clzName)
			}
			method.invoke(obj)
		}catch(e: Throwable){
			var ex = e
			if(e is InvocationTargetException ){
				val targetException = e.getTargetException()
				ex = targetException
			}
			logger.error("TinyScript[$clzName].run(): " + ex)
			throw ex
		}finally{
			TinyApp.shutdown()
			val endTime = System.currentTimeMillis()/1000L
			logger.info("TinyScript[$clzName] end, time cost: ${endTime-startTime} second(s)")
		}
	}
}