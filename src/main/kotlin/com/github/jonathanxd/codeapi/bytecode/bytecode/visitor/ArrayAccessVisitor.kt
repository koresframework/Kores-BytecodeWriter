package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.ArrayAccess
import com.github.jonathanxd.iutils.data.MapData

object ArrayAccessVisitor : VoidVisitor<ArrayAccess, BytecodeClass, MVData> {

    override fun voidVisit(t: ArrayAccess, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData?) {
        val targetOpt = t.target

        if(targetOpt.isPresent) {
            val target = targetOpt.get()

            visitorGenerator.generateTo(target.javaClass, target, extraData, null, additional)
        }
    }

}