package tiny.lib.soa

import java.util.concurrent.TimeUnit

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.HttpUrl
import okhttp3.logging.HttpLoggingInterceptor

import java.io.IOException
import tiny.annotation.TimeIt

private val JSON = MediaType.parse("application/json; charset=utf-8")

private val defaultReadTimeout : Long = 5
private val defaultWriteTimeout : Long = 5
private val defaultConnectTimeout : Long = 2

private val httpClient = OkHttpClient.Builder()
	.readTimeout(defaultReadTimeout, TimeUnit.SECONDS)
	.writeTimeout(defaultWriteTimeout, TimeUnit.SECONDS)
	.connectTimeout(defaultConnectTimeout, TimeUnit.SECONDS)
	.build()

class ApiClient (host: String, port: Int = 80, scheme: String = "http") {

	private var _host: String
	private var _port: Int
	private var _scheme: String
	private var _timeout: Long = defaultReadTimeout
	private var _method: String = ""
	private var _debug: Boolean = false
	private var _requestMap: JsonMap? = null
	private var _url: HttpUrl? = null

	init{
		_host = host
		_port = port
		_scheme = scheme			
	}

	fun host(host: String): ApiClient {
		_host = host
		return this
	}

	fun method(method: String): ApiClient {
		_method = method
		return this
	}

	fun port(port: Int): ApiClient {
		_port = port
		return this
	}	

	fun scheme(scheme: String): ApiClient {
		_scheme = scheme
		return this
	}

	fun request(map: Map<String, Any>): ApiClient {
		_requestMap  = JsonMap(map)
		return this
	}

	fun getRequest(): Map<String, Any?>{
		return _requestMap?.getMap() ?: mapOf<String, Any?>()
	}

	fun getUrl(): String {
		return _url.toString()
	}

	fun timeout(seconds: Long): ApiClient {
		_timeout = seconds
		return this
	}

	fun debug(): ApiClient {
		_debug = true
		return this
	}

	@TimeIt
	fun call(): ApiResult{

		var jsonStr: String = ""
		if(_requestMap != null){
			jsonStr = _requestMap.toString()
		}
		val body = RequestBody.create(JSON, jsonStr)

		val method = _method.trim('/')

		val url = HttpUrl.Builder()
			.scheme(_scheme)
			.host(_host)
			.port(_port)
			.addPathSegments(method)
			.build()

		_url = url

		val request = Request.Builder()
			.url(url)
			.post(body)
			.build()


		var client = httpClient

		if(_timeout != defaultReadTimeout || _debug != false){

			val builder = httpClient.newBuilder()
				.readTimeout(_timeout, TimeUnit.SECONDS)
				.writeTimeout(defaultWriteTimeout, TimeUnit.SECONDS)
				.connectTimeout(defaultConnectTimeout, TimeUnit.SECONDS)

			if(_debug){
				val loggingInterceptor = HttpLoggingInterceptor()
				loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
				builder.addInterceptor(loggingInterceptor)
			}	
			
			client = builder.build()
		}

		try {
			val response = client.newCall(request).execute()
			val bodyStr = response.body()?.string()

			val map = JsonMap(bodyStr)

			val code = map["code"].toInt(-1)

			if(code == 0){
				return ApiResult(null, map["data"].getMap())
			}else if(code > 0){
				return ApiResult(ApiError(code, map["detail"].toString()), null)
			}else{ //code == -1
				return ApiResult(ApiError(RequestFailed, "json parse error"), null)
			}

		}catch(e: IOException) {
			val detail = e.toString()
			return ApiResult(ApiError(RequestFailed, detail), null)
		}
	}	
}
