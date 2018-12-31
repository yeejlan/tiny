package example.helper

import tiny.annotation.Helper

@Helper
class AddHelper {

	fun getSquare(a: Long, b: Long): Long {
		return a + b
	}
}