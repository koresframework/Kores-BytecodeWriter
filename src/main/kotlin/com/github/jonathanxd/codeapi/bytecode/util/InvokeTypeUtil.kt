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
package com.github.jonathanxd.codeapi.bytecode.util

import com.github.jonathanxd.codeapi.base.InvokeType
import com.github.jonathanxd.codeapi.base.InvokeType.*
import org.objectweb.asm.Opcodes.*

object InvokeTypeUtil {

    /**
     * Convert [InvokeType] to asm invocation opcode.
     *
     * @param invokeType Type to convert
     * @return asm opcode corresponding to `invokeType`.
     */
    fun toAsm(invokeType: InvokeType): Int {
        when (invokeType) {
            INVOKE_INTERFACE -> return INVOKEINTERFACE
            INVOKE_SPECIAL -> return INVOKESPECIAL
            INVOKE_VIRTUAL -> return INVOKEVIRTUAL
            INVOKE_STATIC -> return INVOKESTATIC
            else -> throw RuntimeException("Cannot determine opcode of '$invokeType'")
        }
    }

    /**
     * Convert [InvokeType] to asm dynamic invocation opcode.
     *
     * @param invokeType Type to convert
     * @return asm opcode corresponding to `invokeType` (dynamic).
     */
    fun toAsm_H(invokeType: InvokeType): Int {
        when (invokeType) {
            INVOKE_INTERFACE -> return H_INVOKEINTERFACE
            INVOKE_SPECIAL -> return H_INVOKESPECIAL
            INVOKE_VIRTUAL -> return H_INVOKEVIRTUAL
            INVOKE_STATIC -> return H_INVOKESTATIC
            else -> throw RuntimeException("Cannot determine opcode of '$invokeType'")
        }
    }

    /**
     * Convert asm invocation opcode to [InvokeType].
     *
     * @param opcode Opcode to convert
     * @return asm flag corresponding to `invokeType`.
     */
    fun fromAsm(opcode: Int): InvokeType {
        when (opcode) {
            INVOKEINTERFACE -> return INVOKE_INTERFACE
            INVOKESPECIAL -> return INVOKE_SPECIAL
            INVOKEVIRTUAL -> return INVOKE_VIRTUAL
            INVOKESTATIC -> return INVOKE_STATIC
            else -> throw RuntimeException("Cannot determine InvokeType of opcode '$opcode'")
        }
    }

    /**
     * Convert asm [dynamic] invocation opcode to [InvokeType].
     *
     * @param opcode Opcode to convert
     * @return asm flag corresponding to `invokeType` (dynamic).
     */
    fun fromAsm_H(opcode: Int): InvokeType {
        when (opcode) {
            H_INVOKEINTERFACE -> return INVOKE_INTERFACE
            H_INVOKESPECIAL -> return INVOKE_SPECIAL
            H_INVOKEVIRTUAL -> return INVOKE_VIRTUAL
            H_INVOKESTATIC -> return INVOKE_STATIC
            else -> throw RuntimeException("Cannot determine InvokeType of opcode '$opcode'")
        }
    }

}