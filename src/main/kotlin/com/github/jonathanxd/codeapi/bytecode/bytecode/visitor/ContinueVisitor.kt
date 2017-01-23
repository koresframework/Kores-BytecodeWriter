package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.Continue
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object ContinueVisitor : VoidVisitor<Continue, BytecodeClass, MVData> {

    override fun voidVisit(t: Continue, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val visitor = additional.methodVisitor

        val optional = extraData.getOptional(ConstantDatas.FLOW_TYPE_INFO)

        val flow = optional.orElseThrow { IllegalArgumentException("Cannot 'continue' outside a flow!") }

        visitor.visitJumpInsn(Opcodes.GOTO, flow.insideEnd)
    }

}