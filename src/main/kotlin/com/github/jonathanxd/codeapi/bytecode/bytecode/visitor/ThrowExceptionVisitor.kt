package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.ThrowException
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object ThrowExceptionVisitor : VoidVisitor<ThrowException, BytecodeClass, MVData> {

    override fun voidVisit(t: ThrowException, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val partToThrow = t.partToThrow

        visitorGenerator.generateTo(partToThrow.javaClass, partToThrow, extraData, null, additional)

        mv.visitInsn(Opcodes.ATHROW)
    }

}