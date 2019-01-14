package example.aop

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.annotation.*
import org.aspectj.lang.reflect.MethodSignature

import tiny.lib.TinyRedis
import tiny.lib.DebugUtil
import tiny.TinyRegistry
import tiny.annotation.CacheAdd
import tiny.annotation.CacheDelete

private val redis = TinyRegistry["redis.default"] as TinyRedis

@Aspect
class CacheHandle {

	@Around("execution(* *(..)) && @annotation(tiny.annotation.CacheAdd)")
	fun handleCacheAdd(pjp: ProceedingJoinPoint): Any {
		val signature = pjp.getSignature() as MethodSignature
		val method = signature.getMethod()
		val cacheAdd = method.getAnnotation(CacheAdd::class.java)

		if(cacheAdd != null){
			val cacheKey = cacheAdd.key
			println(cacheKey)
		}
		val argsValues = pjp.getArgs()
		val argNames = signature.getParameterNames()
		val paramMap: HashMap<String, Any> = hashMapOf()
		for (i in 0..argsValues.size-1) {
			paramMap.put(argNames[i], argsValues[i])
		}
		DebugUtil.print(paramMap)
		val result = pjp.proceed()
		return result
	}




}