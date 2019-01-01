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
	private var _tinyApp: TypeElement? = null
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
			_helperMap.put(classElement.getQualifiedName().toString(), classElement)
			_foundSomething = true
		}
		/*handle @Helper end*/		

		/*handle @AutoWeave begin*/
		for (ele in roundEnv.getElementsAnnotatedWith(AutoWeave::class.java)){
			if (ele.getKind() != ElementKind.CLASS){
				continue
			}
			val classElement = ele as TypeElement
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
			_tinyApp = classElement
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
		writeDaggerMagicModule()
		writeDaggerMagicBox()
		writeTinyBird()
		printMessage("=="+_autoWeaveMap)

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

	private fun writeDaggerMagicModule(){
		//provide controller initialize?
		val _clzModule = ClassName.get("dagger", "Module")
	}

	private fun writeDaggerMagicBox(){

		val _clzComponent = ClassName.get("dagger", "Component")
		val _clzModule = ClassName.get("dagger", "MagicBoxModule")

		val _annoBuilder = AnnotationSpec.builder(_clzComponent)
			//.addMember("modules", "\$T.class", _clzModule)
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
		if(_tinyApp == null){
			return
		}

		val annotation = (_tinyApp as TypeElement).getAnnotation(TinyApplication::class.java)
		val config = annotation.config

		val _clzServlet = ClassName.get("javax.servlet.http", "HttpServlet")
		val _clzRequest = ClassName.get("javax.servlet.http", "HttpServletRequest")
		val _clzResponse = ClassName.get("javax.servlet.http", "HttpServletResponse")
		val _clzAnnoServlet = ClassName.get("javax.servlet.annotation", "WebServlet")
		val _clzTinyApp = ClassName.get("tiny", "TinyApp")
		val _clzTinyView = ClassName.get("tiny", "TinyView")
		val _clzTinyHelperLoader = ClassName.get("tiny.web", "TinyHelperLoader")
		val _clzServletException = ClassName.get("javax.servlet", "ServletException")
		val _clzIOException = ClassName.get("java.io", "IOException")

		val _methodInit = MethodSpec.methodBuilder("init")
			.addModifiers(Modifier.PUBLIC)
			.addException(_clzServletException)
			.addStatement("""${'$'}T.init("testing", "config/development/tiny.properties")""", _clzTinyApp)
			.addStatement("\$T.bootstrap(new example.ExampleBootstrap())", _clzTinyApp)
			.addStatement("\$T.loadHelpers()", _clzTinyHelperLoader)
			.build()

		val _methodDoGet = MethodSpec.methodBuilder("doGet")
			.addModifiers(Modifier.PUBLIC)
			.addException(_clzServletException)
			.addException(_clzIOException)
			.addParameter(_clzRequest, "request")
			.addParameter(_clzResponse, "response")
			.addStatement("""response.setContentType("text/html;charset=UTF-8")""")
			.addStatement("java.io.PrintWriter out = response.getWriter()")
			.addStatement("\$T view = new \$T()", _clzTinyView, _clzTinyView)
			.addStatement("""out.println(view.render("body"))""")
			.build()

		val _methodDestroy = MethodSpec.methodBuilder("destroy")
			.addModifiers(Modifier.PUBLIC)
			.build()

		val _class = TypeSpec
				.classBuilder("TinyServlet")
				.superclass(_clzServlet)
				.addModifiers(Modifier.PUBLIC)
				.addMethod(_methodInit)
				.addMethod(_methodDoGet)
				.addMethod(_methodDestroy)
				.build()

		val javaFile = JavaFile.builder(_web, _class).build()
		javaFile.writeTo(_filer)
	}

	private fun writeHelperLoader() {

		val _methodBuilder = MethodSpec.methodBuilder("loadHelpers")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)

		val _viewClz = ClassName.get("tiny", "TinyView")
		for(helper in _helperMap){
			var _helperClz = ClassName.get(helper.value)
			_methodBuilder.addStatement("\$T.addHelper(\$S, new \$L())", _viewClz, helper.key, _helperClz)
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

		val _methodBuilder = MethodSpec.methodBuilder("loadActions")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)

		val _tinyControllerClz = ClassName.get("tiny", "TinyController")
		for(controller in _controllerMap){
			var _controllerClz = ClassName.get(controller.value)
			_methodBuilder.addStatement("\$T.addAction(\$S, \$L.class)", _tinyControllerClz, controller.key, _controllerClz)
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
		val _clz = ClassName.get("tiny.annotation", "Processor")
		val _class = TypeSpec
				.classBuilder("TinyProcesorRound" + _round)
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(_clz)
				.build()

		val javaFile = JavaFile.builder(_web, _class).build()
		javaFile.writeTo(_filer)
		_round ++				
	}

}

