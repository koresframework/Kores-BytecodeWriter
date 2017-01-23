package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.ArrayAccess
import com.github.jonathanxd.codeapi.interfaces.ArrayLength
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object ArrayLengthVisitor : VoidVisitor<ArrayLength, BytecodeClass, MVData> {

    override fun voidVisit(t: ArrayLength, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        visitorGenerator.generateTo(ArrayAccess::class.java, t, extraData, null, additional)

        additional.methodVisitor.visitInsn(Opcodes.ARRAYLENGTH)
    }

}