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

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.base.Access
import com.github.jonathanxd.codeapi.base.CodeModifier
import com.github.jonathanxd.codeapi.base.StaticBlock
import com.github.jonathanxd.codeapi.bytecode.common.MethodVisitorHelper
import com.github.jonathanxd.codeapi.bytecode.processor.CLASS_VISITOR
import com.github.jonathanxd.codeapi.bytecode.processor.IN_EXPRESSION
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.processor.TYPE_DECLARATION
import com.github.jonathanxd.codeapi.bytecode.util.asmConstValue
import com.github.jonathanxd.codeapi.common.CodeNothing
import com.github.jonathanxd.codeapi.factory.setFieldValue
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
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
                (it.modifiers.contains(CodeModifier.STATIC) || TYPE_DECLARATION.getOrNull(data)?.isInterface == true)
                        && it.value != CodeNothing
                        && it.value.asmConstValue == null
            }


            val body = MutableCodeSource.create()

            for (fieldDeclaration in all) {

                val value = fieldDeclaration.value

                if (value != CodeNothing) {

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
                processorManager.process(CodeSource::class.java, body, data)

            processorManager.process(CodeSource::class.java, part.body, data)
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