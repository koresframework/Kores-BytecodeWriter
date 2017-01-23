package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.iutils.data.MapData

internal interface InstructionCodePart : CodePart {

    fun apply(value: Any, extraData: MapData, visitorGenerator: VisitorGenerator<*>, additional: Any?)

    companion object {
        fun create(func: (Any, MapData, VisitorGenerator<*>, Any?) -> Unit): InstructionCodePart {
            return object: InstructionCodePart {
                override fun apply(value: Any, extraData: MapData, visitorGenerator: VisitorGenerator<*>, additional: Any?) {
                    func(value, extraData, visitorGenerator, additional)
                }
            }
        }
    }

    object InstructionCodePartVisitor : VoidVisitor<InstructionCodePart, BytecodeClass, Any?> {

        override fun voidVisit(instructionCodePart: InstructionCodePart, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
            instructionCodePart.apply(instructionCodePart, extraData, visitorGenerator, additional)
        }

    }

}
