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
package com.github.jonathanxd.kores.bytecode.util

import com.github.jonathanxd.kores.operator.Operator
import com.github.jonathanxd.kores.operator.Operators.EQUAL_TO
import com.github.jonathanxd.kores.operator.Operators.GREATER_THAN
import com.github.jonathanxd.kores.operator.Operators.GREATER_THAN_OR_EQUAL_TO
import com.github.jonathanxd.kores.operator.Operators.LESS_THAN
import com.github.jonathanxd.kores.operator.Operators.LESS_THAN_OR_EQUAL_TO
import com.github.jonathanxd.kores.operator.Operators.NOT_EQUAL_TO
import org.objectweb.asm.Opcodes

/**
 * Hardcoded conversion of Operators to ASM opcode, documented in my native language.
 */
object OperatorUtil {

    fun convertToSimpleIf(opcode: Int): Int {
        if (opcode == Opcodes.IF_ICMPEQ) {
            return Opcodes.IFEQ
        } else if (opcode == Opcodes.IF_ICMPLT) {
            return Opcodes.IFLT
        } else if (opcode == Opcodes.IF_ICMPLE) {
            return Opcodes.IFLE
        } else if (opcode == Opcodes.IF_ICMPGT) {
            return Opcodes.IFGT
        } else if (opcode == Opcodes.IF_ICMPGE) {
            return Opcodes.IFGE
        } else if (opcode == Opcodes.IF_ICMPNE) {
            return Opcodes.IFNE
        }

        throw IllegalArgumentException("Cannot convert opcode '$opcode' to simple if.")
    }

    fun primitiveToAsm(operator: Operator, isInverse: Boolean): Int {
        if (!isInverse) {
            return primitiveToAsm(operator)
        } else {
            return inversePrimitiveToAsm(operator)
        }
    }

    fun primitiveToAsm(operator: Operator): Int {
        if (operator === EQUAL_TO) {
            // ==
            return Opcodes.IF_ICMPEQ
        } else if (operator === LESS_THAN) {
            // <
            return Opcodes.IF_ICMPLT
        } else if (operator === LESS_THAN_OR_EQUAL_TO) {
            // <=
            return Opcodes.IF_ICMPLE
        } else if (operator === GREATER_THAN) {
            // >
            return Opcodes.IF_ICMPGT
        } else if (operator === GREATER_THAN_OR_EQUAL_TO) {
            // >=
            return Opcodes.IF_ICMPGE
        } else if (operator === NOT_EQUAL_TO) {
            // !=
            return Opcodes.IF_ICMPNE
        }

        throw RuntimeException("Cannot determine primitive opcode of '$operator'")
    }

    /**
     * Converts primitive [operator] to a inverse version in ASM Opcode
     *
     * Reads IF_OPCODES.md
     *
     * The opcodes should be converted to a inverse version because the opcodes branches to a label
     * if the operation succeeds.
     */
    fun inversePrimitiveToAsm(operator: Operator): Int {
        if (operator === EQUAL_TO) {
            // If 'EQUAL_TO', convert to NOT_EQUAL_TO
            // == -> !=
            return Opcodes.IF_ICMPNE

        } else if (operator === LESS_THAN) {
            // If 'LESS_THAN', convert to 'GREATER_THAN_OR_EQUAL_TO'
            // < -> >=
            return Opcodes.IF_ICMPGE

        } else if (operator === LESS_THAN_OR_EQUAL_TO) {
            // If 'LESS_THAN_OR_EQUAL_TO' convert to 'GREATER_THAN'
            // <= -> >
            return Opcodes.IF_ICMPGT

        } else if (operator === GREATER_THAN) {
            // If 'GREATER_THAN' convert to 'LESS_THAN_OR_EQUAL_TO'
            // > -> <=
            return Opcodes.IF_ICMPLE

        } else if (operator === GREATER_THAN_OR_EQUAL_TO) {
            // If 'GREATER_THAN_OR_EQUAL_TO' convert to 'LESS_THAN'
            // >= -> <
            return Opcodes.IF_ICMPLT

        } else if (operator === NOT_EQUAL_TO) {
            // If 'NOT_EQUAL_TO' convert to 'EQUAL_TO'
            // != -> ==
            return Opcodes.IF_ICMPEQ
        }

        throw RuntimeException("Cannot determine primitive opcode of '$operator'")
    }

    fun referenceToAsm(operator: Operator, isInverse: Boolean): Int {
        if (!isInverse) {
            return referenceToAsm(operator)
        } else {
            return inverseReferenceToAsm(operator)
        }
    }

    fun referenceToAsm(operator: Operator): Int {
        if (operator === EQUAL_TO) {
            return Opcodes.IF_ACMPEQ
        } else if (operator === NOT_EQUAL_TO) {
            return Opcodes.IF_ACMPNE
        }

        throw RuntimeException("Cannot determine reference opcode of '$operator'")
    }

    /**
     * Converts reference [operator] to a inverse version in ASM Opcode
     *
     * Reads IF_OPCODES.md
     *
     * The opcodes should be converted to a inverse version because the opcodes branches to a label
     * if the operation succeeds.
     */
    fun inverseReferenceToAsm(operator: Operator): Int {
        if (operator === EQUAL_TO) {
            // If 'EQUAL_TO' convert to 'NOT_EQUAL_TO'
            // == -> !=
            return Opcodes.IF_ACMPNE
        } else if (operator === NOT_EQUAL_TO) {
            // If 'NOT_EQUAL_TO' convert to 'EQUAL_TO'
            // != -> ==
            return Opcodes.IF_ACMPEQ
        }

        throw RuntimeException("Cannot determine reference opcode of '$operator'")
    }

    /**
     * Converts null check [operator] to a inverse version in ASM Opcode
     *
     * Reads IF_OPCODES.md
     *
     * The opcodes should be converted to a inverse version because the opcodes branches to a label
     * if the operation succeeds.
     */
    fun inverseNullCheckToAsm(operator: Operator): Int {
        if (operator === NOT_EQUAL_TO) {
            // If 'NOT_EQUAL_TO' convert to 'EQUAL_TO'
            // NULL -> NOTNULL
            return Opcodes.IFNULL
        } else if (operator === EQUAL_TO) {
            // If 'EQUAL_TO' convert to 'NOT_EQUAL_TO'
            // NONNULL -> NULL
            return Opcodes.IFNONNULL
        }

        throw RuntimeException("Cannot determine reference opcode of '$operator'")
    }

    fun nullCheckToAsm(operator: Operator, isInverse: Boolean): Int {
        if (!isInverse) {
            return nullCheckToAsm(operator)
        } else {
            return inverseNullCheckToAsm(operator)
        }
    }

    fun nullCheckToAsm(operator: Operator): Int {
        if (operator === NOT_EQUAL_TO) {
            return Opcodes.IFNONNULL
        } else if (operator === EQUAL_TO) {
            return Opcodes.IFNULL
        }

        throw RuntimeException("Cannot determine reference opcode of '$operator'")
    }

}