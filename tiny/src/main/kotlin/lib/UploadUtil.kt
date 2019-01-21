package tiny.lib

import org.apache.commons.fileupload.FileItem
import java.io.File
import org.slf4j.LoggerFactory

import tiny.TinyResult

object UploadUtil {
	private val logger = LoggerFactory.getLogger(this::class.java)

	fun SaveUploadFile(fileItem: FileItem?, saveToFilePath: String): TinyResult<Boolean> {
		if(fileItem == null) {
			return TinyResult<Boolean>("file is null", null)
		}else if(fileItem.getSize() == 0L){
			return TinyResult<Boolean>("file size is zero", null)
		}
		val saveTo = File(saveToFilePath)
		try{
			fileItem.write(saveTo)
		}catch(e: Throwable){
			logger.warn("SaveUploadFile error: " + e)
			return TinyResult<Boolean>(e.toString(), null)
		}
		return TinyResult<Boolean>(null, true)
	}


}