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

import tiny.annotation.TinyControllers
import tiny.annotation.TinyHelpers
import tiny.annotation.TinyApplication
import tiny.annotation.Helper

import tiny.TinyController
import tiny.TinyView

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(value = arrayOf(
	"tiny.annotation.TinyControllers",
	"tiny.annotation.TinyHelpers", 
	"tiny.annotation.TinyApplication",
	"tiny.annotation.Helper"
	))
class AnnotationProcessor : AbstractProcessor() {

	private lateinit var _messager: Messager
	private lateinit var _elements: Elements
	private lateinit var _types: Types
	private lateinit var _filer: Filer
	private val _helperMap : HashMap<String, TypeElement> = HashMap()
	private val _controllerMap : HashMap<String, TypeElement> = HashMap()
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

	private fun printMessage(msg: String) {
		_messager.printMessage(MANDATORY_WARNING, msg)
	}

	private fun printError(msg: String) {
		_messager.printMessage(ERROR, msg)
	}	

	override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
		_foundSomething = false

		if(annotations == null){
			return true
		}

		/*handle @Helper begin*/
		for (ele in roundEnv.getElementsAnnotatedWith(Helper::class.java)){
			if (ele.getKind() != ElementKind.CLASS){
				printError("@"+Helper::class.java.getName() + " can only apply on class, incorrect usage on: "+ ele)
			}
			val classElement = ele as TypeElement
			_helperMap.put(classElement.getSimpleName().toString(), classElement)
			_foundSomething = true
		}
		/*handle @Helper end*/

		/*handle @TinyControllers begin*/
		for (ele in roundEnv.getElementsAnnotatedWith(TinyControllers::class.java)){
			if (ele.getKind() != ElementKind.CLASS){
				printError("@"+TinyControllers::class.java.getName() + " can only apply on class, incorrect usage on: "+ ele)
			}
			val classElement = ele as TypeElement
			val annotation = classElement.getAnnotation(TinyControllers::class.java)
			val value = annotation.value
			controllerScan(value)
		}
		/*handle @TinyControllers end*/

		/*handle @TinyHelpers begin*/
		for (ele in roundEnv.getElementsAnnotatedWith(TinyHelpers::class.java)){
			if (ele.getKind() != ElementKind.CLASS){
				printError("@"+TinyHelpers::class.java.getName() + " can only apply on class, incorrect usage on: "+ ele)
			}			
			val classElement = ele as TypeElement
			val annotation = classElement.getAnnotation(TinyHelpers::class.java)
			val value = annotation.value
			helperScan(value)
		}
		/*handle @TinyHelpers end*/

		/*handle @TinyApplication begin*/
		for (ele in roundEnv.getElementsAnnotatedWith(TinyApplication::class.java)){
			if (ele.getKind() != ElementKind.CLASS){
				printError("@"+TinyApplication::class.java.getName() + " can only apply on class, incorrect usage on: "+ ele)
			}
			val classElement = ele as TypeElement
			val annotation = classElement.getAnnotation(TinyApplication::class.java)
			val name = annotation.name
			var daggerModules: String = ""
			for(am in classElement.getAnnotationMirrors()){
				if(am.getAnnotationType().toString() == "tiny.annotation.TinyApplication"){
					for(entry in am.getElementValues()){
						if(entry.key.getSimpleName().toString() == "daggerModules"){
							daggerModules = entry.value.getValue().toString()
							break
						}
					}
					//printMessage(""+am.getElementValues().get("daggerModules"))
					break
				}
			}
			handleTinyApplication(name, daggerModules)
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
		writeHelperLoader()
	}

	private fun controllerScan(pkgList: String){
		val pagSet: MutableList<String> = mutableListOf()
		val pkgArr = pkgList.split(",")
		for(onePkg in pkgArr){
			val pkg = onePkg.trim()
			if(pkg.isEmpty()){
				continue
			}
			pagSet.add(pkg)
		}

		var _method = MethodSpec.methodBuilder("loadActions")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)

		for(pkg in pagSet){
			_method = _method.addStatement("\$T.loadControllers(\$S)", TinyController::class.java, pkg)
		}
		val _methodBuilder = _method.build()

		val scanClass = TypeSpec
				.classBuilder("TinyControllerScanner")
				.addModifiers(Modifier.PUBLIC)
				.addMethod(_methodBuilder)
				.build()
		val javaFile = JavaFile.builder("tiny.generated", scanClass).build()
		javaFile.writeTo(_filer)

	}

	

	private fun helperScan(pkgList: String){
		val pagSet: MutableList<String> = mutableListOf()
		val pkgArr = pkgList.split(",")
		for(onePkg in pkgArr){
			val pkg = onePkg.trim()
			if(pkg.isEmpty()){
				continue
			}
			pagSet.add(pkg)
		}

		var _method = MethodSpec.methodBuilder("loadHelpers")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)

		for(pkg in pagSet){
			_method = _method.addStatement("\$T.loadHelpers(\$S)", TinyView::class.java, pkg)
		}
		val _methodBuilder = _method.build()

		val scanClass = TypeSpec
				.classBuilder("TinyHelperScanner")
				.addModifiers(Modifier.PUBLIC)
				.addMethod(_methodBuilder)
				.build()
		val javaFile = JavaFile.builder("tiny.generated", scanClass).build()
		javaFile.writeTo(_filer)		
	}

	private fun handleTinyApplication(name:String, modList: String){
		val modSet: MutableList<String> = mutableListOf()
		val modArr = modList.split(",")
		for(oneMod in modArr){
			val mod = oneMod.trim()
			if(mod.isEmpty()){
				continue
			}
			modSet.add(mod)
		}

		/*write magicbox*/
		val _clzName = ClassName.get("example", "ExampleBootstrap")

		val _method = MethodSpec.methodBuilder("inject")
			.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
			.addParameter(_clzName, "app")
			.build()

		val _clzComponent = ClassName.get("dagger", "Component")
		val _clzAppModule = ClassName.get("example", "AppModule")
		val _anno = AnnotationSpec.builder(_clzComponent)
			.addMember("modules", "\$T.class", _clzAppModule)
			.build()

		val _interface = TypeSpec.interfaceBuilder("MagicBox")
		.addModifiers(Modifier.PUBLIC)
		.addMethod(_method)
		.addAnnotation(_anno)
		.build()

		val javaFile = JavaFile.builder("tiny.generated", _interface).build()
		javaFile.writeTo(_filer)
		/*write module*/
	}

	private fun writeHelperLoader() {

		var _method = MethodSpec.methodBuilder("loadHelpers")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)

		val _viewClz = ClassName.get("tiny", "TinyView")
		for(helper in _helperMap){
			var _helperClz = ClassName.get(helper.value)
			_method = _method.addStatement("\$T.addHelper(\$S, new \$L())", _viewClz, helper.key, _helperClz)
		}
		val _methodBuilder = _method.build()

		
		val _class = TypeSpec
				.classBuilder("TinyHelperLoader")
				.addModifiers(Modifier.PUBLIC)
				.addMethod(_methodBuilder)
				.build()
		val javaFile = JavaFile.builder("tiny.generated", _class).build()
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

		val javaFile = JavaFile.builder("tiny.generated", _class).build()
		javaFile.writeTo(_filer)
		_round ++				
	}

}

