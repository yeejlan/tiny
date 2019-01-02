package example.helper

import tiny.annotation.Helper

@Helper
class SquareHelper {

	fun getSquare(value: Long): Long {
		return value * value
	}
}
