package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.*
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object AccessVisitor : VoidVisitor<Access, BytecodeClass, MVData> {

    override fun voidVisit(t: Access, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val visitor = additional.methodVisitor

        when (t) {
            is AccessThis, is AccessSuper -> visitor.visitVarInsn(Opcodes.ALOAD, 0)
            is AccessLocal -> {
            }
            is AccessOuter -> {
                val localization = t.getLocalization().orElseThrow(::NullPointerException)
                val part = Util.accessEnclosingClass(extraData, localization) ?: throw IllegalArgumentException("Cannot access \"outer class\" '$localization'.")

                visitorGenerator.generateTo(part.javaClass, part, extraData, additional)
            }
            else -> {
                throw IllegalArgumentException("Cannot handle access '$t'")
            }
        }
    }

}