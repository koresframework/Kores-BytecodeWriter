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
package com.koresframework.kores.bytecode.processor.processors

import com.koresframework.kores.base.Access
import com.koresframework.kores.base.FieldDefinition
import com.koresframework.kores.bytecode.processor.IN_EXPRESSION
import com.koresframework.kores.bytecode.processor.METHOD_VISITOR
import com.koresframework.kores.bytecode.processor.incrementInContext
import com.koresframework.kores.processor.Processor
import com.koresframework.kores.processor.ProcessorManager
import com.koresframework.kores.safeForComparison
import com.koresframework.kores.util.typeDesc
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require
import org.objectweb.asm.Opcodes

object FieldDefinitionProcessor : Processor<FieldDefinition> {

    override fun process(
        part: FieldDefinition,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {
        val localization = Util.resolveType(part.localization, data)
        val target = part.target
        val safeTarget = target.safeForComparison

        val variableName = part.name
        val variableType = part.type
        val opcode =
            if (safeTarget is Access && safeTarget == Access.STATIC) Opcodes.PUTSTATIC else Opcodes.PUTFIELD

        IN_EXPRESSION.incrementInContext(data) {
            processorManager.process(target::class.java, target, data)
        }

        IN_EXPRESSION.incrementInContext(data) {
            processorManager.process(part.value::class.java, part.value, data)
        }

        METHOD_VISITOR.require(data)
            .methodVisitor.visitFieldInsn(
            opcode,
            localization.internalName,
            variableName,
            variableType.typeDesc
        )
    }


}