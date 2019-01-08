package tiny.lib

import java.util.UUID

object UniqueIdUtils {
	@JvmStatic fun getUniqueId() :String {
		return UUID.randomUUID().toString().replace("-", "")
	}
}