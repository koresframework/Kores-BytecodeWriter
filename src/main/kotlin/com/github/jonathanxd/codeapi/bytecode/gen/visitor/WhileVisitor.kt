/*
 *      CodeAPI-BytecodeWriter - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2016 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
 *      Copyright (c) contributors
 *
 *
 *      Permission is hereby granted, free of charge, to any person obtaining a copy
 *      of this software and associated documentation files (the "Software"), to deal
 *      in the Software without restriction, including without limitation the rights
 *      to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *      copies of the Software, and to permit persons to whom the Software is
 *      furnished to do so, subject to the following conditions:
 *
 *      The above copyright notice and this permission notice shall be included in
 *      all copies or substantial portions of the Software.
 *
 *      THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *      IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *      FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *      AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *      LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *      OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *      THE SOFTWARE.
 */
package com.github.jonathanxd.codeapi.bytecode.gen.visitor

import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.bytecode.common.Flow
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.impl.IfBlockImpl
import com.github.jonathanxd.codeapi.interfaces.IfBlock
import com.github.jonathanxd.codeapi.interfaces.WhileBlock
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object WhileVisitor : VoidVisitor<WhileBlock, BytecodeClass, MVData> {

    override fun voidVisit(whileBlock: WhileBlock, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

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

        val instructionCodePart = InstructionCodePart.create { _, _, _, _ ->
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