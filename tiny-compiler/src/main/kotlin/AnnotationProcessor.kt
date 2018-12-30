package tiny.compiler

import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.WARNING
import javax.tools.Diagnostic.Kind.MANDATORY_WARNING

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("tiny.annotation.Controller")
class AnnotationProcessor : AbstractProcessor() {
	override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        //processingEnv.messager.printMessage(MANDATORY_WARNING,"Enter AnnotationProcessor.process")
        if(annotations == null){
        	return true
        }
        for (typeElement in annotations) {
            //processingEnv.messager.printMessage(MANDATORY_WARNING, typeElement.toString())
        }
        //processingEnv.messager.printMessage(MANDATORY_WARNING, roundEnv.toString())
        
        return true
	}
}