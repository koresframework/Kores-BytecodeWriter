package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.InstanceOf
import com.github.jonathanxd.codeapi.util.gen.CodeTypeUtil
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object InstanceOfVisitor : VoidVisitor<InstanceOf, BytecodeClass, MVData> {

    override fun voidVisit(t: InstanceOf, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val visitor = additional.methodVisitor

        val part = t.part
        val codeType = t.checkType

        visitorGenerator.generateTo(part.javaClass, part, extraData, null, additional)

        visitor.visitTypeInsn(Opcodes.INSTANCEOF, CodeTypeUtil.codeTypeToSimpleAsm(codeType))
    }

}