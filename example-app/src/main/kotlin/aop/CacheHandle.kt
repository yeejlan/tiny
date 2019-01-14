package example.aop

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.annotation.*
import org.aspectj.lang.reflect.MethodSignature

@Aspect
class CacheHandle {

	@Pointcut("@annotation(tiny.annotation.AutoWeave)")
	fun doCache() {}
}