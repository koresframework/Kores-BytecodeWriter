package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.impl.AccessLocalImpl
import com.github.jonathanxd.codeapi.interfaces.AccessThis
import com.github.jonathanxd.codeapi.interfaces.VariableAccess
import com.github.jonathanxd.codeapi.types.CodeType
import com.github.jonathanxd.codeapi.util.gen.CodeTypeUtil
import com.github.jonathanxd.iutils.container.MutableContainer
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object VariableAccessVisitor : VoidVisitor<VariableAccess, BytecodeClass, MVData> {

    override fun voidVisit(t: VariableAccess, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor
        var variableAccess = t

        val typeDeclaration by lazy {
            Util.find(TypeVisitor.CODE_TYPE_REPRESENTATION, extraData, null)
        }

        var localization: CodeType? = variableAccess.localization.orElse(null)

        val at = variableAccess.target.orElse(null)


        val isNull = at == null && localization == null

        if (isNull) {
            localization = typeDeclaration
        }

        val access = com.github.jonathanxd.codeapi.gen.visit.bytecode.visitor.Util.access(variableAccess, localization, visitorGenerator, extraData, additional)

        if (access != null)
            return

        if (localization != null) {

            val of = MutableContainer.of<CodeType>(localization)

            variableAccess = com.github.jonathanxd.codeapi.gen.visit.bytecode.visitor.Util.fixAccessor(variableAccess, extraData, of, null)

            localization = of.get()
        }

        if (!isNull and (at != null)) {
            visitorGenerator.generateTo(at.javaClass, at, extraData, null, additional)
        }

        if (at == null) {
            if (localization != null) {
                mv.visitFieldInsn(Opcodes.GETSTATIC, CodeTypeUtil.codeTypeToSimpleAsm(localization), variableAccess.name, CodeTypeUtil.codeTypeToFullAsm(variableAccess.variableType))
            } else {
                // THIS
                mv.visitFieldInsn(Opcodes.GETFIELD, CodeTypeUtil.codeTypeToSimpleAsm(typeDeclaration), variableAccess.name, CodeTypeUtil.codeTypeToFullAsm(variableAccess.variableType))
            }
        } else {

            if (at is AccessLocalImpl) {

                val `var` = additional.getVar(variableAccess.name, variableAccess.variableType)


                if (!`var`.isPresent)
                    throw RuntimeException("Variable '" + variableAccess.name + "' Type: '" + variableAccess.variableType.javaSpecName + "' Not found in local variables map")

                val variable = `var`.get()

                val varPosOpt = additional.getVarPos(variable)

                if (!varPosOpt.isPresent)
                    throw additional.failFind(variable)

                val i = varPosOpt.asInt

                val type = Type.getType(variable.type.javaSpecName)

                val opcode = type.getOpcode(Opcodes.ILOAD) // ALOAD

                mv.visitVarInsn(opcode, i)
            } else if (at is AccessThis) {
                // THIS
                mv.visitFieldInsn(Opcodes.GETFIELD, CodeTypeUtil.codeTypeToSimpleAsm(typeDeclaration), variableAccess.name, CodeTypeUtil.codeTypeToFullAsm(variableAccess.variableType))
            } else {
                mv.visitFieldInsn(Opcodes.GETFIELD, CodeTypeUtil.codeTypeToSimpleAsm(localization), variableAccess.name, CodeTypeUtil.codeTypeToFullAsm(variableAccess.variableType))
            }

        }
    }

}