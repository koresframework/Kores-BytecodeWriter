/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2021 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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

import com.github.jonathanxd.kores.Types
import com.github.jonathanxd.kores.bytecode.processor.IN_EXPRESSION
import com.github.jonathanxd.kores.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.kores.bytecode.processor.incrementInContext
import com.github.jonathanxd.kores.factory.constructorTypeSpec
import com.github.jonathanxd.kores.factory.invokeConstructor
import com.github.jonathanxd.kores.factory.invokeVirtual
import com.github.jonathanxd.kores.factory.typeSpec
import com.github.jonathanxd.kores.literal.Literals
import com.github.jonathanxd.kores.processor.Processor
import com.github.jonathanxd.kores.processor.ProcessorManager
import com.github.jonathanxd.kores.safeForComparison
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require
import com.github.jonathanxd.kores.Instruction
import com.github.jonathanxd.kores.base.*
import com.github.jonathanxd.kores.bytecode.INDY_CONCAT_STRATEGY
import com.github.jonathanxd.kores.bytecode.IndyConcatStrategy
import com.github.jonathanxd.kores.bytecode.processor.indifyEnabled
import com.github.jonathanxd.kores.common.DynamicMethodSpec
import com.github.jonathanxd.kores.common.MethodInvokeSpec
import com.github.jonathanxd.kores.common.MethodTypeSpec
import com.github.jonathanxd.kores.common.Stack
import com.github.jonathanxd.kores.literal.Literal
import com.github.jonathanxd.kores.type
import com.github.jonathanxd.kores.type.*
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.StringConcatFactory
import java.lang.reflect.Type


object ConcatProcessor : Processor<Concat> {

    override fun process(part: Concat, data: TypedData, processorManager: ProcessorManager<*>) {
        val concatenations = part.concatenations

        val first = if (concatenations.isEmpty()) null else concatenations[0]

        val allIsEmpty = concatenations.isNotEmpty() && concatenations.all {
            val safe =
                it.safeForComparison; safe is Literals.StringLiteral && safe.original.isEmpty()
        }

        if (first != null && !allIsEmpty) {

            if (concatenations.size == 1) {
                IN_EXPRESSION.incrementInContext(data) {
                    processorManager.process(first::class.java, first, data)
                }
            } else {
                val isIndy = data.indifyEnabled()
                if (isIndy) {
                    this.processIndy(part, data, processorManager)
                } else {
                    if (concatenations.size == 2) {

                        val stringConcat = invokeVirtual(
                            String::class.java, first, "concat",
                            typeSpec(String::class.java, String::class.java),
                            listOf(concatenations[1])
                        )

                        processorManager.process(MethodInvocation::class.java, stringConcat, data)
                    } else {

                        var strBuilder = Types.STRING_BUILDER.invokeConstructor(
                            constructorTypeSpec(String::class.java),
                            listOf(first)
                        )

                        (1 until concatenations.size)
                            .map { concatenations[it] }
                            .forEach {
                                strBuilder = invokeVirtual(
                                    Types.STRING_BUILDER,
                                    strBuilder,
                                    "append",
                                    typeSpec(Types.STRING_BUILDER, Types.STRING),
                                    listOf(it)
                                )
                            }

                        strBuilder = invokeVirtual(
                            Types.OBJECT,
                            strBuilder,
                            "toString",
                            typeSpec(Types.STRING),
                            emptyList()
                        )

                        processorManager.process(MethodInvocation::class.java, strBuilder, data)
                    }
                }

            }
        } else {
            // If the concatenations is empty or if all concat is empty only put a empty string on stack
            // It is better to CodeAPI (less things to process), and is better to JVM.
            METHOD_VISITOR.require(data).methodVisitor.visitLdcInsn("")
        }

    }

