package tiny.lib

import java.util.UUID

object UniqueIdUtil {
	
	@JvmStatic fun getUniqueId() :String {
		return UUID.randomUUID().toString().replace("-", "")
	}
}