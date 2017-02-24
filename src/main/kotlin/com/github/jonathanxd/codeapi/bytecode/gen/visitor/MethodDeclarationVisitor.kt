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

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.GENERATE_BRIDGE_METHODS
import com.github.jonathanxd.codeapi.bytecode.VALIDATE_SUPER
import com.github.jonathanxd.codeapi.bytecode.VALIDATE_THIS
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.common.Variable
import com.github.jonathanxd.codeapi.bytecode.util.*
import com.github.jonathanxd.codeapi.bytecode.util.asm.ParameterVisitor
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.gen.visit.SugarSyntaxVisitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.inspect.SourceInspect
import com.github.jonathanxd.codeapi.util.element.ElementUtil
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object MethodDeclarationVisitor : VoidVisitor<MethodDeclaration, BytecodeClass, Any?> {

    override fun voidVisit(t: MethodDeclaration, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {

        val validateSuper = visitorGenerator.options.getOrElse(VALIDATE_SUPER, true)
        val validateThis = visitorGenerator.options.getOrElse(VALIDATE_THIS, true)
        val genBridge = visitorGenerator.options.getOrElse(GENERATE_BRIDGE_METHODS, false)

        val isConstructor = t is ConstructorDeclaration

        val typeDeclaration = Util.find<TypeDeclaration>(TypeVisitor.TYPE_DECLARATION_REPRESENTATION, extraData, additional)

        if (!t.modifiers.contains(CodeModifier.BRIDGE) && genBridge) {
            val bridgeOpt = BridgeUtil.genBridgeMethod(typeDeclaration, t)

            if (bridgeOpt.isPresent) {
                val bridgeMethod = bridgeOpt.get()

                val methodSpec = ElementUtil.getMethodSpec(typeDeclaration, bridgeMethod)

                val any = !SourceInspect
                        .find { codePart -> codePart is MethodDeclaration && ElementUtil.getMethodSpec(typeDeclaration, codePart).compareTo(methodSpec) == 0 }
                        .include { bodied -> bodied is CodeSource }
                        .inspect(typeDeclaration.body).isEmpty()

                if (!any) {
                    visitorGenerator.generateTo(bridgeMethod.javaClass, bridgeMethod, extraData, additional)
                }
            }
        }

        val cw = Util.find<ClassVisitor>(TypeVisitor.CLASS_VISITOR_REPRESENTATION, extraData, additional)

        val body = t.body

        val modifiers = ArrayList(t.modifiers)

        if (!isConstructor && typeDeclaration.isInterface && !modifiers.contains(CodeModifier.ABSTRACT) && !modifiers.contains(CodeModifier.DEFAULT)) {
            modifiers.add(CodeModifier.ABSTRACT)
        }

        val isAbstract = modifiers.contains(CodeModifier.ABSTRACT)

        val asmModifiers = ModifierUtil.modifiersToAsm(modifiers)

        val parameters = t.parameters

        val signature = GenericUtil.methodGenericSignature(t)

        var methodName = t.name

        if (t is ConstructorDeclaration) {
            methodName = "<init>"
        }

        val desc = CodeTypeUtil.parametersAndReturnToInferredDesc(typeDeclaration, t, parameters, t.returnType)

        val mv = cw.visitMethod(asmModifiers, methodName, desc, signature, null)

        val mvData = MVData(mv, mutableListOf())

        // Register Sugar Env
        val sugarEnv = MVDataSugarEnvironment(mvData)
        extraData.registerData(SugarSyntaxVisitor.ENVIRONMENT, sugarEnv)

        visitorGenerator.generateTo(Annotable::class.java, t, extraData, null, mvData)

        for (i in parameters.indices) {
            val codeParameter = parameters[i]

            mv.visitParameter(codeParameter.name, 0)
            visitorGenerator.generateTo(Annotable::class.java, codeParameter, extraData, null, ParameterVisitor(mvData, i))
        }

        if (!isAbstract || isConstructor) {
            mv.visitCode()

            val startLabel = Label()
            mv.visitLabel(startLabel)


            if (modifiers.contains(CodeModifier.STATIC)) {

                ParameterUtil.parametersToVars(parameters, startLabel).forEach {
                    mvData.addVar(it)
                }
            } else {
                mvData.addVar(Variable("this", typeDeclaration, startLabel, null))
                ParameterUtil.parametersToVars(parameters, startLabel).forEach {
                    mvData.addVar(it)
                }
            }


            val l0 = Label()
            mv.visitLabel(l0)

            val bodySource = body

            var methodSource: CodeSource = CodeSource.fromIterable(bodySource)

            var isGenerated = false

            val initSuper = ConstructorUtil.searchForSuper(typeDeclaration, methodSource, validateSuper)
            val initThis = ConstructorUtil.searchInitThis(typeDeclaration, methodSource, validateThis)

            if (typeDeclaration is ClassDeclaration && isConstructor) {
                if (!initSuper && !initThis) {
                    ConstructorUtil.generateSuperInvoke(typeDeclaration, mv)
                    isGenerated = true
                }
            }

            if (isConstructor) {
                if (isGenerated) {
                    ConstructorUtil.declareFinalFields(visitorGenerator, methodSource, typeDeclaration, mv, extraData, mvData, validateThis)
                } else {
                    if (!initThis) {
                        val declarationBody = typeDeclaration.body

                        methodSource = CodeSourceUtil.insertAfter(
                                { part -> part is MethodInvocation && ConstructorUtil.isInitForThat(part) },
                                ConstructorUtil.generateFinalFields(declarationBody),
                                methodSource)
                    }
                }
            }

            visitorGenerator.generateTo(CodeSource::class.java, methodSource, extraData, null, mvData)

            /**
             * Instructions here
             */

            val returnType = CodeTypeUtil.toTypeDesc(t.returnType)
            if (returnType == "V") {
                mv.visitInsn(Opcodes.RETURN)
            }

            val end = Label()

            mv.visitLabel(end)

            mvData.visitVars(startLabel, end)

            try {
                mv.visitMaxs(0, 0)
            } catch (e: Exception) {
                RuntimeException("An exception occurred during the call of 'MethodVisitor.visitMaxs(0, 0)' (stack count and frame generation) of method '$t'!", e).printStackTrace()
            }

        }

        // Unregister sugar env
        extraData.unregisterData(SugarSyntaxVisitor.ENVIRONMENT, sugarEnv)

        mv.visitEnd()
    }

}