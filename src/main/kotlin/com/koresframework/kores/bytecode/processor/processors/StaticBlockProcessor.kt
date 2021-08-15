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

import com.koresframework.kores.Instructions
import com.koresframework.kores.MutableInstructions
import com.koresframework.kores.base.Access
import com.koresframework.kores.base.KoresModifier
import com.koresframework.kores.base.StaticBlock
import com.koresframework.kores.bytecode.common.MethodVisitorHelper
import com.koresframework.kores.bytecode.processor.CLASS_VISITOR
import com.koresframework.kores.bytecode.processor.IN_EXPRESSION
import com.koresframework.kores.bytecode.processor.METHOD_VISITOR
import com.koresframework.kores.bytecode.processor.TYPE_DECLARATION
import com.koresframework.kores.bytecode.util.asmConstValue
import com.koresframework.kores.common.KoresNothing
import com.koresframework.kores.factory.setFieldValue
import com.koresframework.kores.processor.Processor
import com.koresframework.kores.processor.ProcessorManager
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.inContext
import com.github.jonathanxd.iutils.kt.require
import org.objectweb.asm.Opcodes

object StaticBlockProcessor : Processor<StaticBlock> {

    override fun process(
        part: StaticBlock,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {
        IN_EXPRESSION.set(data, 0)
        val cw = CLASS_VISITOR.require(data)

        val mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)

        val mvHelper = MethodVisitorHelper(mv, mutableListOf())

        mv.visitCode()

        METHOD_VISITOR.inContext(data, mvHelper) {

            // Variable Initialize
            val typeDeclaration = TYPE_DECLARATION.require(data)

            val all = typeDeclaration.fields.filter {
                (it.modifiers.contains(KoresModifier.STATIC) || TYPE_DECLARATION.getOrNull(data)?.isInterface == true)
                        && it.value != KoresNothing
                        && it.value.asmConstValue == null
            }


            val body = MutableInstructions.create()

            for (fieldDeclaration in all) {

                val value = fieldDeclaration.value

                if (value != KoresNothing) {

                    val def =
                        setFieldValue(
                            typeDeclaration,
                            Access.STATIC,
                            fieldDeclaration.type,
                            fieldDeclaration.name,
                            value
                        )

                    body.add(def)
                }
            }


            if (body.isNotEmpty)
                processorManager.process(Instructions::class.java, body, data)

            processorManager.process(Instructions::class.java, part.body, data)
        }

        mv.visitInsn(Opcodes.RETURN)

        try {
            mv.visitMaxs(0, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        IN_EXPRESSION.remove(data)
        mv.visitEnd()

    }

}