/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
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

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.MethodNode

object GotoOptimizer : MethodProcessor {

    override fun process(owner: String, methodNode: MethodNode): MethodNode {
        val insns = methodNode.instructions

        for (insn in insns) {
            if (insn is JumpInsnNode) {
                var label = insn.label
                var target: AbstractInsnNode?
                while (true) {
                    target = label

                    while (target != null && target.opcode < 0) {
                        target = target.next
                    }

                    if (target != null && target.opcode == Opcodes.GOTO && target is JumpInsnNode) {
                        label = target.label
                    } else {
                        break
                    }
                }

                insn.label = label

                if (insn.opcode == Opcodes.GOTO && target != null) {
                    when (target.opcode) {
                        in Opcodes.IRETURN..Opcodes.RETURN, Opcodes.ATHROW -> {
                            insns.set(insn, target.clone(null))
                        }
                    }
                }
            }
        }

        return methodNode
    }

}
