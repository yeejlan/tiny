package tiny.compiler

import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.WARNING
import javax.tools.Diagnostic.Kind.MANDATORY_WARNING

import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.element.*

import com.squareup.javapoet.*

import tiny.annotation.TinyApplication
import tiny.annotation.AutoWeave
import tiny.annotation.WeaverBird
import tiny.annotation.Controller
import tiny.annotation.Helper

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(value = arrayOf(
	"tiny.annotation.TinyApplication",
	"tiny.annotation.AutoWeave",
	"tiny.annotation.WeaverBird",
	"tiny.annotation.Controller",
	"tiny.annotation.Helper"
	))
class AnnotationProcessor : AbstractProcessor() {

	private val _web = "tiny.web"
	private val _weaver = "tiny.weaver"
	private lateinit var _messager: Messager
	private lateinit var _elements: Elements
	private lateinit var _types: Types
	private lateinit var _filer: Filer
	private var _tinyApplication: TypeElement? = null
	private val _helperMap : HashMap<String, TypeElement> = HashMap()
	private val _controllerMap : HashMap<String, TypeElement> = HashMap()
	private val _autoWeaveMap : HashMap<String, TypeElement> = HashMap()
	private val _weaverBirdMap : HashMap<String, TypeElement> = HashMap()
	private var _round: Long = 1
	private var _lastRound = false
	private var _foundSomething = false

	@Synchronized override fun init(processingEnv: ProcessingEnvironment) {
		super.init(processingEnv)
		_messager = processingEnv.getMessager()
		_elements = processingEnv.getElementUtils()
		_types = processingEnv.getTypeUtils()
		_filer = processingEnv.getFiler()

	}

	private fun printMessage(msg: Any) {
		_messager.printMessage(MANDATORY_WARNING, ""+msg)
	}

	private fun printError(msg: Any) {
		_messager.printMessage(ERROR, ""+msg)
	}	

	override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
		_foundSomething = false

		if(annotations == null){
			return true
		}

		/*handle @Controller begin*/
		for (ele in roundEnv.getElementsAnnotatedWith(Controller::class.java)){
			if (ele.getKind() != ElementKind.CLASS){
				continue
			}
			val classElement = ele as TypeElement
			_controllerMap.put(classElement.getQualifiedName().toString(), classElement)
			_foundSomething = true
		}
		/*handle @Controller end*/

		/*handle @Helper begin*/
		for (ele in roundEnv.getElementsAnnotatedWith(Helper::class.java)){
			if (ele.getKind() != ElementKind.CLASS){
				continue
			}
			val classElement = ele as TypeElement
			_helperMap.put(classElement.getSimpleName().toString(), classElement)
			_foundSomething = true
		}
		/*handle @Helper end*/		

		/*handle @AutoWeave begin*/
		for (ele in roundEnv.getElementsAnnotatedWith(AutoWeave::class.java)){
			if (ele.getKind() != ElementKind.CONSTRUCTOR){
				continue
			}
			val exeElement = ele as ExecutableElement
			val parentElement = exeElement.getEnclosingElement()
			if(parentElement.getKind() != ElementKind.CLASS){
				printError("Could not found class define for: " + exeElement.getSimpleName())
			}
			val classElement = parentElement as TypeElement
			_autoWeaveMap.put(classElement.getQualifiedName().toString(), classElement)
			_foundSomething = true
		}
		/*handle @AutoWeave end*/

		/*handle @WeaverBird begin*/
		for (ele in roundEnv.getElementsAnnotatedWith(WeaverBird::class.java)){
			if (ele.getKind() != ElementKind.CLASS){
				continue
			}
			val classElement = ele as TypeElement
			_weaverBirdMap.put(classElement.getQualifiedName().toString(), classElement)
			_foundSomething = true
		}
		/*handle @WeaverBird end*/

		/*handle @TinyApplication begin*/
		for (ele in roundEnv.getElementsAnnotatedWith(TinyApplication::class.java)){
			if (ele.getKind() != ElementKind.CLASS){
				printError("@"+TinyApplication::class.java.getName() + " can only apply on class, incorrect usage on: "+ ele)
			}
			val classElement = ele as TypeElement
			_tinyApplication = classElement
			_foundSomething = true
		}
		/*handle @TinyApplication end*/

		if(_foundSomething && !_lastRound){
			forceNextRound()
		}

		if(!_foundSomething && !_lastRound){
			_lastRound = true
			writeStuff()
		}

