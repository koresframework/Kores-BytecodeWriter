package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.helper.Helper
import com.github.jonathanxd.codeapi.helper.PredefinedTypes
import com.github.jonathanxd.codeapi.interfaces.IfBlock
import com.github.jonathanxd.codeapi.interfaces.IfExpr
import com.github.jonathanxd.codeapi.literals.Literals
import com.github.jonathanxd.codeapi.operators.Operators
import com.github.jonathanxd.codeapi.util.gen.CodePartUtil
import com.github.jonathanxd.codeapi.util.gen.IfUtil
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object BytecodeIfBlockVisitor {
    fun visit(ifBlock: IfBlock,
              ifStartLabel: Label,
              outOfIfLabel: Label,
              revert: Boolean,
              jumpToStart: Boolean,
              extraData: MapData,
              visitorGenerator: VisitorGenerator<BytecodeClass>,
              mvData: MVData) {

        val visitor = mvData.methodVisitor

        val ifExpression = ArrayList(ifBlock.ifExprsAndOps)

        val listIterator = ifExpression.listIterator()

        val elseBlock = ifBlock.elseBlock

        val inIfLabel = Label()

        val elseLabel = if (elseBlock.isPresent) Label() else null

        while (listIterator.hasNext()) {
            val current = listIterator.next()
            var next: CodePart? = null

            if (listIterator.hasNext()) {
                next = listIterator.next()
                listIterator.previous()
            }

            if (current is IfExpr) {
                val isInverse = !revert == (next == null || next !== Operators.OR)

                val lbl = if (ifBlock is SwitchVisitor.SwitchIfBlock) { // Workaround ?
                    extraData.getRequired(ConstantDatas.FLOW_TYPE_INFO).insideEnd
                } else {
                    if (jumpToStart) ifStartLabel else if (!isInverse) inIfLabel else elseLabel ?: outOfIfLabel // Jump to else if exists
                }

                val operation = current.operation

                var expr1 = current.expr1
                var expr2 = current.expr2

                val expr1Type = CodePartUtil.getType(expr1)
                val expr2Type = CodePartUtil.getType(expr2)

                val expr1Primitive = CodePartUtil.isPrimitive(expr1)
                val expr2Primitive = CodePartUtil.isPrimitive(expr2)

                val firstIsBoolean = expr1Primitive && CodePartUtil.isBoolean(expr1)
                val secondIsBoolean = expr2Primitive && CodePartUtil.isBoolean(expr2)

                if (firstIsBoolean || secondIsBoolean) {
                    val operatorIsEq = operation === Operators.EQUAL_TO
                    val value = if (firstIsBoolean) CodePartUtil.getBooleanValue(expr1) else CodePartUtil.getBooleanValue(expr2)
                    var opcode = IfUtil.getIfNeEqOpcode(value)

                    if (!operatorIsEq)
                        opcode = IfUtil.invertIfNeEqOpcode(opcode)

                    if (isInverse)
                        opcode = IfUtil.invertIfNeEqOpcode(opcode)

                    if (firstIsBoolean) {
                        visitorGenerator.generateTo(expr2.javaClass, expr2, extraData, null, mvData)
                        visitor.visitJumpInsn(opcode, lbl)
                    } else {
                        visitorGenerator.generateTo(expr1.javaClass, expr1, extraData, null, mvData)
                        visitor.visitJumpInsn(opcode, lbl)
                    }

                } else {
                    // Old Code ->
                    // TODO: Rewrite

                    if (expr1Primitive != expr2Primitive) {

                        if (expr2Primitive) {
                            expr1 = Helper.cast(expr1Type, expr2Type, expr1)
                        } else {
                            expr2 = Helper.cast(expr2Type, expr1Type, expr2)
                        }
                    }

                    visitorGenerator.generateTo(expr1.javaClass, expr1, extraData, null, mvData)

                    if (expr2 === Literals.NULL) {
                        visitor.visitJumpInsn(Operators.nullCheckToAsm(operation, isInverse), lbl)
                    } else if (CodePartUtil.isPrimitive(expr1) && CodePartUtil.isPrimitive(expr2)) {
                        visitorGenerator.generateTo(expr2.javaClass, expr2, extraData, null, mvData)

                        val firstType = CodePartUtil.getType(expr1)
                        val secondType = CodePartUtil.getType(expr2)

                        if (!firstType.`is`(secondType))
                            throw IllegalArgumentException("'$expr1' and '$expr2' have different types, cast it to correct type.")

                        var generateCMPCheck = false

                        if (expr1Type.`is`(PredefinedTypes.LONG)) {
                            visitor.visitInsn(Opcodes.LCMP)
                            generateCMPCheck = true
                        } else if (expr1Type.`is`(PredefinedTypes.DOUBLE)) {
                            visitor.visitInsn(Opcodes.DCMPG)
                            generateCMPCheck = true
                        } else if (expr1Type.`is`(PredefinedTypes.FLOAT)) {
                            visitor.visitInsn(Opcodes.FCMPG)
                            generateCMPCheck = true
                        }

                        var check = Operators.primitiveToAsm(operation, isInverse)

                        if (generateCMPCheck) {
                            check = Operators.convertToSimpleIf(check)
                        }

                        visitor.visitJumpInsn(check, lbl)
                    } else {
                        visitorGenerator.generateTo(expr2.javaClass, expr2, extraData, null, mvData)

                        visitor.visitJumpInsn(Operators.referenceToAsm(operation, isInverse), lbl)
                    }
                }

            }

        }

        visitor.visitLabel(inIfLabel)

        val body = ifBlock.body.orElseThrow(::RuntimeException)

        visitorGenerator.generateTo(CodeSource::class.java, body, extraData, null, mvData)

        if (elseLabel != null) {
            visitor.visitJumpInsn(Opcodes.GOTO, outOfIfLabel)
        }


        if (elseLabel != null) {
            visitor.visitLabel(elseLabel)

            if (elseBlock.isPresent) {

                val elseBlock_ = elseBlock.get()

                val elseBodyOpt = elseBlock_.body

                if (elseBodyOpt.isPresent) {
                    val elseBody = elseBodyOpt.get()

                    visitorGenerator.generateTo(CodeSource::class.java, elseBody, extraData, null, mvData)
                }
            }
        }

    }

}