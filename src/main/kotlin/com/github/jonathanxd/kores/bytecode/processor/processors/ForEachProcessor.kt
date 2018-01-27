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
import com.github.jonathanxd.kores.common.KoresNothing
import com.github.jonathanxd.kores.factory.*
import com.github.jonathanxd.kores.literal.Literals
import com.github.jonathanxd.kores.operator.Operators
import com.github.jonathanxd.kores.processor.Processor
import com.github.jonathanxd.kores.processor.ProcessorManager
import com.github.jonathanxd.kores.type
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require

object ForEachProcessor : Processor<ForEachStatement> {

    override fun process(
        part: ForEachStatement,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {

        val mvHelper = METHOD_VISITOR.require(data)

        val iterationType = part.iterationType

        val stm = when (iterationType) {
            IterationType.ARRAY -> {
                val arrayName = mvHelper.getUniqueVariableName("\$array#")
                val name = mvHelper.getUniqueVariableName("\$arrayIndex#")

                val arrayVariable = variable(
                    modifiers = setOf(KoresModifier.FINAL),
                    type = part.iterableElement.type, name = arrayName, value = part.iterableElement
                )
                val indexVariable = variable(type = Types.INT, name = name, value = Literals.INT(0))

                val accessArray = accessVariable(arrayVariable)
                val arrayType = arrayVariable.type

                ForStatement.Builder.builder()
                    .forInit(listOf(arrayVariable, indexVariable))
                    .forExpression(
                        check(
                            accessVariable(indexVariable),
                            Operators.LESS_THAN,
                            arrayLength(arrayType, accessArray)
                        )
                    )
                    .forUpdate(
                        listOf(
                            operateAndAssign(
                                indexVariable,
                                Operators.ADD,
                                Literals.INT(1)
                            )
                        )
                    )
                    .body(
                        Instructions.fromPart(
                            variable(
                                name = part.variable.name,
                                type = part.variable.type,
                                value = accessArrayValue(
                                    arrayType = arrayType,
                                    target = accessArray,
                                    index = accessVariable(indexVariable),
                                    valueType = part.variable.type

                                )
                            )
                        ) + part.body

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
                    )
                )

                ForStatement.Builder.builder()
                    .forInit(iteratorVariable)
                    .forExpression(
                        check(
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
                        )
                    )
                    .forUpdate(KoresNothing)
                    .body(
                        Instructions.fromPart(
                            variable(
                                name = part.variable.name,
                                type = part.variable.type,
                                value = cast(
                                    from = iterationType.nextMethodSpec.typeSpec.returnType,
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
                            )
                        ) + part.body
                    )
                    .build()

            }
        }

        mvHelper.enterNewFrame()
        processorManager.process(stm::class.java, stm, data)
        mvHelper.exitFrame()

    }

}
