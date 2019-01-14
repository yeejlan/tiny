package tiny.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CacheDelete(
	val key: String
)