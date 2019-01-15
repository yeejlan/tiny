package example.aop

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.annotation.*
import org.aspectj.lang.reflect.MethodSignature

import tiny.lib.TinyCache
import tiny.lib.DebugUtil
import tiny.TinyRegistry
import tiny.annotation.CacheAdd
import tiny.annotation.CacheDelete

@Aspect
class CacheHandle {
	private val _paramRegex = "\\{([_a-zA-Z0-9]+)\\}".toRegex()

	@Around("execution(* *(..)) && @annotation(tiny.annotation.CacheAdd)")
	fun handleCacheAdd(pjp: ProceedingJoinPoint): Any {
		val signature = pjp.getSignature() as MethodSignature
		val method = signature.getMethod()
		val cacheAdd = method.getAnnotation(CacheAdd::class.java)

		if(cacheAdd == null){ //should never happen
			throw CacheHandleAOPException("Error on get annotation")
		}

		val cacheKey = cacheAdd.key
		val realCacheKey = getRealCacheKey(cacheKey, pjp)
		val returnType = signature.getReturnType()
		val cachedResult = TinyCache.get(realCacheKey, returnType)
		if(cachedResult != null){
			return cachedResult
		}

		val result = pjp.proceed()
		if(result != null){
			TinyCache.set(realCacheKey, result)
		}
		return result
	}

	@Around("execution(* *(..)) && @annotation(tiny.annotation.CacheDelete)")
	fun handleCacheDelete(pjp: ProceedingJoinPoint): Any {
		val signature = pjp.getSignature() as MethodSignature
		val method = signature.getMethod()
		val cacheDelete = method.getAnnotation(CacheDelete::class.java)

		if(cacheDelete == null){ //should never happen
			throw CacheHandleAOPException("Error on get annotation")
		}

		val cacheKey = cacheDelete.key
		val realCacheKey = getRealCacheKey(cacheKey, pjp)
		val result = pjp.proceed()
		TinyCache.delete(realCacheKey)
		return result
	}

	private fun getRealCacheKey(cacheKey: String, pjp: ProceedingJoinPoint): String {
		var realCacheKey = cacheKey
		val matchList = _paramRegex.findAll(cacheKey).toList().map{it.groupValues}

		//get param map
		val argsValues = pjp.getArgs()
		val signature = pjp.getSignature() as MethodSignature
		val argNames = signature.getParameterNames()
		val paramMap: HashMap<String, Any> = hashMapOf()
		for (i in 0..argsValues.size-1) {
			paramMap.put(argNames[i], argsValues[i])
		}

		matchList.onEach{
			val replaceName = it[0]
			val bindName = it[1]
			val bindValue = paramMap.get(bindName)
			if(bindValue == null) {
				throw CacheHandleAOPException("bind param missing: [$bindName]")
			}
			realCacheKey = realCacheKey.replace(replaceName, bindValue.toString())
		}
		return realCacheKey
	}


}

private class CacheHandleAOPException(message: String?) : Throwable(message)