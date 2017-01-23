package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.common.Flow
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.impl.IfBlockImpl
import com.github.jonathanxd.codeapi.interfaces.DoWhileBlock
import com.github.jonathanxd.codeapi.interfaces.IfBlock
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label

object DoWhileVisitor : VoidVisitor<DoWhileBlock, BytecodeClass, MVData> {

    override fun voidVisit(t: DoWhileBlock, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val whileStart = Label()
        val outOfIf = Label()
        val insideStart = Label()
        val insideEnd = Label()
        val outsideEnd = Label()

        val source = CodeSource.empty()

        val ifBlock = IfBlockImpl.instance(t, source)

        mv.visitLabel(whileStart)

        mv.visitLabel(insideStart)

        val flow = Flow(whileStart, insideStart, insideEnd, outsideEnd)

        extraData.registerData(ConstantDatas.FLOW_TYPE_INFO, flow)

        visitorGenerator.generateTo(IfBlock::class.java, ifBlock, extraData, null, additional)

        if(t.body.isPresent) {
            visitorGenerator.generateTo(CodeSource::class.java, t.body.get(), extraData, null, additional)
        }

        mv.visitLabel(insideEnd)

        BytecodeIfBlockVisitor.visit(ifBlock, whileStart, outOfIf, true, true, extraData, visitorGenerator, additional)

        extraData.unregisterData(ConstantDatas.FLOW_TYPE_INFO, flow)

        mv.visitLabel(outsideEnd)
    }

}