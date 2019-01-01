package example.aop

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.annotation.*
import org.aspectj.lang.reflect.MethodSignature

import tiny.annotation.AutoWeave
import tiny.weaver.TinyBird
import example.controller.UserController

@Aspect
class AutoWeaveHandle {
	@Pointcut("execution(*.new(..)) && within(example.controller.UserController)")
	fun initMethod() {}

	@Before("initMethod()")
	fun autoWeave(jp: JoinPoint) {
		println("this is autoWeave")

		val clz = Class.forName(jp.getTarget()::class.java.getCanonicalName())
		println(clz)

		//val _method = jp.getTarget()::class.java.getMethod("weave", clz)
		
		//println(_method)
	
		//TinyBird.get().weave( castObject(clz, jp.getTarget())  )
	}

	private fun <T> castObject(clz: Class<T>, obj: Any): T {
		return obj as T
	}
}
