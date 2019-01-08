package tiny.lib

interface SessionStorage {

	fun load()
	fun save()

	fun get(key: String): String?
	fun set(key: String, value: String)
	fun delete(key: String)
	fun clean()
}