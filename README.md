# tiny
A Kotlin/Java mvc framework inspired by zend framework 1

### Reinvent the wheel?
I want to write xml config files as little as possible, thus say goodbye to [Spring Boot](https://spring.io/projects/spring-boot), also want to make the controller class threadsafe, so I can store temporary variables in base controller and use them later, rather than inject a request scoped object to each controller. And I like the idea of mapping URI directly to actions. Here comes this new framework.

### Kotlin or Java?
In short, Kotlin is better Java.

##### The good stuff
* No checked error(May I say stupid feature in java?)
* String("abc") == String("abc") is true
* Auto type inference  
```
	val longValue = 5L
```	
* String expression  
```
	logger.warn("script running error, file: ${script.file}, line: ${script.getCurrentLine()} at $script")
```
* Multiline String
```
	val text = """first line
		second line
		third line
	""".trimMargin()
```
* Operator override  
```  
	val cat = Cargo["cat"]
	Cargo["cat"] = Cat()
```	
Instead of  
```
	Cat cat = Cargo.get("cat")
	Cargo.set("cat") = new Cat()
```
* Default parameter  
```
	setCache(key: String, value: String, expireSeconds: Long = 3600)
```	
* Call method via named parameter 
``` 
	setCookie("userid", userId, domain = "example.com", path = "/", maxAge = 3600)
```	
* Data class  
```
	data class HelperPair(val name: String, val clazz: Class<*>)
	val loggerHelper = HelperPair("logger helper", com.example.LoggerHelper)
	println(loggerHelper.name)
	val logger= loggerHelper.clazz.newInstance()
```	
* singleton class  
```
	object ASingletonClass(){val name = "singleton"}
```	
* import alias  

```
	import System.out.print as p
	import System.out.println as pn
```	
* and many others, such as auto close resource with use{...} etc

##### The bad things
For now, Kotlin compiler is pretty bad at syntax error tips, especially if compiler can't find your annotation, the only hint you got is somthing like "@error.NonExistentClass()"




