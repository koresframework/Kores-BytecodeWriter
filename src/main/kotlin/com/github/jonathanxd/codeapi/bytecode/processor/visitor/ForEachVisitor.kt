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
package com.github.jonathanxd.codeapi.bytecode.processor.visitor

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.processor.require
import com.github.jonathanxd.codeapi.bytecode.util.CodePartUtil
import com.github.jonathanxd.codeapi.factory.*
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.operator.Operators
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.iutils.data.TypedData

object ForEachVisitor : Processor<ForEachStatement> {

    override fun process(part: ForEachStatement, data: TypedData, codeProcessor: CodeProcessor<*>) {

        val mvHelper = METHOD_VISITOR.require(data)

        val iterationType = part.iterationType

        val stm = when (iterationType) {
            IterationType.ARRAY -> {
                val name = mvHelper.getUniqueVariableName("\$arrayIndex#")

                val indexVariable = variable(type = Types.INT, name = name, value = Literals.INT(0))

                val arrayType = CodePartUtil.getType(part.iterableElement)

                ForStatement.Builder.builder()
                        .withForInit(indexVariable)
                        .withForExpression(check(
                                accessVariable(indexVariable),
                                Operators.LESS_THAN,
                                arrayLength(arrayType, part.iterableElement)
                        ))
                        .withForUpdate(operateAndAssign(indexVariable, Operators.ADD, Literals.INT(1)))
                        .withBody(
                                CodeSource.fromPart(variable(
                                        name = part.variable.name,
                                        type = part.variable.type,
                                        value = accessArrayValue(
                                                arrayType = arrayType,
                                                target = part.iterableElement,
                                                index = accessVariable(indexVariable),
                                                valueType = part.variable.type

                                        )
                                )) + part.body

                        )
                        .build()

            }
            else -> {
                val iterableType = iterationType.iteratorMethodSpec.localization
                val iteratorType = iterationType.iteratorMethodSpec.typeSpec.returnType
                val iteratorGetterName = iterationType.iteratorMethodSpec.methodName

                val name = mvHelper.getUniqueVariableName("\$iteratorInstance#")

                val iteratorVariable = variable(
                        type = iteratorType,
                        name = name,
                        value = invoke(
                                invokeType = InvokeType.get(iterableType),
                                localization = iterableType,
                                name = iteratorGetterName,
                                target = part.iterableElement,
                                spec = iterationType.iteratorMethodSpec.typeSpec,
                                arguments = emptyList()
                        ))

                ForStatement.Builder.builder()
                        .withForInit(iteratorVariable)
                        .withForExpression(check(
                                invoke(
                                        invokeType = InvokeType.get(iteratorType),
                                        localization = iteratorType,
                                        name = iterationType.hasNextName,
                                        target = accessVariable(iteratorVariable),
                                        spec = TypeSpec(Types.BOOLEAN),
                                        arguments = emptyList()
                                ),
                                Operators.EQUAL_TO,
                                Literals.TRUE
                        ))
                        .withForUpdate(null)
                        .withBody(
                                CodeSource.fromPart(variable(
                                        name = part.variable.name,
                                        type = part.variable.type,
                                        value = cast(from = iterationType.nextMethodSpec.typeSpec.returnType,
                                                to = part.variable.type,
                                                part = invoke(
                                                        invokeType = InvokeType.get(iteratorType),
                                                        localization = iteratorType,
                                                        name = iterationType.nextMethodSpec.methodName,
                                                        target = accessVariable(iteratorVariable),
                                                        spec = iterationType.nextMethodSpec.typeSpec,
                                                        arguments = emptyList()
                                                )
                                        )
                                )) + part.body
                        )
                        .build()

            }
        }

        mvHelper.enterNewFrame()
        codeProcessor.process(stm::class.java, stm, data)
        mvHelper.exitFrame()

    }

}
