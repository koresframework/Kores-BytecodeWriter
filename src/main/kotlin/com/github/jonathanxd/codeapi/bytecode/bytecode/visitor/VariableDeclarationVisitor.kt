package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.gen.visit.bytecode.visitor.HiddenField
import com.github.jonathanxd.codeapi.gen.visit.bytecode.visitor.TypeVisitor
import com.github.jonathanxd.codeapi.gen.visit.bytecode.visitor.Util
import com.github.jonathanxd.codeapi.impl.AccessLocalImpl
import com.github.jonathanxd.codeapi.interfaces.AccessThis
import com.github.jonathanxd.codeapi.interfaces.FieldDeclaration
import com.github.jonathanxd.codeapi.interfaces.TypeDeclaration
import com.github.jonathanxd.codeapi.interfaces.VariableDeclaration
import com.github.jonathanxd.codeapi.literals.Literals
import com.github.jonathanxd.codeapi.util.gen.CodeTypeUtil
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object VariableDeclarationVisitor : VoidVisitor<VariableDeclaration, BytecodeClass, MVData> {

    override fun voidVisit(variableDeclaration: VariableDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val typeDeclaration = Util.find(TypeVisitor.CODE_TYPE_REPRESENTATION, extraData, null)

        val localization = variableDeclaration.localization.orElse(null)


        val value = variableDeclaration.value.orElse(Literals.NULL)

        val at = variableDeclaration.target.orElse(null)

        if (at == null && localization == null) {
            mv.visitVarInsn(Opcodes.ALOAD, 0) // Legacy
        } else if (at != null) {
            visitorGenerator.generateTo(at.javaClass, at, extraData, null, additional)
        }

        visitorGenerator.generateTo(value.javaClass, value, extraData, null, additional)

        if (at == null) {
            if (localization != null) {
                mv.visitFieldInsn(Opcodes.PUTSTATIC, CodeTypeUtil.codeTypeToSimpleAsm(localization), variableDeclaration.name, CodeTypeUtil.codeTypeToFullAsm(variableDeclaration.variableType))
            } else {
                mv.visitFieldInsn(Opcodes.PUTFIELD, CodeTypeUtil.codeTypeToSimpleAsm(typeDeclaration), variableDeclaration.name, CodeTypeUtil.codeTypeToFullAsm(variableDeclaration.variableType))
            }
        } else {
            if (at is AccessLocalImpl) {

                val `var` = additional.getVar(variableDeclaration.name, variableDeclaration.variableType)


                if (!`var`.isPresent() && variableDeclaration !is FieldDeclaration)
                    throw RuntimeException("Missing Variable Definition. Variable: '" + variableDeclaration.name + "' Type: '" + variableDeclaration.variableType.javaSpecName + "'.")
                else if (`var`.isPresent() && variableDeclaration is FieldDeclaration && variableDeclaration !is HiddenField)
                    throw RuntimeException("Variable '" + variableDeclaration.getName() + "' Type: '" + variableDeclaration.getVariableType().javaSpecName + "'. Already defined!")


                val i_label = Label()

                mv.visitLabel(i_label)

                val i: Int

                if (variableDeclaration is HiddenField) {
                    i = additional.storeInternalVar(variableDeclaration.getName(), variableDeclaration.getVariableType(), i_label, null)
                            .orElseThrow({ additional.failStore(variableDeclaration) })
                } else {
                    i = additional.storeVar(variableDeclaration.name, variableDeclaration.variableType, i_label, null)
                            .orElseThrow({ additional.failStore(variableDeclaration) })
                }

                val type = Type.getType(variableDeclaration.variableType.javaSpecName)

                val opcode = type.getOpcode(Opcodes.ISTORE) // ALOAD

                mv.visitVarInsn(opcode, i)

            } else if (at is AccessThis) {
                // THIS
                mv.visitFieldInsn(Opcodes.PUTFIELD, CodeTypeUtil.codeTypeToSimpleAsm(typeDeclaration), variableDeclaration.name, CodeTypeUtil.codeTypeToFullAsm(variableDeclaration.variableType))
            } else {
                mv.visitFieldInsn(Opcodes.PUTFIELD, CodeTypeUtil.codeTypeToSimpleAsm(localization), variableDeclaration.name, CodeTypeUtil.codeTypeToFullAsm(variableDeclaration.variableType))
            }
        }
    }

}