/*
 *      CodeAPI-BytecodeWriter - Translates CodeAPI Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
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
package com.github.jonathanxd.codeapi.bytecode.processor.processors

import com.github.jonathanxd.codeapi.CodeInstruction
import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.Cast
import com.github.jonathanxd.codeapi.base.TypeSpec
import com.github.jonathanxd.codeapi.bytecode.processor.IN_EXPRESSION
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.processor.incrementInContext
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.factory.cast
import com.github.jonathanxd.codeapi.factory.constructorTypeSpec
import com.github.jonathanxd.codeapi.factory.invokeConstructor
import com.github.jonathanxd.codeapi.factory.invokeVirtual
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.type.*
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require
import org.objectweb.asm.Opcodes
import java.lang.reflect.Type

object CastProcessor : Processor<Cast> {

    override fun process(part: Cast, data: TypedData, processorManager: ProcessorManager<*>) {
        val mv = METHOD_VISITOR.require(data).methodVisitor

        val from = part.originalType
        val to = part.targetType

        val castedPart = part.instruction

        // No cast of void types.
        if (from != null && !from.`is`(to) && from.`is`(Types.VOID) || to.`is`(Types.VOID)) {
            IN_EXPRESSION.incrementInContext(data) {
                processorManager.process(castedPart::class.java, castedPart, data)
            }
            return
        }

        val autoboxing = if (from != null) autoboxing(from, to, castedPart) else null

        if (autoboxing != null && from != null) {
            processorManager.process(autoboxing::class.java, autoboxing, data)

            if (from.isPrimitive && !to.isPrimitive && from.wrapperType!!.canonicalName != to.canonicalName) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, to.internalName)
            }

        } else {
            IN_EXPRESSION.incrementInContext(data) {
                processorManager.process(castedPart::class.java, castedPart, data)
            }

            if (from != null && !from.`is`(to)) {
                if (to.isPrimitive) {
                    CodeTypeUtil.convertToPrimitive(from, to, mv)
                    return
                }

                mv.visitTypeInsn(Opcodes.CHECKCAST, to.internalName)
            } else if (from == null) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, to.internalName)
            }
        }
    }

    // Autobox FROM -> TO or AutoUnbox FROM -> TO
    private fun autoboxing(from: Type, to: Type, casted: CodeInstruction): CodePart? {
        var translate: CodePart? = null

        if (from.isArray || to.isArray)
            return null

        // No autoboxing for void types
        if (!from.`is`(to) && from.`is`(Types.VOID) || to.`is`(Types.VOID))
            return null

        if (from.isPrimitive && !to.isPrimitive) {

            translate =
                    from.wrapperType!!.invokeConstructor(constructorTypeSpec(from), listOf(casted))

        } else if (!from.isPrimitive && to.isPrimitive) {

            val wrapper = to.wrapperType!!

            var castTo: CodeType? = null

            if (wrapper.canonicalName != from.canonicalName) {
                castTo = to.wrapperType
            }

            val methodName = if (wrapper.canonicalName == "java.lang.Byte") {
                "byteValue"
            } else if (wrapper.canonicalName == "java.lang.Short") {
                "shortValue"
            } else if (wrapper.canonicalName == "java.lang.Integer") {
                "intValue"
            } else if (wrapper.canonicalName == "java.lang.Long") {
                "longValue"
            } else if (wrapper.canonicalName == "java.lang.Integer") {
                "floatValue"
            } else if (wrapper.canonicalName == "java.lang.Double") {
                "doubleValue"
            } else if (wrapper.canonicalName == "java.lang.Boolean") {
                "booleanValue"
            } else if (wrapper.canonicalName == "java.lang.Character") {
                "charValue"
            } else null

            methodName!!

            if (castTo == null) {
                translate = invokeVirtual(wrapper, casted, methodName, TypeSpec(to), emptyList())
            } else {
                translate = invokeVirtual(
                    wrapper,
                    cast(from, castTo, casted),
                    methodName,
                    TypeSpec(to),
                    emptyList()
                )
            }
        }

        return translate

    }

}