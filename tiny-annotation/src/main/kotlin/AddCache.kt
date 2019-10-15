package tiny.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class AddCache(
	val key: String,
	val expire: Long = 3600
)