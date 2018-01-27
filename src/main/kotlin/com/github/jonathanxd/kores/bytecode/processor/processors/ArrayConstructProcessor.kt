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

import com.github.jonathanxd.kores.base.ArrayConstructor
import com.github.jonathanxd.kores.base.ArrayStore
import com.github.jonathanxd.kores.bytecode.processor.IN_EXPRESSION
import com.github.jonathanxd.kores.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.kores.bytecode.processor.incrementInContext
import com.github.jonathanxd.kores.bytecode.util.ArrayUtil
import com.github.jonathanxd.kores.literal.Literals
import com.github.jonathanxd.kores.processor.Processor
import com.github.jonathanxd.kores.processor.ProcessorManager
import com.github.jonathanxd.kores.type.arrayComponent
import com.github.jonathanxd.kores.type.arrayDimension
import com.github.jonathanxd.kores.util.typeDesc
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require
import org.objectweb.asm.Opcodes

object ArrayConstructProcessor : Processor<ArrayConstructor> {

    override fun process(
        part: ArrayConstructor,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {
        val mv = METHOD_VISITOR.require(data).methodVisitor
        val arguments = part.arguments

        val initialize = arguments.isNotEmpty()
        val dimensions = part.dimensions
        val multi = dimensions.size > 1
        val component = part.arrayType.arrayComponent

        if (part.arrayType.arrayDimension != dimensions.size)
            throw IllegalArgumentException("Array dimension not equal to provided dimensions. Array Dimension: ${part.arrayType.arrayDimension}. Provided dimensions: ${dimensions.size}")

        if (multi && !initialize) {
            dimensions.forEach {
                IN_EXPRESSION.incrementInContext(data) {
                    processorManager.process(it::class.java, it, data)
                }
            }

            mv.visitMultiANewArrayInsn(component.typeDesc, dimensions.size)
        } else {
            val dimensionX = if (dimensions.isNotEmpty()) dimensions[0] else Literals.INT(0)

            processorManager.process(dimensionX::class.java, dimensionX, data)

            ArrayUtil.visitArrayStore(component, mv) // ANEWARRAY, ANEWARRAY T_INT, etc...
        }

        if (initialize) {
            // Initialize

            for (arrayStore in part.arrayValues) {
                mv.visitInsn(Opcodes.DUP)
                processorManager.process(ArrayStore::class.java, arrayStore, data)
            }
        }

        if (IN_EXPRESSION.require(data) == 0) {
            METHOD_VISITOR.require(data).methodVisitor.visitInsn(Opcodes.POP)
        }
    }

}