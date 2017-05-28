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

import com.github.jonathanxd.codeapi.base.ArrayAccess
import com.github.jonathanxd.codeapi.base.ArrayStore
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.processor.require
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.factory.cast
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.util.`is`
import com.github.jonathanxd.codeapi.util.arrayComponent
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Opcodes

object ArrayStoreVisitor : Processor<ArrayStore> {

    override fun process(part: ArrayStore, data: TypedData, codeProcessor: CodeProcessor<*>) {
        codeProcessor.process(ArrayAccess::class.java, part, data)

        val index = part.index

        codeProcessor.process(index::class.java, index, data)

        var value = part.valueToStore
        val valueType = part.valueType

        val arrayComponentType = part.arrayType.arrayComponent

        if (!arrayComponentType.`is`(valueType)) {
            // Auto casting.
            value = cast(valueType, arrayComponentType, value)
        }

        codeProcessor.process(value::class.java, value, data)

        val opcode = CodeTypeUtil.getOpcodeForType(arrayComponentType, Opcodes.IASTORE)

        METHOD_VISITOR.require(data).methodVisitor.visitInsn(opcode)
    }

}