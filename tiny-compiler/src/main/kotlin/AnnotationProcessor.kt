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
import javax.lang.model.element.*

import com.squareup.javapoet.*

import tiny.annotation.TinyControllers
import tiny.annotation.TinyHelpers
import tiny.annotation.DaggerInject

import tiny.TinyController
import tiny.TinyView

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(value = arrayOf("tiny.annotation.TinyControllers", "tiny.annotation.TinyHelpers", "tiny.annotation.DaggerInject"))
class AnnotationProcessor : AbstractProcessor() {

	private lateinit var _messager: Messager
	private lateinit var _elements: Elements
	private lateinit var _types: Types
	private lateinit var _filer: Filer

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

		if(annotations == null){
			return true
		}

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

		/*handle @DaggerInject begin*/
		for (ele in roundEnv.getElementsAnnotatedWith(DaggerInject::class.java)){
			if (ele.getKind() != ElementKind.CLASS){
				printError("@"+DaggerInject::class.java.getName() + " can only apply on class, incorrect usage on: "+ ele)
			}		
			val classElement = ele as TypeElement

			// public constructor check
			var pubConstructorExists = false
			for (enclosedEle in classElement.getEnclosedElements()) {
				if (enclosedEle.getKind() == ElementKind.CONSTRUCTOR) {
					val constructorEle = enclosedEle as ExecutableElement
					if (constructorEle.getParameters().size == 0 &&
							constructorEle.getModifiers().contains(Modifier.PUBLIC)) {
						pubConstructorExists = true
						break
					}
				}
			}
			if(false == pubConstructorExists){
				printError("@"+DaggerInject::class.java.getName() + " need a public constructor with empty params, which not found in class: "+ classElement)	
			}


			printMessage("" + classElement)
		}		
		/*handle @DaggerInject end*/

		return true
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
}

