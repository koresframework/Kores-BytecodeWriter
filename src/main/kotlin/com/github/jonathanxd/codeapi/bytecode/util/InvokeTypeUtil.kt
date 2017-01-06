package com.github.jonathanxd.codeapi.bytecode.util

import com.github.jonathanxd.codeapi.common.InvokeType
import com.github.jonathanxd.codeapi.common.InvokeType.*
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
            INVOKE_DYNAMIC -> return INVOKEDYNAMIC
            else -> throw RuntimeException("Cannot determine opcode of '$invokeType'")
        }
    }

    /**
     * Convert [InvokeType] to asm [dynamic] invocation opcode.
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
            INVOKE_DYNAMIC -> throw RuntimeException("Cannot invoke dynamic 'dynamic invocation'!")
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
            INVOKEDYNAMIC -> return INVOKE_DYNAMIC
            else -> throw RuntimeException("Cannot determine InvokeType of opcde '$opcode'")
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