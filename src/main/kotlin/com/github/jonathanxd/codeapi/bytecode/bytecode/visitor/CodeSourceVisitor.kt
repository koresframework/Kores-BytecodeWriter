package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.iutils.data.MapData

object CodeSourceVisitor : Visitor<CodeSource, BytecodeClass, Any?> {

    override fun visit(t: CodeSource, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?): Array<BytecodeClass> {
        val appender = visitorGenerator.createAppender()

        for (i in 0..t.size() - 1) {
            val codePart = t.get(i)

            val aClass = codePart.javaClass

            visitorGenerator.generateTo(aClass, codePart, extraData, appender::add, additional)
        }

        return appender.get()
    }

}