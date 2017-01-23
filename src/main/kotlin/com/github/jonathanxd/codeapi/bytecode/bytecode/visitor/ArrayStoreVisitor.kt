package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.ArrayAccess
import com.github.jonathanxd.codeapi.interfaces.ArrayStore
import com.github.jonathanxd.codeapi.util.gen.CodeTypeUtil
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object ArrayStoreVisitor : VoidVisitor<ArrayStore, BytecodeClass, MVData> {

    override fun voidVisit(t: ArrayStore, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        visitorGenerator.generateTo(ArrayAccess::class.java, t, extraData, null, additional)

        val index = t.index

        visitorGenerator.generateTo(index.javaClass, index, extraData, null, additional)

        var value = t.valueToStore
        val valueType = t.valueType

        val arrayComponentType = t.arrayType.arrayComponent

        if (!arrayComponentType.`is`(valueType)) { // Auto casting.
            value = CodeAPI.cast(valueType, arrayComponentType, value)
        }

        visitorGenerator.generateTo(value.javaClass, value, extraData, null, additional)

        val opcode = CodeTypeUtil.getOpcodeForType(arrayComponentType, Opcodes.IASTORE)

        additional.methodVisitor.visitInsn(opcode)
    }

}