package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.IfBlock
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label

object IfBlockVisitor : VoidVisitor<IfBlock, BytecodeClass, MVData> {

    override fun voidVisit(t: IfBlock, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val startIfLabel = Label()
        val endIfLabel = Label()

        val methodVisitor = additional.methodVisitor

        methodVisitor.visitLabel(startIfLabel)

        BytecodeIfBlockVisitor.visit(t, startIfLabel, endIfLabel, false, false, extraData, visitorGenerator, additional)

        methodVisitor.visitLabel(endIfLabel)
    }

}