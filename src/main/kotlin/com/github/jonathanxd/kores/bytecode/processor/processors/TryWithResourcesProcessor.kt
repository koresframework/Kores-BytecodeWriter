/*
 *      Kores-BytecodeWriter - Translates CodeAPI Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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
package com.github.jonathanxd.kores.bytecode.processor.processors

import com.github.jonathanxd.kores.Instructions
import com.github.jonathanxd.kores.Types
import com.github.jonathanxd.kores.base.*
import com.github.jonathanxd.kores.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.kores.factory.*
import com.github.jonathanxd.kores.literal.Literals
import com.github.jonathanxd.kores.processor.Processor
import com.github.jonathanxd.kores.processor.ProcessorManager
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require

object TryWithResourcesProcessor : Processor<TryWithResources> {

    override fun process(
        part: TryWithResources,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {
        val mvHelper = METHOD_VISITOR.require(data)
        val vari = part.variable

        // Generate try-catch initialize field
        processorManager.process(VariableDeclaration::class.java, vari, data)

        val throwableFieldName = mvHelper.getUniqueVariableName("\$throwable_")

        // Generate exception field
        val throwableVariable = variable(
            name = throwableFieldName,
            type = Types.THROWABLE,
            value = Literals.NULL
        )

        processorManager.process(VariableDeclaration::class.java, throwableVariable, data)

        // Generate try block
        val catch_ = mvHelper.getUniqueVariableName("\$catch_")
        val catchStatement = catchStatement(
            listOf(Types.THROWABLE),
            variable(Types.THROWABLE, catch_),
            Instructions.fromVarArgs(
                setVariableValue(
                    Types.THROWABLE,
                    throwableFieldName,
                    accessVariable(Types.THROWABLE, catch_)
                ),
                throwException(accessVariable(Types.THROWABLE, catch_))
            )
        )

        val catch2_name = mvHelper.getUniqueVariableName("\$catch_2_")

        //AutoCloseable#close();
        val closeInvocation = invokeInterface(
            AutoCloseable::class.java,
            accessVariable(vari.variableType, vari.name),
            "close",
            TypeSpec(Types.VOID),
            emptyList()
        )

        //Throwable#addSuppressed(Throwable)
        val addSuppressedInvocation = invokeVirtual(
            Types.THROWABLE,
            accessVariable(throwableVariable.variableType, throwableVariable.name),
            "addSuppressed",
            TypeSpec(Types.VOID, listOf(Types.THROWABLE)),
            listOf(accessVariable(Types.THROWABLE, catch2_name))
        )

        val surroundedCloseInvocation = tryStatement(
            Instructions.fromPart(closeInvocation),
            listOf(
                catchStatement(
                    listOf(Types.THROWABLE),
                    variable(Types.THROWABLE, catch2_name),
                    Instructions.fromPart(
                        addSuppressedInvocation
                    )
                )
            )
        )

        val catchStatements = java.util.ArrayList<CatchStatement>()

        catchStatements.add(catchStatement)
        catchStatements.addAll(part.catchStatements)

        val tryCatchStatement = tryStatement(
            part.body,
            catchStatements,
            Instructions.fromPart(
                ifStatement(
                    checkNotNull(accessVariable(vari.type, vari.name)),
                    Instructions.fromPart(
                        ifStatement(
                            checkNotNull(
                                accessVariable(
                                    throwableVariable.type,
                                    throwableVariable.name
                                )
                            ),
                            Instructions.fromPart(
                                surroundedCloseInvocation
                            ),
                            Instructions.fromPart(
                                closeInvocation
                            )
                        )
                    )
                )
            ) + part.finallyStatement
        )

        processorManager.process(TryStatement::class.java, tryCatchStatement, data)
    }


}