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

import com.github.jonathanxd.codeapi.base.ArrayConstructor
import com.github.jonathanxd.codeapi.base.ArrayStore
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.util.ArrayUtil
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.util.codeTypeToTypeDesc
import org.objectweb.asm.Opcodes

object ArrayConstructVisitor : VoidVisitor<ArrayConstructor, BytecodeClass, MVData> {

    override fun voidVisit(t: ArrayConstructor, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor
        val arguments = t.arguments

        val initialize = arguments.isNotEmpty()
        val dimensions = t.dimensions
        val multi = dimensions.size > 1
        val component = t.arrayType.arrayComponent

        if (t.arrayType.arrayDimension != dimensions.size)
            throw IllegalArgumentException("Array dimension not equal to provided dimensions. Array Dimension: ${t.arrayType.arrayDimension}. Provided dimensions: ${dimensions.size}")

        if (multi && !initialize) {
            dimensions.forEach {
                visitorGenerator.generateTo(it::class.java, it, extraData, null, additional)
            }

            mv.visitMultiANewArrayInsn(codeTypeToTypeDesc(component), dimensions.size)
        } else {
            val dimensionX = if (dimensions.isNotEmpty()) dimensions[0] else Literals.INT(0)

            visitorGenerator.generateTo(dimensionX::class.java, dimensionX, extraData, null, additional)

            ArrayUtil.visitArrayStore(component, mv) // ANEWARRAY, ANEWARRAY T_INT, etc...
        }

        if (initialize) {
            // Initialize

            for (arrayStore in t.arrayValues) {
                mv.visitInsn(Opcodes.DUP)
                visitorGenerator.generateTo(ArrayStore::class.java, arrayStore, extraData, null, additional)
            }
        }

    }

}