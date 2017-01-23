package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.common.Flow
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.gen.visit.bytecode.visitor.ConstantDatas
import com.github.jonathanxd.codeapi.impl.IfBlockImpl
import com.github.jonathanxd.codeapi.interfaces.IfBlock
import com.github.jonathanxd.codeapi.interfaces.WhileBlock
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object WhileVisitor : VoidVisitor<WhileBlock, BytecodeClass, MVData> {

    override fun voidVisit(whileBlock: WhileBlock, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.getMethodVisitor()

        val whileStart = Label()
        val insideStart = Label()
        val insideEnd = Label()
        val outsideEnd = Label()

        val source = MutableCodeSource()

        val body = whileBlock.body

        if (body.isPresent) {
            source.addAll(body.get())
        }

        val ifBlock = IfBlockImpl.instance(whileBlock, source)

        mv.visitLabel(whileStart)

        val instructionCodePart = InstructionCodePart.create { value, extraData1, visitorGenerator1, additional ->
            mv.visitLabel(insideEnd) // Outside of while (continue;)
            mv.visitJumpInsn(Opcodes.GOTO, whileStart)
        }

        source.add(instructionCodePart)

        mv.visitLabel(insideStart)

        val flow = Flow(whileStart, insideStart, insideEnd, outsideEnd)

        extraData.registerData(ConstantDatas.FLOW_TYPE_INFO, flow)

        visitorGenerator.generateTo(IfBlock::class.java, ifBlock, extraData, null, additional)

        extraData.unregisterData(ConstantDatas.FLOW_TYPE_INFO, flow)

        mv.visitLabel(outsideEnd) // break;
    }

}