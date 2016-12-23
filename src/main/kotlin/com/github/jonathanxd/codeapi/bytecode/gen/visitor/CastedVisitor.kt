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

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.common.TypeSpec
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.helper.Helper
import com.github.jonathanxd.codeapi.interfaces.Casted
import com.github.jonathanxd.codeapi.types.CodeType
import com.github.jonathanxd.codeapi.util.Stack
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.optional.Require
import org.objectweb.asm.Opcodes

object CastedVisitor : VoidVisitor<Casted, BytecodeClass, MVData> {

    override fun voidVisit(t: Casted, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val from = t.originalType
        val to = Require.require(t.type, "Required cast target type")

        val castedPart = t.castedPart.orElse(Stack.INSTANCE)

        val autoboxing = if (from.isPresent) autoboxing(from.get(), to, castedPart) else null

        if (autoboxing != null && from.isPresent) {
            visitorGenerator.generateTo(autoboxing.javaClass, autoboxing, extraData, null, additional)

            if (from.get().isPrimitive && !to.isPrimitive && from.get().wrapperType.canonicalName != to.canonicalName) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, CodeTypeUtil.codeTypeToSimpleAsm(to))
            }

        } else {
            visitorGenerator.generateTo(castedPart.javaClass, castedPart, extraData, null, additional)

            if (from.isPresent && from.get() != to) {
                if (to.isPrimitive) {
                    CodeTypeUtil.convertToPrimitive(from.get(), to, mv)
                    return
                }

                mv.visitTypeInsn(Opcodes.CHECKCAST, CodeTypeUtil.codeTypeToSimpleAsm(to))
            } else if (!from.isPresent) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, CodeTypeUtil.codeTypeToSimpleAsm(to))
            }
        }
    }

    // Autobox FROM -> TO or AutoUnbox FROM -> TO
    private fun autoboxing(from: CodeType, to: CodeType, casted: CodePart?): CodePart? {
        var casted = casted

        var translate: CodePart? = null

        if (casted == null)
            casted = Stack.INSTANCE

        if (from.isPrimitive && !to.isPrimitive) {

            translate = CodeAPI.invokeConstructor(from.wrapperType, CodeAPI.constructorTypeSpec(from), CodeAPI.argument(casted))

        } else if (!from.isPrimitive && to.isPrimitive) {

            val wrapper = to.wrapperType

            var castTo: CodeType? = null

            if (to.wrapperType.canonicalName != from.canonicalName) {
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

            checkNotNull(methodName)

            if (castTo == null) {
                translate = CodeAPI.invokeVirtual(wrapper, casted, methodName, TypeSpec(to))
            } else {
                translate = CodeAPI.invokeVirtual(wrapper, Helper.cast(from, castTo, casted), methodName, TypeSpec(to))
            }
        }

        return translate

    }

}