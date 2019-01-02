package example.aop

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.annotation.*
import org.aspectj.lang.reflect.MethodSignature

import tiny.annotation.AutoWeave
import tiny.weaver.TinyBird

@Aspect
class AutoWeaveHandle {
	@Pointcut("execution(*.new(..)) && @annotation(tiny.annotation.AutoWeave)")
	fun initMethod() {}

	@Before("initMethod()")
	fun autoWeave(jp: JoinPoint) {

		val clz = jp.getTarget()::class.java
		
		val _method = TinyBird.get()::class.java.getMethod("weave", clz)
		_method.invoke(TinyBird.get(), jp.getTarget())

	}
}
