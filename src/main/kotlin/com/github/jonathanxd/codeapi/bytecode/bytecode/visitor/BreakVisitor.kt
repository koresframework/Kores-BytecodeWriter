package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.Break
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object BreakVisitor : VoidVisitor<Break, BytecodeClass, MVData> {

    override fun voidVisit(t: Break, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val optional = extraData.getOptional(ConstantDatas.FLOW_TYPE_INFO)

        val flow = optional.orElseThrow { IllegalArgumentException("Cannot 'break' outside a flow!") }

        additional.methodVisitor.visitJumpInsn(Opcodes.GOTO, flow.outsideEnd)
    }

}