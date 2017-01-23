package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.TagLine
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label

object TagLineVisitor : VoidVisitor<TagLine<*, *>, BytecodeClass, MVData> {

    override fun voidVisit(t: TagLine<*, *>, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val value = t.value

        val label = Label()

        extraData.registerData(VisitorGenerator.LINES_REPRESENTATION, t)

        val line = extraData.getAll(VisitorGenerator.LINES_REPRESENTATION).size - 1

        mv.visitLabel(label)

        mv.visitLineNumber(line, label)

        visitorGenerator.generateTo(value.javaClass, value, extraData, null, additional)

        mv.visitLabel(Label())
    }

}