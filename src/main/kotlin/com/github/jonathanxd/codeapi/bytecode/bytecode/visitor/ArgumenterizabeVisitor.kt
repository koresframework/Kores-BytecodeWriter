package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.Argumenterizable
import com.github.jonathanxd.codeapi.util.gen.CodeTypeUtil
import com.github.jonathanxd.codeapi.util.gen.InsnUtil
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.optional.Require
import org.objectweb.asm.Opcodes

object ArgumenterizabeVisitor : VoidVisitor<Argumenterizable, BytecodeClass, MVData> {

    override fun voidVisit(argumenterizable: Argumenterizable, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val arguments = argumenterizable.arguments

        if (!argumenterizable.isArray) {

            for (argument in arguments) {
                val value = Require.require(argument.value)

                visitorGenerator.generateTo(value.javaClass, value, extraData, null, additional)

                if (argument.isCasted) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, CodeTypeUtil.codeTypeToSimpleAsm(Require.require(argument.type)))
                }
            }
        } else {
            for (i in arguments.indices) {

                InsnUtil.visitInt(i, mv) // Visit index

                val argument = arguments[i]

                val value = Require.require(argument.value)

                visitorGenerator.generateTo(value.javaClass, value, extraData, null, additional)

                if (argument.isCasted) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, CodeTypeUtil.codeTypeToSimpleAsm(Require.require(argument.type)))
                }
            }
        }
    }

}