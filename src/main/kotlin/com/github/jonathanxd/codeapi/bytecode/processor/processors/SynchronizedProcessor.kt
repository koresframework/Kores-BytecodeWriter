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

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.Synchronized
import com.github.jonathanxd.codeapi.base.TryStatement
import com.github.jonathanxd.codeapi.base.VariableDeclaration
import com.github.jonathanxd.codeapi.bytecode.extra.Dup
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.factory.accessVariable
import com.github.jonathanxd.codeapi.factory.variable
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.util.require
import com.github.jonathanxd.codeapi.util.typeOrNull
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Opcodes

object SynchronizedProcessor : Processor<Synchronized> {

    override fun process(part: Synchronized, data: TypedData, processorManager: ProcessorManager<*>) {

        val mvHelper = METHOD_VISITOR.require(data)
        val visitor = mvHelper.methodVisitor

        val name = mvHelper.getUniqueVariableName("\$sync_var#0")
        val type = part.instruction.typeOrNull ?: Types.OBJECT

        val variable = variable(type, name, Dup(part.instruction))

        processorManager.process(VariableDeclaration::class.java, variable, data)

        visitor.visitInsn(Opcodes.MONITORENTER)

        TryStatement.Builder.builder()
                .body(part.body)
                .finallyStatement(CodeSource.fromVarArgs(
                        accessVariable(variable),
                        InstructionCodePart.create { _, _, _ ->
                            visitor.visitInsn(Opcodes.MONITOREXIT)
                        }
                ))
                .build().let {
            processorManager.process(TryStatement::class.java, it, data)
        }
    }
}