package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.common.Flow
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.impl.IfBlockImpl
import com.github.jonathanxd.codeapi.interfaces.ForBlock
import com.github.jonathanxd.codeapi.interfaces.IfBlock
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object ForIVisitor : VoidVisitor<ForBlock, BytecodeClass, MVData> {

    override fun voidVisit(t: ForBlock, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val outsideStart = Label()
        val whileStart = Label()
        val whileEnd = Label()
        val outsideEnd = Label()

        mv.visitLabel(outsideStart)

        val init = t.forInit

        if(init.isPresent) {
            visitorGenerator.generateTo(init.get().javaClass, init.get(), extraData, null, additional)
        }


        val source = MutableCodeSource()

        t.body.ifPresent({ source.addAll(it) })

        val ifBlock = IfBlockImpl.instance(t, source)

        mv.visitLabel(whileStart)

        val flow = Flow(outsideStart, whileStart, whileEnd, outsideEnd)

        extraData.registerData(ConstantDatas.FLOW_TYPE_INFO, flow)

        val instructionCodePart = InstructionCodePart.create { value, extraData1, visitorGenerator1, additional ->
            mv.visitLabel(whileEnd)
            val update = t.forUpdate

            if(update.isPresent) {
                visitorGenerator.generateTo(update.get().javaClass, update.get(), extraData, null, additional)
            }

            mv.visitJumpInsn(Opcodes.GOTO, whileStart)
        }

        source.add(instructionCodePart)

        visitorGenerator.generateTo(IfBlock::class.java, ifBlock, extraData, null, additional)

        extraData.unregisterData(ConstantDatas.FLOW_TYPE_INFO, flow)

        mv.visitLabel(outsideEnd)
    }

}