package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.literals.Literal
import com.github.jonathanxd.codeapi.util.gen.LiteralUtil
import com.github.jonathanxd.iutils.data.MapData

object LiteralVisitor : Visitor<Literal, BytecodeClass, MVData> {

    override fun visit(t: Literal, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData): Array<BytecodeClass> {
        LiteralUtil.visitLiteral(t, additional.methodVisitor)

        return emptyArray()
    }

}