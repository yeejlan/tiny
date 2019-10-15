package tiny.aop

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.annotation.*
import org.aspectj.lang.reflect.MethodSignature

import tiny.TinyResult
import tiny.lib.TinyCache
import tiny.lib.DebugUtil
import tiny.TinyRegistry
import tiny.annotation.AddCache
import tiny.annotation.DeleteCache

@Aspect
class CacheHandle {
	private val _paramRegex = "\\{([_a-zA-Z0-9]+)\\}".toRegex()

	@Around("execution(* *(..)) && @annotation(tiny.annotation.AddCache)")
	fun handleCacheAdd(pjp: ProceedingJoinPoint): Any? {
		val signature = pjp.getSignature() as MethodSignature
		val method = signature.getMethod()
		val cacheAdd = method.getAnnotation(AddCache::class.java)

		if(cacheAdd == null){ //should never happen
			throw CacheHandleException("Error on get annotation")
		}

		val cacheKey = cacheAdd.key
		val cacheExpire = cacheAdd.expire
		val realCacheKey = getRealCacheKey(cacheKey, pjp)
		val returnType = signature.getReturnType()
		val cachedResult = TinyCache.get(realCacheKey, returnType)
		if(cachedResult != null){
			return cachedResult
		}

		val result = pjp.proceed()
		if(result != null){
			if(result is TinyResult<*>) {
				if(result.error() || result.data() == null || result.data() == ""){
					return result
				}
				if(result.data() is List<*> && (result.data() as List<*>).isEmpty()){
					return result
				}
				if(result.data() is Map<*, *> && (result.data() as Map<*, *>).isEmpty()){
					return result
				}
			}
			TinyCache.set(realCacheKey, result, cacheExpire)
		}
		return result
	}

	@Around("execution(* *(..)) && @annotation(tiny.annotation.DeleteCache)")
	fun handleCacheDelete(pjp: ProceedingJoinPoint): Any? {
		val signature = pjp.getSignature() as MethodSignature
		val method = signature.getMethod()
		val cacheDelete = method.getAnnotation(DeleteCache::class.java)

		if(cacheDelete == null){ //should never happen
			throw CacheHandleException("Error on get annotation")
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
				throw CacheHandleException("bind param missing: [$bindName]")
			}
			realCacheKey = realCacheKey.replace(replaceName, bindValue.toString())
		}
		return realCacheKey
	}


}

private class CacheHandleException(message: String?) : Throwable(message)