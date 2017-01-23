package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.Annotable
import com.github.jonathanxd.codeapi.interfaces.Annotation
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object AnnotableVisitor : VoidVisitor<Annotable, BytecodeClass, Any?>, Opcodes {

    override fun voidVisit(t: Annotable, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
        t.annotations.forEach {
            visitorGenerator.generateTo(Annotation::class.java, it, extraData, null, additional)
        }
    }

}