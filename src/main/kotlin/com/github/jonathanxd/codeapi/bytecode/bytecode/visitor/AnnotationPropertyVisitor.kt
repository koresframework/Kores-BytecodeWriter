package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.Annotable
import com.github.jonathanxd.codeapi.interfaces.AnnotationProperty
import com.github.jonathanxd.codeapi.util.gen.AnnotationUtil
import com.github.jonathanxd.codeapi.util.gen.CodeTypeUtil
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object AnnotationPropertyVisitor : VoidVisitor<AnnotationProperty, BytecodeClass, Any?> {

    override fun voidVisit(t: AnnotationProperty, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
        val cw = Util.find(TypeVisitor.CLASS_WRITER_REPRESENTATION, extraData, additional)

        val asmModifiers = Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT

        val type = CodeTypeUtil.codeTypeToFullAsm(t.type.orElseThrow(::NullPointerException))
        val name = t.name
        val valueOpt = t.value

        val mv = cw.visitMethod(asmModifiers, name, "()" + type, null, null)

        if (valueOpt.isPresent) {
            val value = valueOpt.get()

            val annotationVisitor = mv.visitAnnotationDefault()

            AnnotationUtil.visitAnnotationValue(annotationVisitor, null, value)

            annotationVisitor.visitEnd()
        }

        val mvData = MVData(mv, mutableListOf())

        visitorGenerator.generateTo(Annotable::class.java, t, extraData, null, mvData)

        mv.visitEnd()
    }

}