package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.ArrayAccess
import com.github.jonathanxd.codeapi.interfaces.ArrayLoad
import com.github.jonathanxd.codeapi.util.gen.CodeTypeUtil
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object ArrayLoadVisitor : VoidVisitor<ArrayLoad, BytecodeClass, MVData> {

    override fun voidVisit(t: ArrayLoad, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        visitorGenerator.generateTo(ArrayAccess::class.java, t, extraData, null, additional)

        val index = t.index

        visitorGenerator.generateTo(index.javaClass, index, extraData, null, additional)

        val valueType = t.valueType

        val arrayComponentType = t.arrayType.arrayComponent

        val opcode = CodeTypeUtil.getOpcodeForType(valueType, Opcodes.IALOAD)

        additional.methodVisitor.visitInsn(opcode)

        if (!arrayComponentType.`is`(valueType)) {
            val cast = CodeAPI.cast(valueType, arrayComponentType, null)
            visitorGenerator.generateTo(cast.javaClass, cast, extraData, additional)
        }
    }

}