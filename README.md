# tiny
A Kotlin/Java mvc framework inspired by Zend Framework 1

### Reinvent the wheel?
I want to write xml config files as little as possible, so say goodbye to [Spring Boot](https://spring.io/projects/spring-boot), rather than injecting request scoped objects to singleton threadsafe controllers, create non threadsafe controllers for each request, and store temporary variables in those controllers is my favor way. Also I like the idea of mapping URI directly to actions. Here comes this new framework.

### Kotlin or Java?
In short, Kotlin is better Java.

##### The good stuff
* No checked error(May I say stupid feature in java?)
* String("abc") == String("abc") is true
* Auto type inference  
```kotlin
val longValue = 5L
```	
* String expression  
```kotlin
logger.warn("script running error, file: ${script.file}, line: ${script.getCurrentLine()} at $script")
```
* Multiline String
```kotlin
val text = """first line
	second line
	third line
""".trimMargin()
```
* Operator override  
```kotlin
val cat = Cargo["cat"]
Cargo["cat"] = Cat()
```	
Instead of  
```java
Cat cat = Cargo.get("cat")
Cargo.set("cat", new Cat())
```
* Default parameter  
```kotlin
fun setCache(key: String, value: String, expireSeconds: Long = 3600){...}
```	
* Call method via named parameter 
```kotlin
setCookie("userid", userId, domain = "example.com", path = "/", maxAge = 3600)
```
* Data class  

```kotlin
data class HelperPair(val name: String, val clazz: Class<*>)
val loggerHelper = HelperPair("logger helper", com.example.LoggerHelper)
println(loggerHelper.name)
val logger= loggerHelper.clazz.newInstance()
```
* singleton class  
```kotlin
object ASingletonClass(){val name = "singleton"}
```	
* import alias  

```kotlin
import System.out.print as p
import System.out.println as pn
```	
* and many others, such as auto close resource with use{...} etc

##### The bad things
For now, Kotlin compiler is pretty bad at syntax error tips, especially if compiler can't find your annotation, the only hint you got is somthing like "@error.NonExistentClass()"

### Coding principle

* [Keep it simple and stupid](https://en.wikipedia.org/wiki/KISS_principle)  

* [Worse is better](https://en.wikipedia.org/wiki/Worse_is_better)  

### Use tiny framework

##### Create application

```kotlin
@TinyApplication
class MyApp : TinyBootstrap {
	val name = "myapp"
	val env = System.getProperty("tiny.app.env") ?: "production"

	override fun bootstrap() {

		/*the application config file is classpath:"config/${env}/${name}.ini"  */
		TinyApp.init(env, name)
	}
}
```

A @WebServlet annotated servlet is auto generated and @TinyApplication annotated class.bootstrap() is called in servlet.init() method.

Run application in embedded jetty server

```kotlin
fun main(args: Array<String>){
	TinyApp.runJetty()
}

java -jar myapp.jar -Dtiny.app.env=development
```

Test something in shell

```kotlin
fun main(args: Array<String>){
	val app = MyApp()
	app.bootstrap()

	val jdbc = TinyRegistry["db.account"] as TinyJdbc
	try{
		val users = jdbc.queryForList("select id, name from users where 1 order by id desc limit 5")
		users.ex?.printStackTrace()
		DebugUtil.print(users.data)
		DebugUtil.print(TinyRegistry.getStorage())
	}finally{
		TinyApp.shutdown()
	}
}

java -jar myapp.jar -Dtiny.app.env=development
```
##### Create config

Config files are located in classpath: "config/${evn}" directory and end with ".ini".

```ini
#Application config: ${appName}.ini

#a must-have
timezone = Asia/Shanghai
#a must-have
log.path = /data/logs

#extra static file path for development hot reload
static.extra.dir = src/main/resources/static
#extra template file path for development hot reload
template.extra.dir = src/main/resources/templates

#cookie domain
cookie.domain = 

session.enable = true
session.name = MYSESSIONID
#for now the only storage supported is "redis"
session.storage = redis
#redis provider in "redis.ini"
session.storage.provider = redis.default
session.expire.seconds = 3600

cache.enable = true
cache.prefix = myapp_
cache.expire.seconds = 3600
#redis provider in "redis.ini"
cache.storage.provider = redis.default

#apache commons fileupload config
upload.fileInMemory.maxSize.megabyte = 5
upload.tempfile.dir = /tmp
upload.post.maxSize.megabyte = 50
```

```ini
#datasource config: db.ini
db.account.autoload = true #autoload on framework start or not
db.account.url = jdbc:mysql://127.0.0.1:3306/account?useUnicode=true&characterEncoding=UTF-8&serverTimezone=GMT
db.account.username = username
db.account.password = password
db.account.hikari.minimumIdle = 1
db.account.hikari.maximumPoolSize = 30	
```

```ini
#redis config: redis.ini
redis.default.autoload = true #autoload on framework start or not
redis.default.host = 127.0.0.1
redis.default.port = 6379
redis.default.database = 1
redis.default.pool.maxTotal = 10
redis.default.pool.maxIdle = 5
redis.default.pool.minIdle = 1
```

##### Create controller and action

```kotlin
/*Controllers need in a package name ending with ".controller", e.g "myapp.controller", 
by doing this hotswap plugin can identify it as a controller*/

package myapp.controller

import tiny.annotation.Controller
import tiny.TinyController

@Controller
/* URL http://yourhost/hello/world */
class HelloController : TinyController(){
	fun worldAction(): String {
		return "hello world"
	}

	fun greetingAction(): String{
		val username = ctx.params["username"]
		return "How are you $username"
	}
}
```

The URI and controller/action name is case insensitive, so /HeLLo/WorlD and HeLLoController.WOrlDAction() work, but you have to end with "Controller" and "Action"

##### Create url rewrite
```kotlin
/* 
 * /greeting/lina rewrite to IndexController.greetingAction() 
 * with ctx.params["username"] == "lina" 
*/
TinyRouter.addRoute("/greeting/([a-zA-Z]+)", "index/greeting", arrayOf(Pair(1, "username")))
```	

##### Create view
The only template engine supported is Groovy [GString](http://docs.groovy-lang.org/latest/html/documentation/template-engines.html#_gstringtemplateengine)

```groovy
<%=view.render("header")%>
This is a groovy gstring template<br />
<%def square = SquareHelper%>
call square helper: 25*25 = <%=square.getSquare(25)%> <br />
<%=view.render("footer")%>
```
```kotlin
class HelloController : TinyController(){
	fun tplAction(): Any{
		return render("body")  /* groovy.lang.Writable */
	}
}
```

template file is in classpath: "templates" and ending with ".tpl", template cache is disabled in "development" environment.

##### Create helper

```kotlin
/*Helpers need in a package name ending with ".helper", e.g "myapp.helper", 
by doing this hotswap plugin can identify it as a helper*/

package example.helper

import tiny.annotation.Helper

@Helper
/*helper class need end with "Helper"*/
class SquareHelper {

	fun getSquare(value: Long): Long {
		return value * value
	}
}
```

##### Use request,response,params,session,cookie,fileupload

```kotlin
class TestController : TinyController(){
	fun testAction(): String {

		val request = ctx.request /*javax.servlet.http.HttpServletRequest*/
		val response = ctx.response /*javax.servlet.http.HttpServletResponse*/

		/* params */
		val userAge = ctx.params.getLong("age", 18) //defalut age = 18
		val userName = ctx.params["name"]
		val action = ctx.params.getString("action")

		/* session */
		val userId: Long = ctx.session["userid"]
		if(userId < 1) {
			val loggedinId = doUserlogin()
			/* loggedin with a new session for better security */
			ctx.newSession()
			ctx.session["userid"] = loggedinId
		}
		if(action == "logout") {
			ctx.session.destroy()
			ctx.session["flashMessage"]  = "You have been logged out successfully"
		}

		/* cookie */
		val currentArticleId = ctx.cookies.getInt("current_article_id")
		val currentEditor = ctx.cookies["current_editor"]
		ctx.setCookie("currentEditor", "Zorro", 
			maxAge = 3600, 
			domain = "example.com", 
			path = "/", 
			secure = false, 
			httponly = false)

		/* fileupload */
		val avatar = ctx.files["avatar"]  /*org.apache.commons.fileupload.FileItem*/
		val saveTo = File("/tmp/avatar_${UniqueIdUtil.getUniqueId()}")
		try{
			avatar.write(saveTo)
		}catch(e: Throwable){
			logger.warn("Save avator error: " + e)
			throw e
		}
	}
}
```

##### Use TinyConfig, TinyCache, TinyRedis, TinyJdbc
```kotlin
fun testing() {
	/* TinyConfig */
	val redisConfig = TinyConfig("config/${TinyApp.getEnvString()}/redis.ini")
	val loader = TinyResourceLoader()
	val redisLocal = loader.loadRedis(redisConfig, "redis.local")
	val redis = TinyRegistry["redis.default"] as TinyRedis
	val jdbcAccount = TinyRegistry["db.account"] as TinyJdbc

	/* TinyCache */
	/* real key == "demo_user_id_123" since app.ini, cache.prefix = demo_ */
	TinyCache.set("user_id_123", HashMap<String, Any>(
			"id" to 123,
			"name" to "nana"
		), expireSeconds = 3600)
	val userNana = TinyCache.get("user_id_123", HashMap::class.java) /*HashMap*/
	val userNanaString = TinyCache.get("user_id_123") /*String*/
	TinyCache.delete("user_id_1")

	/* TinyRedis */
	redis.set("demo_user_id_123", HashMap<String, Any>(
			"id" to 123,
			"name" to "nana"
		), expireSeconds = 3600)

	redis.exec({ connection ->
		val commands = connection.sync()
		commands.expire("demo_user_id_123", 600)
	})

	/* TinyJdbc */
	val p = HashMap<String, Any>(
			"id" to 1001,
			"name" to "grrr"
		)
	jdbcAccount.insert("insert into users(id, name) values(:id, :name)", p)

	val userGrrr = jdbcAccount.selectForMap("select id, name from user where id = :id", p)
	userGrrr.ex?.printStackTrace()
	DebugUtil.print(userGrrr.data)

}

fun main(args: Array<String>){
	TinyApp.init("development", "myapp")
	try{
		testing()
	}finally{
		TinyApp.shutdown()
	}
}
```

##### Use ioc

The ioc framework using is [Dagger2](https://google.github.io/dagger/)  

Tiny framework automatically create the dagger component(tiny.weaver.MagicBox) and component holder(tiny.weaver.TinyBird)  

@WeaverBird links a module to current component:

```kotlin
package myapp.weaver

import tiny.annotation.WeaverBird
import javax.inject.Inject
import dagger.Provides
import dagger.Module
import javax.inject.Named

private val dbAccount = TinyRegistry["db.account"] as TinyJdbc

@WeaverBird
@Module
class JdbcWeaver {

	@Provides @Named("db.account") fun provideAccount(jdbc: TinyJdbc) : TinyJdbc {
		return dbAccount
	}
}
```

@AutoWeave create "weave" method in dagger commponent, you can call "TinyBird.get().weave(this)" in constructor to inject Dagger to target class, or do it automatically via aop [AutoWeaveHandle](example-app/src/main/kotlin/aop/AutoWeaveHandle.kt)

```kotlin
package myapp.dao

import javax.inject.Inject
import javax.inject.Named
import tiny.weaver.TinyBird
import tiny.annotation.AutoWeave

class AccountDao {

	@Inject @Named("db.account")
	private lateinit var _dbAccount: TinyJdbc

	@AutoWeave fun constructor(){
		/* do not need this if you have a aop */
		TinyBird.get().weave(this)
	}

	fun getUserInfo (userId: Long): SqlResult<Map<String, Any>> {

		val p = HashMap<String, Any>(
			"id" to userId,
		)
		val info = _dbAccount.select("select id, name from user where id = :id", p)
		return info
	}
}
```
##### Use TinyLog

```kotlin
/*
 * create log file in ${log.path}/yyyy/mm/myevent_dd.log with content:
 * 1998-01-01T16:25:21+0800 my message
 */
TinyLog.log("my message", "myevent")
```

##### Create batch command or crontab job
To run a batch command, you need to avoid those things:

* Don't use logback log to the same file, since logback lock the opened file.
* Don't autoload redis and database, since big resource pool can be used in production environment, for example, you may have hikari.minimumIdle == hikari.maximumPoolSize, once you start a batch command, it creates maximumPoolSize connections immediately.
```kotlin
fun main(args: Array<String>) {
	val name = "myapp"
	val env = System.getProperty("tiny.app.env") ?: "production"		
	val script = System.getProperty("tiny.app.script") ?: ""
	if(!script.isEmpty()){
		TinyScript.run(env, name, script)
	}
}
```
java -jar myapp.jar -Dtiny.app.env=development -Dtiny.app.script=myapp.script.Hello

```kotlin
package myapp.script

import tiny.*
import tiny.lib.*

class Hello{

	fun run() {
		val redisConfig = TinyConfig("config/${TinyApp.getEnvString()}/redis.ini")
		val dbConfig = TinyConfig("config/${TinyApp.getEnvString()}/db.ini")

		val loader = TinyResourceLoader()
		loader.loadRedis(redisConfig, "redis.default", fixedPoolSize = 1)
		loader.loadJdbc(dbConfig, "db.account", fixedPoolSize = 1)

		printRegister()
		printHello()
		throw HelloScriptException("something is wrong")
	}

	fun printRegister() {
		DebugUtil.print(TinyRegistry.getStorage())
	}

	fun printHello() {
		println("this is hello script")
	}
}

private class HelloScriptException(message: String?) : Throwable(message)
```

##### Code reload
* For static and template files

```ini
static.extra.dir = src/main/resources/static
template.extra.dir = src/main/resources/templates
```
Now you can refresh your browser and check your updates, please make sure the path exists, otherwise it fallback to classpath which have to update via your resource compile command

* For class file  

There is a [hotswap agent](https://github.com/HotswapProjects/HotswapAgent) plugin [tiny.hotswap.TinyHotSwap](tiny/src/main/kotlin/hotswap/TinyHotSwap.kt), once you have hotswap agent installed

```ini
pluginPackages=tiny.hotswap

autoHotswap=false
disabledPlugins=Hibernate, Hibernate3JPA, Hibernate3, Spring, Jersey1, Jersey2, Jetty, Tomcat, ZK, Logback, Log4j2, MyFaces, Mojarra, Omnifaces, Seam, ELResolver, WildFlyELResolver, OsgiEquinox, Owb, WebObjects, Weld, JBossModules, ResteasyRegistry, Deltaspike, GlassFish, Vaadin, Wicket
```
Add this to [hotswap-agent.properties](example-app/src/main/resources/hotswap-agent.properties), compile classes and check you work.

That's all. Hope you enjoy it:)
