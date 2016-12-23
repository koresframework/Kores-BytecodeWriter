/*
 *      CodeAPI-BytecodeWriter - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2016 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
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
package com.github.jonathanxd.codeapi.bytecode.gen.visitor

import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.AccessLocal
import com.github.jonathanxd.codeapi.interfaces.VariableAccess
import com.github.jonathanxd.codeapi.interfaces.VariableOperate
import com.github.jonathanxd.codeapi.literals.Literal
import com.github.jonathanxd.codeapi.operators.Operators
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.optional.Require

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
            throw IllegalStateException("Cannot find variable '" + variable + "' in stack table: " + additional.getVariables())

        val i = varPosOpt.asInt

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

            requireNotNull(value, { "value is null, cannot operate without value using operator: '$operation'" })

            visitorGenerator.generateTo(VariableAccess::class.java, variableOperate, extraData, null, additional)

            visitorGenerator.generateTo(value!!.javaClass, value, extraData, null, additional)

            OperateVisitor.operateVisit(variableOperate.variableType, operation, false, additional)
        } else {
            requireNotNull(value, { "value is null, cannot operate without value using operator: '$operation'" })

            visitorGenerator.generateTo(VariableAccess::class.java, variableOperate, extraData, null, additional)

            visitorGenerator.generateTo(value!!.javaClass, value, extraData, null, additional)

            OperateVisitor.operateVisit(variableOperate.variableType, operation, false, additional)

        }
    }

}