/*
 *      CodeAPI-BytecodeWriter - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2017 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
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

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.CatchStatement
import com.github.jonathanxd.codeapi.base.TryStatement
import com.github.jonathanxd.codeapi.base.TryWithResources
import com.github.jonathanxd.codeapi.base.VariableDeclaration
import com.github.jonathanxd.codeapi.base.impl.VariableDeclarationImpl
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.common.TypeSpec
import com.github.jonathanxd.codeapi.factory.variable
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.literal.Literals

object TryWithResourcesVisitor : VoidVisitor<TryWithResources, BytecodeClass, MVData> {

    override fun voidVisit(t: TryWithResources, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val vari = t.variable

        // Generate try-catch initialize field
        visitorGenerator.generateTo(VariableDeclaration::class.java, vari, extraData, null, additional)

        val throwableFieldName = additional.getUniqueVariableName("\$throwable_")

        // Generate exception field
        val throwableVariable = VariableDeclarationImpl(
                name = throwableFieldName,
                variableType = Types.THROWABLE,
                value = Literals.NULL)

        visitorGenerator.generateTo(VariableDeclaration::class.java, throwableVariable, extraData, null, additional)

        // Generate try block
        val catch_ = additional.getUniqueVariableName("\$catch_")
        val catchStatement = CodeAPI.catchStatement(listOf(Types.THROWABLE),
                variable(Types.THROWABLE, catch_),
                CodeAPI.source(
                        CodeAPI.setLocalVariable(Types.THROWABLE, throwableFieldName, CodeAPI.accessLocalVariable(Types.THROWABLE, catch_)),
                        CodeAPI.throwException(CodeAPI.accessLocalVariable(Types.THROWABLE, catch_))))

        val catch2_name = additional.getUniqueVariableName("\$catch_2_")

        //AutoCloseable#close();
        val closeInvocation = CodeAPI.invokeInterface(
                AutoCloseable::class.java,
                CodeAPI.accessLocalVariable(vari.variableType, vari.name),
                "close",
                TypeSpec(Types.VOID),
                emptyList())

        //Throwable#addSuppressed(Throwable)
        val addSuppressedInvocation = CodeAPI.invokeVirtual(Types.THROWABLE,
                CodeAPI.accessLocalVariable(throwableVariable.variableType, throwableVariable.name),
                "addSuppressed",
                TypeSpec(Types.VOID, listOf(Types.THROWABLE)),
                listOf(CodeAPI.argument(CodeAPI.accessLocalVariable(Types.THROWABLE, catch2_name))))

        val surroundedCloseInvocation = CodeAPI.tryStatement(CodeAPI.source(closeInvocation),
                listOf(CodeAPI.catchStatement(listOf(Types.THROWABLE),
                        variable(Types.THROWABLE, catch2_name),
                        CodeAPI.source(
                                addSuppressedInvocation
                        )
                ))
        )

        val catchStatements = java.util.ArrayList<CatchStatement>()

        catchStatements.add(catchStatement)
        catchStatements.addAll(t.catchStatements)

        val tryCatchStatement = CodeAPI.tryStatement(t.body,
                catchStatements,
                CodeAPI.source(
                        CodeAPI.ifStatement(
                                CodeAPI.checkNotNull(CodeAPI.accessLocalVariable(vari.type, vari.name)),
                                CodeAPI.source(
                                        CodeAPI.ifStatement(
                                                CodeAPI.checkNotNull(CodeAPI.accessLocalVariable(throwableVariable.type, throwableVariable.name)),
                                                CodeAPI.source(
                                                        surroundedCloseInvocation
                                                ),
                                                CodeAPI.source(
                                                        closeInvocation
                                                ))
                                )),
                        t.finallyStatement
                ))

        visitorGenerator.generateTo(TryStatement::class.java, tryCatchStatement, extraData, null, additional)
    }

}