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
package com.github.jonathanxd.codeapi.bytecode.processor.processors

import com.github.jonathanxd.codeapi.base.Access
import com.github.jonathanxd.codeapi.base.FieldDefinition
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.util.require
import com.github.jonathanxd.codeapi.util.safeForComparison
import com.github.jonathanxd.codeapi.util.typeDesc
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Opcodes

object FieldDefinitionProcessor : Processor<FieldDefinition> {

    override fun process(part: FieldDefinition, data: TypedData, codeProcessor: CodeProcessor<*>) {
        val localization = Util.resolveType(part.localization, data)
        val target = part.target
        val safeTarget = target.safeForComparison

        val variableName = part.name
        val variableType = part.type
        val opcode = if (safeTarget is Access && safeTarget == Access.STATIC) Opcodes.PUTSTATIC else Opcodes.PUTFIELD

        codeProcessor.process(target::class.java, target, data)

        codeProcessor.process(part.value::class.java, part.value, data)

        METHOD_VISITOR.require(data).methodVisitor.visitFieldInsn(opcode, localization.internalName, variableName, variableType.typeDesc)
    }


}