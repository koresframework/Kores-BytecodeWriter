package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.ForEachBlock
import com.github.jonathanxd.iutils.data.MapData

object ForEachVisitor : VoidVisitor<ForEachBlock, BytecodeClass, MVData> {

    override fun voidVisit(t: ForEachBlock, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val iterationType = t.iterationType
        val start = iterationType.generator

        val generated = start.generate(t)

        visitorGenerator.generateTo(CodeSource::class.java, generated, extraData, null, additional)
    }

}