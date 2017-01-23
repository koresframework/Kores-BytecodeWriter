package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.AccessLocal
import com.github.jonathanxd.codeapi.interfaces.VariableAccess
import com.github.jonathanxd.codeapi.interfaces.VariableOperate
import com.github.jonathanxd.codeapi.literals.Literal
import com.github.jonathanxd.codeapi.operators.Operator
import com.github.jonathanxd.codeapi.operators.Operators
import com.github.jonathanxd.codeapi.types.CodeType
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.optional.Require
import java.util.*

object VariableOperateVisitor : VoidVisitor<VariableOperate, BytecodeClass, MVData> {

    override fun voidVisit(variableOperate: VariableOperate, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val at = variableOperate.target.orElse(null)

        val operation = Require.require(variableOperate.operation, "Operation is required.")

        val value = variableOperate.value.orElse(null)

        var constantVal = true

        var constant = 1

        if (value != null && (value !is Literal || Require.require(value.type, "Literal Type required").javaSpecName != "I")) {
            constantVal = false
        } else if (value != null) {
            constant = Integer.valueOf((value as Literal).name)!!
        }


        val `var` = additional.getVar(variableOperate.name, variableOperate.variableType)


        if (!`var`.isPresent)
            throw RuntimeException("Variable '" + variableOperate.name + "' Type: '" + variableOperate.variableType.javaSpecName + "' Not found in local variables map")

        val variable = `var`.get()

        val varPosOpt = additional.getVarPos(variable)

        if (!varPosOpt.isPresent)
            throw IllegalStateException("Cannot find variable '" + variable + "' in stack table: " + additional.variables)

        val i = varPosOpt.asInt.toInt()

        if (at is AccessLocal) {
            if (operation === Operators.INCREMENT) {
                mv.visitIincInsn(i, 1)
                return
            } else if (operation === Operators.DECREMENT) {
                mv.visitIincInsn(i, -1)
                return
            } else if (constantVal) {
                if (operation === Operators.ADD) {
                    mv.visitIincInsn(i, constant)
                    return
                }
                if (operation === Operators.SUBTRACT) {
                    mv.visitIincInsn(i, -constant)
                    return
                }
            }

            requireNotNull(value, {"value is null, cannot operate without value using operator: '$operation'"})

            visitorGenerator.generateTo(VariableAccess::class.java, variableOperate, extraData, null, additional)

            visitorGenerator.generateTo(value!!.javaClass, value, extraData, null, additional)

            OperateVisitor.operateVisit(variableOperate.variableType, operation, false, additional)
        } else {
            requireNotNull(value, {"value is null, cannot operate without value using operator: '$operation'"})

            visitorGenerator.generateTo(VariableAccess::class.java, variableOperate, extraData, null, additional)

            visitorGenerator.generateTo(value!!.javaClass, value, extraData, null, additional)

            OperateVisitor.operateVisit(variableOperate.variableType, operation, false, additional)

        }
    }

}