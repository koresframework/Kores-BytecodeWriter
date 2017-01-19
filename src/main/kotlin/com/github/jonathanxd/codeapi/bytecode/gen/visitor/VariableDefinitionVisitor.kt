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

import com.github.jonathanxd.codeapi.base.Operate
import com.github.jonathanxd.codeapi.base.VariableDefinition
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object VariableDefinitionVisitor : VoidVisitor<VariableDefinition, BytecodeClass, MVData> {

    override fun voidVisit(t: VariableDefinition, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val variableName = t.name
        val variableType = t.type

        val value = t.value

        val variable = additional.getVar(variableName, variableType)

        if (!variable.isPresent)
            throw IllegalArgumentException("Could not find variable (name: $variableName, type: $variableType)")

        val varPos = additional.getVarPos(variable.get()).asInt

        // Try to optimize the VariableDefinition of a operation
        if (value is Operate && VariableOperateVisitor.visit(t, value, varPos, additional))
            return

        visitorGenerator.generateTo(value.javaClass, value, extraData, additional)

        val type = Type.getType(variableType.javaSpecName)

        val opcode = type.getOpcode(Opcodes.ISTORE) // ASTORE


        additional.methodVisitor.visitVarInsn(opcode, varPos)

    }

}