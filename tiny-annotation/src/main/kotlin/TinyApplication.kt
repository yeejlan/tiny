package tiny.annotation

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class TinyApplication(val name: String = "app", val daggerModules: Array<KClass<*>> = [])