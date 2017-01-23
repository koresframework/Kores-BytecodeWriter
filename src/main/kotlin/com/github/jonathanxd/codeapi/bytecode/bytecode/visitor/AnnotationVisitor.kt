package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.Annotation
import com.github.jonathanxd.codeapi.util.AnnotationVisitorCapable
import com.github.jonathanxd.codeapi.util.asm.ParameterVisitor
import com.github.jonathanxd.codeapi.util.gen.AnnotationUtil
import com.github.jonathanxd.iutils.data.MapData

object AnnotationVisitor : VoidVisitor<Annotation, BytecodeClass, Any> {

    override fun voidVisit(t: Annotation, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
        val classWriterOpt =
                if (additional == null) {
                    extraData.getOptional(TypeVisitor.CLASS_WRITER_REPRESENTATION)
                } else {
                    null
                }

        var annotationVisitorCapable: AnnotationVisitorCapable? = null

        when (additional) {
            null -> if (classWriterOpt!!.isPresent) {
                annotationVisitorCapable = AnnotationVisitorCapable.ClassWriterVisitorCapable(classWriterOpt.get())
            }

            is MVData -> annotationVisitorCapable = AnnotationVisitorCapable.MethodVisitorCapable(additional.methodVisitor)
            is org.objectweb.asm.FieldVisitor -> annotationVisitorCapable = AnnotationVisitorCapable.FieldVisitorCapable(additional)
            is ParameterVisitor -> annotationVisitorCapable = AnnotationVisitorCapable.ParameterVisitorCapable(additional)
        }

        checkNotNull(annotationVisitorCapable, { "Cannot determine Annotation visitor!!" })

        AnnotationUtil.visitAnnotation(t, annotationVisitorCapable)
    }

}