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
package com.github.jonathanxd.kores.bytecode.post

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class Processor(val api: Int, val processors: List<MethodProcessor>, val addTimes: Int) :
    PostProcessor {

    override fun process(classBytes: ByteArray): ByteArray {
        val cr = ClassReader(classBytes)
        val cn = ClassNode(api)
        cr.accept(cn, ClassReader.SKIP_FRAMES)

        val methods = cn.methods.map { m ->
            m as MethodNode

            if (addTimes > 0) {
                (1..addTimes).fold(m) { _, _ ->
                    processors.fold(m) { lm, p ->
                        p.process(
                            cn.name,
                            lm
                        )
                    }
                }
            } else {
                processors.fold(m) { lm, p -> p.process(cn.name, lm) }
            }
        }.toList()

        cn.methods.clear()

        cn.methods.addAll(methods)

        return ClassWriter(ClassWriter.COMPUTE_FRAMES).also { cn.accept(it) }.toByteArray()
    }
}