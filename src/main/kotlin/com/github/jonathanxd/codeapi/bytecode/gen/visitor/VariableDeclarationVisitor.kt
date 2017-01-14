/*
 *      CodeAPI-BytecodeWriter - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2017 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
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

import com.github.jonathanxd.codeapi.base.FieldDeclaration
import com.github.jonathanxd.codeapi.base.VariableDeclaration
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.util.VariableVariantUtil
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.util.HiddenVariable
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object VariableDeclarationVisitor : VoidVisitor<VariableDeclaration, BytecodeClass, MVData> {

    override fun voidVisit(t: VariableDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val value = t.value

        if (value != null) {
            visitorGenerator.generateTo(value.javaClass, value, extraData, null, additional)
        }

        val `var` = additional.getVar(t.name, t.variableType)

        if (`var`.isPresent && t is FieldDeclaration && !VariableVariantUtil.isVariant(t))
            throw RuntimeException("Variable '" + t.name + "' Type: '" + t.variableType.javaSpecName + "'. Already defined!")


        val i_label = Label()

        mv.visitLabel(i_label)

        val i: Int

        if (t is HiddenVariable) {
            i = additional.storeInternalVar(t.name, t.variableType, i_label, null)
                    .orElseThrow({ additional.failStore(t) })
        } else {
            i = additional.storeVar(t.name, t.variableType, i_label, null)
                    .orElseThrow({ additional.failStore(t) })
        }

        val type = Type.getType(t.variableType.javaSpecName)

        val opcode = type.getOpcode(Opcodes.ISTORE) // ALOAD

        mv.visitVarInsn(opcode, i)

    }

}