		return true
	}

	private fun writeStuff(){

		writeControllerLoader()
		writeHelperLoader()
		writeTinyServlet()
		writeTinyFileCleanerCleanupListener()
		writeMagicBox()
		writeTinyBird()

	}

	private fun writeTinyBird(){
		val _clzDaggerMagicBox = ClassName.get("tiny.weaver", "DaggerMagicBox")
		val _clzMagicBox = ClassName.get("tiny.weaver", "MagicBox")

		val _methodGet = MethodSpec.methodBuilder("get")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
			.returns(_clzMagicBox)
			.addStatement("return theMagicBox")
			.build()

		val _field = FieldSpec.builder(_clzMagicBox, "theMagicBox")
			.addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
			.initializer("\$T.create()", _clzDaggerMagicBox)
			.build()

		val _class = TypeSpec
			.classBuilder("TinyBird")
			.addModifiers(Modifier.PUBLIC)
			.addField(_field)
			.addMethod(_methodGet)
			.build()

		val javaFile = JavaFile.builder(_weaver, _class).build()
		javaFile.writeTo(_filer)
	}


	private fun writeMagicBox(){

		val _clzComponent = ClassName.get("dagger", "Component")

		val _annoBuilder = AnnotationSpec.builder(_clzComponent)

		for(weaverBird in _weaverBirdMap){
			_annoBuilder.addMember("modules", "\$T.class", weaverBird.value)
		}
		val _anno = _annoBuilder.build()

		val _interfaceBuilder = TypeSpec.interfaceBuilder("MagicBox")
			.addModifiers(Modifier.PUBLIC)
			.addAnnotation(_anno)

		for(autoWeave in _autoWeaveMap){
			val _clzName = ClassName.get(autoWeave.value)
			val _method = MethodSpec.methodBuilder("weave")
				.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
				.addParameter(_clzName, "obj")
				.build()	
			_interfaceBuilder.addMethod(_method)
		}

		val _interface = _interfaceBuilder.build()

		val javaFile = JavaFile.builder(_weaver, _interface).build()
		javaFile.writeTo(_filer)
	}

	private fun writeTinyServlet(){
		if(_tinyApplication == null){
			printError("No @TinyApplication found, please provide one")
			return
		}

		val _clzApp = ClassName.get(_tinyApplication)
		val _clzServlet = ClassName.get("javax.servlet.http", "HttpServlet")
		val _clzRequest = ClassName.get("javax.servlet.http", "HttpServletRequest")
		val _clzResponse = ClassName.get("javax.servlet.http", "HttpServletResponse")
		val _clzAnnoServlet = ClassName.get("javax.servlet.annotation", "WebServlet")
		val _clzTinyRouter = ClassName.get("tiny", "TinyRouter")
		val _clzTinyHelperLoader = ClassName.get("tiny.web", "TinyHelperLoader")
		val _clzTinyControllerLoader = ClassName.get("tiny.web", "TinyControllerLoader")
		val _clzServletException = ClassName.get("javax.servlet", "ServletException")
		val _clzIOException = ClassName.get("java.io", "IOException")
		val _clzTinyApp = ClassName.get("tiny", "TinyApp")

		val _anno = AnnotationSpec.builder(_clzAnnoServlet)
			.addMember("name", "\$S", "TinyServlet")
			.addMember("urlPatterns", "\$S", "/*")
			.addMember("loadOnStartup", "1")
			.build()

		val _methodInit = MethodSpec.methodBuilder("init")
			.addModifiers(Modifier.PUBLIC)
			.addException(_clzServletException)
			.addStatement("new \$T().bootstrap()", _clzApp)
			.addStatement("\$T.loadActions()", _clzTinyControllerLoader)
			.addStatement("\$T.loadHelpers()", _clzTinyHelperLoader)
			.build()

		val _methodDoGet = MethodSpec.methodBuilder("doGet")
			.addModifiers(Modifier.PUBLIC)
			.addException(_clzServletException)
			.addException(_clzIOException)
			.addParameter(_clzRequest, "request")
			.addParameter(_clzResponse, "response")
			.addStatement("\$T.dispatch(request, response)", _clzTinyRouter)
			.build()

		val _methodDoPost = MethodSpec.methodBuilder("doPost")
			.addModifiers(Modifier.PUBLIC)
			.addException(_clzServletException)
			.addException(_clzIOException)
			.addParameter(_clzRequest, "request")
			.addParameter(_clzResponse, "response")
			.addStatement("\$T.dispatch(request, response)", _clzTinyRouter)
			.build()

		val _methodDestroy = MethodSpec.methodBuilder("destroy")
			.addModifiers(Modifier.PUBLIC)
			.addStatement("\$T.shutdown()", _clzTinyApp)
			.build()

		val _class = TypeSpec
				.classBuilder("TinyServlet")
				.superclass(_clzServlet)
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(_anno)
				.addMethod(_methodInit)
				.addMethod(_methodDoGet)
				.addMethod(_methodDoPost)
				.addMethod(_methodDestroy)
				.build()

		val javaFile = JavaFile.builder(_web, _class).build()
		javaFile.writeTo(_filer)
	}

	private fun writeTinyFileCleanerCleanupListener() {
		val _clzFileCleanup = ClassName.get("org.apache.commons.fileupload.servlet", "FileCleanerCleanup")
		val _clzWebListener = ClassName.get("javax.servlet.annotation", "WebListener")

		val _class = TypeSpec
				.classBuilder("TinyFileCleanerCleanup")
				.superclass(_clzFileCleanup)
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(_clzWebListener)
				.build()

		val javaFile = JavaFile.builder(_web, _class).build()
		javaFile.writeTo(_filer)
	}

	private fun writeHelperLoader() {

		val _methodBuilder = MethodSpec.methodBuilder("loadHelpers")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)

		val _viewClz = ClassName.get("tiny", "TinyView")
		for(helper in _helperMap){
			if(!helper.key.endsWith("Helper")){
				continue
			}
			var _helperClz = ClassName.get(helper.value)
			val helperStr = helper.key.substring(0, helper.key.length - "Helper".length)
			_methodBuilder.addStatement("\$T.addHelper(\$S, new \$L())", _viewClz, helperStr, _helperClz)
		}
		val _method = _methodBuilder.build()

		
		val _class = TypeSpec
				.classBuilder("TinyHelperLoader")
				.addModifiers(Modifier.PUBLIC)
				.addMethod(_method)
				.build()
		val javaFile = JavaFile.builder(_web, _class).build()
		javaFile.writeTo(_filer)	
	}

	private fun writeControllerLoader() {
		val actionMap: HashMap<String, Pair<TypeElement,String>> = HashMap()
		for(oneController in _controllerMap){
			val clz = oneController.value
			for(ele in clz.getEnclosedElements()){
				if (ele.getKind() != ElementKind.METHOD){
					continue
				}
				if(!ele.getModifiers().contains(Modifier.PUBLIC))	{
					continue
				}
				val params = (ele as ExecutableElement).getParameters()
				if(!params.isEmpty()){
					continue
				}
				val actionStr = ele.getSimpleName().toString()
				if(!actionStr.endsWith("Action")){
					continue
				}
				val controllerStr = clz.getSimpleName().toString()
				if(!controllerStr.endsWith("Controller")){
					continue
				}
				val action = actionStr.substring(0, actionStr.length - "Action".length)
				val controller = controllerStr.substring(0, controllerStr.length - "Controller".length)
				val actionKey = "${controller}/${action}".toLowerCase()
				actionMap.put(actionKey, Pair(clz, actionStr))
			}
		}

		val _methodBuilder = MethodSpec.methodBuilder("loadActions")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)

		val _tinyControllerClz = ClassName.get("tiny", "TinyController")
		val _tinyActionPairClz = ClassName.get("tiny", "ActionPair")
		for(action in actionMap){
			val actionPair = action.value
			val _controllerClz = ClassName.get(actionPair.first)
			_methodBuilder.addStatement("\$T.addAction(\$S, new \$T(\$T.class, \$S))",
							_tinyControllerClz, action.key,  _tinyActionPairClz, _controllerClz, actionPair.second)
		}
		val _method = _methodBuilder.build()

		
		val _class = TypeSpec
				.classBuilder("TinyControllerLoader")
				.addModifiers(Modifier.PUBLIC)
				.addMethod(_method)
				.build()
		val javaFile = JavaFile.builder(_web, _class).build()
		javaFile.writeTo(_filer)	
	}	

	private fun forceNextRound() {
		if(_round>10) {
			return
		}
		//val _clz = ClassName.get("tiny.annotation", "Processor")
		val _class = TypeSpec
				.classBuilder("TinyProcesorRound" + _round)
				.addModifiers(Modifier.PUBLIC)
				//.addAnnotation(_clz)
				.build()

		val javaFile = JavaFile.builder(_web, _class).build()
		javaFile.writeTo(_filer)
		_round ++				
	}

}

