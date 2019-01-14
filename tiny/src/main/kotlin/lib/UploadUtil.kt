package tiny.lib

import org.apache.commons.fileupload.FileItem
import java.io.File
import org.slf4j.LoggerFactory

object UploadUtil {
	private val logger = LoggerFactory.getLogger(this::class.java)

	fun SaveUploadFile(fileItem: FileItem, saveToFilePath: String): Throwable? {
		val saveTo = File(saveToFilePath)
		try{
			fileItem.write(saveTo)
		}catch(e: Throwable){
			logger.warn("SaveUploadFile error: " + e)
			return e
		}
		return null
	}


}