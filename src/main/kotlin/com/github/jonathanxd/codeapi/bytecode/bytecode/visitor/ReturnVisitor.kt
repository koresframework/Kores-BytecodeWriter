package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.helper.PredefinedTypes
import com.github.jonathanxd.codeapi.interfaces.Return
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object ReturnVisitor : VoidVisitor<Return, BytecodeClass, MVData> {

    override fun voidVisit(t: Return, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val tValue = t.value

        if (tValue.isPresent) {
            visitorGenerator.generateTo(tValue.get().javaClass, tValue.get(), extraData, null, additional)
        }

        val toRet = t.type.orElse(null)

        var opcode = Opcodes.RETURN

        if (toRet != null) {

            if (!toRet.`is`(PredefinedTypes.VOID)) {
                val type = Type.getType(toRet.javaSpecName)

                opcode = type.getOpcode(Opcodes.IRETURN) // ARETURN
            }

        }

        mv.visitInsn(opcode)
    }

}