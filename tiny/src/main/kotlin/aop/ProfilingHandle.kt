package tiny.aop

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.annotation.*
import org.aspectj.lang.reflect.MethodSignature

import tiny.lib.DebugUtil
import tiny.lib.TinyProfiler
import tiny.lib.MethodProfiler
import tiny.annotation.Profiling


@Aspect
class ProfilingHandle {

	@Around("execution(* *(..)) && @annotation(tiny.annotation.Profiling)")
	fun handleProfiling(pjp: ProceedingJoinPoint): Any {
		if(!TinyProfiler.enabled()){
			return pjp.proceed()
		}

		val signature = pjp.getSignature() as MethodSignature
		val method = signature.getMethod()

		val argNames = signature.getParameterNames()
		val argsValues = pjp.getArgs()
		val paramMap: HashMap<String, Any?> = hashMapOf()
		for (i in 0..argsValues.size-1) {
			paramMap.put(argNames[i], argsValues[i])
		}
		val timeStart = System.currentTimeMillis()
		val result = pjp.proceed()
		val timeEnd = System.currentTimeMillis()

		/* apiclient.call handle begin */
		val obj = pjp.getTarget()
		if(obj is tiny.lib.soa.ApiClient){
			paramMap.put("url", obj.getUrl())
			paramMap.put("request", obj.getRequest())
		}
		/* apiclient.call handle end */

		val methodProfiler = MethodProfiler(method.toString(), paramMap, timeEnd-timeStart)
		TinyProfiler.add(methodProfiler)
		return result
	}
}