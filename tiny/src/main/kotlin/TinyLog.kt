package tiny

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.FileAttribute
import java.io.BufferedWriter
import java.util.concurrent.ConcurrentLinkedQueue

import java.text.SimpleDateFormat

import java.util.Calendar
import java.util.Date
import java.io.IOException

import tiny.TinyException

private var loggerStarted = false
private var logPath = "/tmp/logs"
private var isPosix = true

private val queue = ConcurrentLinkedQueue<LogObject>()

private val filePerms = PosixFilePermissions.fromString("rw-rw----")
private val fileAttr = PosixFilePermissions.asFileAttribute(filePerms)
private val dirPerms = PosixFilePermissions.fromString("rwxrwx---")
private val dirAttr = PosixFilePermissions.asFileAttribute(dirPerms)
private val maxFileOpened = 10
private val writerCache = WriterCache()
private var loggerRunning = true

private class WriterCache(initialCapacity: Int = 15, loadFactor: Float = 0.75f, accessOrder:Boolean = true)
	: LinkedHashMap<Path, BufferedWriter>(initialCapacity, loadFactor, accessOrder) {

	override fun removeEldestEntry(eldest: Map.Entry<Path, BufferedWriter>): Boolean {
			val tooMany = this.size > maxFileOpened
			if(tooMany) {
				val writer = eldest.value
				try{
					writer.close()
				}catch(e: IOException){
					//pass
				}
			}
			return tooMany
		}
}

object TinyLog {
	
	@JvmStatic fun log(message: String, prefix: String = "default") {
		if(!loggerStarted){
			throw TinyException("Logger not started, please call init first")
		}
		queue.add(LogObject(message, prefix))
	}

	private fun setLogPath(path: String) {
		logPath = path
		val p = Paths.get(logPath)
		if(!Files.isDirectory(p)) {
			throw TinyException("Directory not exist: " + logPath)
		}
		if(!Files.isWritable(p)) {
			throw TinyException("Directory not writable: " + logPath)	
		}
	}

	private fun startLoggerThread() {

		val loggerThread = LoggerThread()
		loggerThread.setDaemon(true)
		loggerThread.setName("TinyLog logger worker")
		loggerThread.start()
		loggerStarted = true

	}

	@JvmStatic fun init(path: String) {
		val os = System.getProperty("os.name")
		if(os.toLowerCase().startsWith("win")){
			isPosix = false
		}
		TinyLog.setLogPath(path)
		TinyLog.startLoggerThread()
		Runtime.getRuntime().addShutdownHook(LoggerShutdownThread())
	}
}

private class LoggerThread() : Thread() {

	override fun run(){

		try{
			while(loggerRunning){
				
				var logObject: LogObject? = queue.poll()
				while(logObject != null) {
					writeLog(logObject.message, logObject.prefix)
					logObject = queue.poll()
				}
				Thread.sleep(1)
			}
		}catch(e: InterruptedException){
			Thread.currentThread().interrupt()
		}
	}

	private fun writeLog(message: String, prefix: String) {

		val c = Calendar.getInstance()
		val year = c.get(Calendar.YEAR).toString()
		val month = String.format("%02d", c.get(Calendar.MONTH) + 1)
		val day = String.format("%02d", c.get(Calendar.DAY_OF_MONTH))

		val logName = prefix + "_" + day + ".log"

		val monthDir = Paths.get(logPath, year, month)
		try{
			if(!Files.isDirectory(monthDir)) {
				if(isPosix){
					Files.createDirectories(monthDir, dirAttr)
				}else{
					Files.createDirectories(monthDir)
				}
			}
		}catch(e: IOException){
			//pass
		}

		val logFile = Paths.get(logPath, year, month, logName)
		try{
			if(!Files.exists(logFile)) {
				if(isPosix){
					Files.createFile(logFile, fileAttr)
				}else{
					Files.createFile(logFile)
				}
			}
		}catch(e: IOException){
			//pass
		}

		val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
		val now = sdf.format(Date())

		var writer = writerCache.get(logFile)

		if(writer == null){//not found in cache
			try{
				writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8, 
					StandardOpenOption.APPEND, StandardOpenOption.WRITE)
			}catch(e: IOException){
				//pass
			}
			if(writer != null){
				writerCache.put(logFile, writer)
			}
		}
		val msg = now + " " + message + "\r\n"
		try{

			writer?.write(msg)
			writer?.flush()
		}catch(e: IOException){ //remove bad writer
			writerCache.remove(logFile)
			try{
				writer?.close()
			}catch(e: IOException){
				//pass
			}
		}
	}
}

private data class LogObject(val message: String, val prefix: String)

private class LoggerShutdownThread() : Thread() {
	override fun run(){
		loggerRunning = false
		Thread.sleep(100) //to finish log writing
		for(one in writerCache){
			val writer = one.value
			try{
				writer.close()
			}catch(e: IOException){
				//pass
			}
		}
	}
}