    data class ConstantsAndArguments(
        val constants: List<String>,
        val arguments: List<Instruction>,
        val bootstrapArg: List<String>
    ) {
        private val bootstrapArgWithConstFlags
            get() = this.bootstrapArg.joinToString("") { if (it != "\u0001") "\u0002" else it }

        val bootstrapArgsWithConst
            get() = listOf(bootstrapArgWithConstFlags) + this.bootstrapArg.filterNot { it == "\u0001" }

        val bootstrapArgWithInterpolation
            get() = listOf(this.bootstrapArg.joinToString(separator = ""))
    }

    private fun Type.typeString() = when {
        this is Class<*> -> this.toString()
        this is LoadedKoresType<*> -> this.loadedType.toString()
        this.koresTypeOrNull?.isPrimitive == true -> this.koresTypeOrNull?.type
        this.koresTypeOrNull?.isInterface == true -> this.koresTypeOrNull?.type?.let { "interface $it" }
        else -> this.koresTypeOrNull?.type?.let { "class $it" }
    }

    /**
     * Optimize concatenation
     */
    private fun constants(part: Concat): ConstantsAndArguments {
        val constants = part.concatenations.map {
            val safe = it.safeForComparison
            when(safe) {
                is Stack -> it
                is Literals.StringLiteral -> safe.original
                is Literals.ClassLiteral -> safe.type.typeString() ?: it
                is Literal -> (safe.value as? String) ?: safe.value.toString()
                else -> it
            }
        }

        return ConstantsAndArguments(
            constants.filterIsInstance<String>(),
            constants.filterIsInstance<Instruction>(),
            constants.map { (it as? String) ?: "\u0001" }
        )
    }

    /**
     * Optimize concatenation cases with Literals only
     */
    private fun optimizedConcatenation(part: Concat): Instruction? {
        val constants = constants(part)
        return optimizedConcatenation(constants)
    }

    /**
     * Optimize concatenation cases with Literals only
     */
    private fun optimizedConcatenation(constants: ConstantsAndArguments): Instruction? {
        val optimize = constants.arguments.isEmpty()

        return if (optimize) {
            if (constants.constants.isEmpty()) Literals.STRING("")
            else Literals.STRING(constants.constants.joinToString(separator = ""))
        } else {
            null
        }
    }

    private fun processIndy(part: Concat, data: TypedData, processorManager: ProcessorManager<*>) {
        val constants = constants(part)
        val optimize = optimizedConcatenation(constants)

        if (optimize != null) {
            processorManager.process(optimize, data)
        } else {
            val indyConcatStrategy = processorManager.options[INDY_CONCAT_STRATEGY] ?: IndyConcatStrategy.INTERPOLATE

            val arguments = when (indyConcatStrategy) {
                IndyConcatStrategy.INTERPOLATE, IndyConcatStrategy.CONSTANT -> constants.arguments
                IndyConcatStrategy.LDC -> part.concatenations
            }

            val types = arguments.map { it.type }

            val bootstrapArgs = when (indyConcatStrategy) {
                IndyConcatStrategy.INTERPOLATE -> constants.bootstrapArgWithInterpolation
                IndyConcatStrategy.CONSTANT -> constants.bootstrapArgsWithConst
                IndyConcatStrategy.LDC -> listOf(arguments.map { "\u0001" })
            }

            val indy = InvokeDynamic.Builder.builder()
                .bootstrap(
                    MethodInvokeSpec(
                        InvokeType.INVOKE_STATIC,
                        MethodTypeSpec(
                            typeOf<StringConcatFactory>(),
                            "makeConcatWithConstants",
                            TypeSpec(
                                returnType = typeOf<CallSite>(),
                                parameterTypes = listOf(
                                    typeOf<MethodHandles.Lookup>(),
                                    typeOf<String>(),
                                    typeOf<MethodType>(),
                                    typeOf<String>(),
                                    typeOf<Array<Any>>()
                                )
                            )
                        )
                    )
                )
                .bootstrapArgs(bootstrapArgs)
                .dynamicMethod(
                    DynamicMethodSpec(
                        "makeConcatWithConstants",
                        TypeSpec(typeOf<String>(), types),
                        arguments
                    )
                )
                .build()

            processorManager.process(indy, data)
        }
    }


}