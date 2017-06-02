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
package com.github.jonathanxd.codeapi.bytecode.processor.processors

import com.github.jonathanxd.codeapi.base.Operate
import com.github.jonathanxd.codeapi.base.VariableDefinition
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.util.javaSpecName
import com.github.jonathanxd.codeapi.util.require
import com.github.jonathanxd.codeapi.util.safeForComparison
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object VariableDefinitionProcessor : Processor<VariableDefinition> {

    override fun process(part: VariableDefinition, data: TypedData, codeProcessor: CodeProcessor<*>) {

        val mvHelper = METHOD_VISITOR.require(data)

        val variableName = part.name
        val variableType = part.type

        val value = part.value
        val safeValue = value.safeForComparison

        val variable = mvHelper.getVar(variableName, variableType)

        if (!variable.isPresent)
            throw IllegalArgumentException("Could not find variable (name: $variableName, type: $variableType)")

        val varPos = mvHelper.getVarPos(variable.get()).asInt

        // Try to optimize the VariableDefinition of a operation
        if (safeValue is Operate && VariableOperateProcessor.visit(part, safeValue, value, varPos, mvHelper))
            return

        codeProcessor.process(value::class.java, value, data)

        val type = Type.getType(variableType.javaSpecName)

        val opcode = type.getOpcode(Opcodes.ISTORE) // ASTORE


        mvHelper.methodVisitor.visitVarInsn(opcode, varPos)
    }

}