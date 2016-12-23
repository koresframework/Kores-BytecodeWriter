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

import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.impl.AccessLocalImpl
import com.github.jonathanxd.codeapi.interfaces.AccessThis
import com.github.jonathanxd.codeapi.interfaces.FieldDeclaration
import com.github.jonathanxd.codeapi.interfaces.VariableDeclaration
import com.github.jonathanxd.codeapi.literals.Literals
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.util.HiddenField
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object VariableDeclarationVisitor : VoidVisitor<VariableDeclaration, BytecodeClass, MVData> {

    override fun voidVisit(variableDeclaration: VariableDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val typeDeclaration = Util.find(TypeVisitor.CODE_TYPE_REPRESENTATION, extraData, null)

        val localization = variableDeclaration.localization.orElse(null)


        val value = variableDeclaration.value.orElse(Literals.NULL)

        val at = variableDeclaration.target.orElse(null)

        if (at == null && localization == null) {
            mv.visitVarInsn(Opcodes.ALOAD, 0) // Legacy
        } else if (at != null) {
            visitorGenerator.generateTo(at.javaClass, at, extraData, null, additional)
        }

        visitorGenerator.generateTo(value.javaClass, value, extraData, null, additional)

        if (at == null) {
            if (localization != null) {
                mv.visitFieldInsn(Opcodes.PUTSTATIC, CodeTypeUtil.codeTypeToSimpleAsm(localization), variableDeclaration.name, CodeTypeUtil.codeTypeToFullAsm(variableDeclaration.variableType))
            } else {
                mv.visitFieldInsn(Opcodes.PUTFIELD, CodeTypeUtil.codeTypeToSimpleAsm(typeDeclaration), variableDeclaration.name, CodeTypeUtil.codeTypeToFullAsm(variableDeclaration.variableType))
            }
        } else {
            if (at is AccessLocalImpl) {

                val `var` = additional.getVar(variableDeclaration.name, variableDeclaration.variableType)


                if (!`var`.isPresent && variableDeclaration !is FieldDeclaration)
                    throw RuntimeException("Missing Variable Definition. Variable: '" + variableDeclaration.name + "' Type: '" + variableDeclaration.variableType.javaSpecName + "'.")
                else if (`var`.isPresent && variableDeclaration is FieldDeclaration && variableDeclaration !is HiddenField)
                    throw RuntimeException("Variable '" + variableDeclaration.getName() + "' Type: '" + variableDeclaration.getVariableType().javaSpecName + "'. Already defined!")


                val i_label = Label()

                mv.visitLabel(i_label)

                val i: Int

                if (variableDeclaration is HiddenField) {
                    i = additional.storeInternalVar(variableDeclaration.getName(), variableDeclaration.getVariableType(), i_label, null)
                            .orElseThrow({ additional.failStore(variableDeclaration) })
                } else {
                    i = additional.storeVar(variableDeclaration.name, variableDeclaration.variableType, i_label, null)
                            .orElseThrow({ additional.failStore(variableDeclaration) })
                }

                val type = Type.getType(variableDeclaration.variableType.javaSpecName)

                val opcode = type.getOpcode(Opcodes.ISTORE) // ALOAD

                mv.visitVarInsn(opcode, i)

            } else if (at is AccessThis) {
                // THIS
                mv.visitFieldInsn(Opcodes.PUTFIELD, CodeTypeUtil.codeTypeToSimpleAsm(typeDeclaration), variableDeclaration.name, CodeTypeUtil.codeTypeToFullAsm(variableDeclaration.variableType))
            } else {
                mv.visitFieldInsn(Opcodes.PUTFIELD, CodeTypeUtil.codeTypeToSimpleAsm(localization), variableDeclaration.name, CodeTypeUtil.codeTypeToFullAsm(variableDeclaration.variableType))
            }
        }
    }

}