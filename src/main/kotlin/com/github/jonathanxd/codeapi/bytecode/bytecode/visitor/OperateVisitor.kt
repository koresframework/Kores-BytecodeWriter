package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.builder.OperateHelper
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.Operate
import com.github.jonathanxd.codeapi.literals.Literals
import com.github.jonathanxd.codeapi.operators.Operator
import com.github.jonathanxd.codeapi.operators.Operators
import com.github.jonathanxd.codeapi.types.CodeType
import com.github.jonathanxd.codeapi.util.gen.CodePartUtil
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.optional.Require
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.*

object OperateVisitor : VoidVisitor<Operate, BytecodeClass, MVData> {

    override fun voidVisit(t: Operate, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val target = t.target.orElse(null)

        val operation = Require.require(t.operation, "Operation is required.")

        val value = t.value.orElse(null)

        if (operation === Operators.UNARY_BITWISE_COMPLEMENT) { // ~
            Objects.requireNonNull<CodePart>(value, "Value cannot be null if operation is '$operation'!")
            // Desugar
            val desugar = OperateHelper.builder(target)
                    .subtract(
                            OperateHelper.builder(value)
                                    .xor(Literals.INT(-1))
                                    .build()
                    )
                    .build()

            visitorGenerator.generateTo(desugar.javaClass, desugar, extraData, additional)

            return
        }

        visitorGenerator.generateTo(target.javaClass, target, extraData, additional)

        if (value != null) {
            visitorGenerator.generateTo(value.javaClass, value, extraData, additional)
        }

        when (operation) {
            Operators.ADD, Operators.SUBTRACT, Operators.MULTIPLY, Operators.DIVISION, Operators.REMAINDER,
            Operators.SIGNED_LEFT_SHIFT, // <<
            Operators.SIGNED_RIGHT_SHIFT, // >>
            Operators.UNSIGNED_RIGHT_SHIFT, // >>>
            Operators.BITWISE_EXCLUSIVE_OR, // ^
            Operators.BITWISE_INCLUSIVE_OR, // |
            Operators.BITWISE_AND // &
            -> {
                val type = CodePartUtil.getType(target)

                operateVisit(type, operation, value == null, additional)
            }
            else -> throw RuntimeException("Cannot handle operation: '$operation'!")
        }

    }

    internal fun operateVisit(codeType: CodeType, operation: Operator, valueIsNull: Boolean, mvData: MVData) {
        val type = Type.getType(codeType.javaSpecName)

        val opcode = if (operation === Operators.ADD) {
            type.getOpcode(Opcodes.IADD)
        } else if (operation === Operators.SUBTRACT) {
            if (!valueIsNull) {
                type.getOpcode(Opcodes.ISUB)
            } else {
                type.getOpcode(Opcodes.INEG)
            }
        } else if (operation === Operators.MULTIPLY) {
            type.getOpcode(Opcodes.IMUL)
        } else if (operation === Operators.DIVISION) {
            type.getOpcode(Opcodes.IDIV)
        } else if (operation === Operators.REMAINDER) {
            type.getOpcode(Opcodes.IREM)
        } else if (operation === Operators.BITWISE_AND) {
            type.getOpcode(Opcodes.IAND)
        } else if (operation === Operators.BITWISE_INCLUSIVE_OR) {
            type.getOpcode(Opcodes.IOR)
        } else if (operation === Operators.BITWISE_EXCLUSIVE_OR) {
            type.getOpcode(Opcodes.IXOR)
        } else if (operation === Operators.UNARY_BITWISE_COMPLEMENT) { // desugar
            throw IllegalArgumentException("Invalid operator: '$operation'!!!")
        } else -1

        if (opcode != -1)
            mvData.methodVisitor.visitInsn(opcode)


    }
}