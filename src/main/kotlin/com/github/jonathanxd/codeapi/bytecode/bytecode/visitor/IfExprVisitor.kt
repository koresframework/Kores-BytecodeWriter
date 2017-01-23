package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.IfBlock
import com.github.jonathanxd.codeapi.interfaces.IfExpr
import com.github.jonathanxd.codeapi.literals.Literals
import com.github.jonathanxd.iutils.data.MapData

object IfExprVisitor : VoidVisitor<IfExpr, BytecodeClass, MVData> {

    override fun voidVisit(t: IfExpr, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val ifBlock = CodeAPI.ifBlock(
                CodeAPI.ifExprs(t),
                CodeAPI.sourceOfParts(
                        Literals.BOOLEAN(true)
                ),
                CodeAPI.elseBlock(
                        Literals.BOOLEAN(false)
                ))

        visitorGenerator.generateTo(IfBlock::class.java, ifBlock, extraData, additional)
    }

}