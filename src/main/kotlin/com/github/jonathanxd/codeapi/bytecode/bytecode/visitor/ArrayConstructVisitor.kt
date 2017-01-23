package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.ArrayConstructor
import com.github.jonathanxd.codeapi.interfaces.ArrayStore
import com.github.jonathanxd.codeapi.literals.Literals
import com.github.jonathanxd.codeapi.util.gen.ArrayUtil
import com.github.jonathanxd.codeapi.util.gen.CodeTypeUtil
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object ArrayConstructVisitor : VoidVisitor<ArrayConstructor, BytecodeClass, MVData> {

    override fun voidVisit(t: ArrayConstructor, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor
        val arguments = t.arguments

        val initialize = arguments.isNotEmpty()
        val dimensions = t.dimensions
        val multi = dimensions.size > 1

        if(multi && !initialize) {
            dimensions.forEach {
                visitorGenerator.generateTo(it.javaClass, it, extraData, null, additional)
            }

            mv.visitMultiANewArrayInsn(CodeTypeUtil.codeTypeToArray(t.arrayType, dimensions.size), dimensions.size)
        } else {
            val dimensionX = if (dimensions.isNotEmpty()) dimensions[0] else Literals.INT(0)

            visitorGenerator.generateTo(dimensionX.javaClass, dimensionX, extraData, null, additional)

            ArrayUtil.visitArrayStore(t.arrayType, dimensions.size, mv) // ANEWARRAY, ANEWARRAY T_INT, etc...
        }

        if (initialize) {
            // Initialize

            for (arrayStore in t.arrayValues) {
                mv.visitInsn(Opcodes.DUP)
                visitorGenerator.generateTo(ArrayStore::class.java, arrayStore, extraData, null, additional)
            }
        }

    }

}