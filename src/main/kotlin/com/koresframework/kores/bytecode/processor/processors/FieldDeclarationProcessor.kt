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

import com.koresframework.kores.base.Annotable
import com.koresframework.kores.base.KoresModifier
import com.koresframework.kores.base.FieldDeclaration
import com.koresframework.kores.bytecode.processor.ANNOTATION_VISITOR_CAPABLE
import com.koresframework.kores.bytecode.processor.CLASS_VISITOR
import com.koresframework.kores.bytecode.processor.TYPE_DECLARATION
import com.koresframework.kores.bytecode.util.AnnotationVisitorCapable
import com.koresframework.kores.bytecode.util.ModifierUtil
import com.koresframework.kores.bytecode.util.asmConstValue
import com.koresframework.kores.processor.Processor
import com.koresframework.kores.processor.ProcessorManager
import com.koresframework.kores.type.GenericType
import com.koresframework.kores.util.descriptor
import com.koresframework.kores.util.typeDesc
import com.github.jonathanxd.iutils.data.TypedData
import com.koresframework.kores.util.descriptorDiscardBound

object FieldDeclarationProcessor : Processor<FieldDeclaration> {

    override fun process(
        part: FieldDeclaration,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {
        val visitor = CLASS_VISITOR.getOrNull(data)!!

        val modifiers =
            if (!part.modifiers.contains(KoresModifier.STATIC)
                    && TYPE_DECLARATION.getOrNull(data)?.isInterface == true
            )
                part.modifiers + KoresModifier.STATIC
            else part.modifiers

        val access = ModifierUtil.modifiersToAsm(modifiers)
        val signature = (part.type as? GenericType)?.descriptorDiscardBound
        val constValue =
            if (modifiers.contains(KoresModifier.STATIC)) part.value.asmConstValue
            else null

        visitor.visitField(access, part.name, part.type.typeDesc, signature, constValue).let {
            ANNOTATION_VISITOR_CAPABLE.set(data, AnnotationVisitorCapable.FieldVisitorCapable(it))
            processorManager.process(Annotable::class.java, part, data)
            ANNOTATION_VISITOR_CAPABLE.remove(data)
            it.visitEnd()
        }
    }

}