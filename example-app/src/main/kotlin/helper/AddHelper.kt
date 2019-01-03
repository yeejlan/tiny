package example.helper

import tiny.annotation.Helper

@Helper
class AddHelper {

	fun add(a: Long, b: Long): Long {
		return a + b
	}
}