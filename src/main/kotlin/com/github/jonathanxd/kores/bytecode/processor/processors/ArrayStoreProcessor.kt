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

import com.github.jonathanxd.kores.base.ArrayAccess
import com.github.jonathanxd.kores.base.ArrayStore
import com.github.jonathanxd.kores.bytecode.processor.IN_EXPRESSION
import com.github.jonathanxd.kores.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.kores.bytecode.processor.incrementInContext
import com.github.jonathanxd.kores.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.kores.factory.cast
import com.github.jonathanxd.kores.processor.Processor
import com.github.jonathanxd.kores.processor.ProcessorManager
import com.github.jonathanxd.kores.type.`is`
import com.github.jonathanxd.kores.type.arrayComponent
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require
import org.objectweb.asm.Opcodes

object ArrayStoreProcessor : Processor<ArrayStore> {

    override fun process(part: ArrayStore, data: TypedData, processorManager: ProcessorManager<*>) {
        IN_EXPRESSION.incrementInContext(data) {
            processorManager.process(ArrayAccess::class.java, part, data)
        }

        val index = part.index

        IN_EXPRESSION.incrementInContext(data) {
            processorManager.process(index::class.java, index, data)
        }

        var value = part.valueToStore
        val valueType = part.valueType

        val arrayComponentType = part.arrayType.arrayComponent

        if (!arrayComponentType.`is`(valueType)) {
            // Auto casting.
            value = cast(valueType, arrayComponentType, value)
        }

        IN_EXPRESSION.incrementInContext(data) {
            processorManager.process(value::class.java, value, data)
        }

        val opcode = CodeTypeUtil.getOpcodeForType(arrayComponentType, Opcodes.IASTORE)

        METHOD_VISITOR.require(data).methodVisitor.visitInsn(opcode)
    }

}