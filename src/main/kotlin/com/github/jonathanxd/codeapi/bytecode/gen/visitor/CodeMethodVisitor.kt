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

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.util.*
import com.github.jonathanxd.codeapi.bytecode.util.asm.ParameterVisitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.helper.PredefinedTypes
import com.github.jonathanxd.codeapi.inspect.SourceInspect
import com.github.jonathanxd.codeapi.interfaces.*
import com.github.jonathanxd.codeapi.options.CodeOptions
import com.github.jonathanxd.codeapi.bytecode.common.Variable
import com.github.jonathanxd.codeapi.util.element.ElementUtil
import com.github.jonathanxd.codeapi.util.source.BridgeUtil
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object CodeMethodVisitor : VoidVisitor<MethodDeclaration, BytecodeClass, Any?> {

    override fun voidVisit(t: MethodDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
        val validateSuper = visitorGenerator.options.getOrElse(CodeOptions.VALIDATE_SUPER, true)
        val validateThis = visitorGenerator.options.getOrElse(CodeOptions.VALIDATE_THIS, true)
        val genBridge = visitorGenerator.options.getOrElse(CodeOptions.GENERATE_BRIDGE_METHODS, false)

        val isConstructor = t is ConstructorDeclaration

        val typeDeclaration = extraData.getRequired(TypeVisitor.CODE_TYPE_REPRESENTATION, "Cannot find CodeClass. Register 'TypeVisitor.CODE_TYPE_REPRESENTATION'!")

        if (!t.modifiers.contains(CodeModifier.BRIDGE) && genBridge) {
            val bridgeOpt = BridgeUtil.genBridgeMethod(typeDeclaration, t)

            if (bridgeOpt.isPresent) {
                val bridgeMethod = bridgeOpt.get()

                val methodSpec = ElementUtil.getMethodSpec(typeDeclaration, bridgeMethod)

                val any = !SourceInspect
                        .find { codePart -> codePart is MethodDeclaration && ElementUtil.getMethodSpec(typeDeclaration, codePart).compareTo(methodSpec) == 0 }
                        .include { bodied -> bodied is CodeSource }
                        .inspect(typeDeclaration.body.orElse(CodeSource.empty())).isEmpty()

                if (!any) {
                    visitorGenerator.generateTo(bridgeMethod.javaClass, bridgeMethod, extraData, additional)
                }
            }
        }

        val cw = Util.find(TypeVisitor.CLASS_WRITER_REPRESENTATION, extraData, additional)

        val bodyOpt = t.body

        val modifiers = ArrayList(t.modifiers)

        if (!isConstructor && !t.hasBody() && !modifiers.contains(CodeModifier.ABSTRACT)) {
            modifiers.add(CodeModifier.ABSTRACT)
        }

        val asmModifiers = ModifierUtil.modifiersToAsm(modifiers)

        val parameters = t.parameters
        val asmParameters = CodeTypeUtil.parametersToAsm(parameters)


        val signature = GenericUtil.methodGenericSignature(t)

        var methodName = t.name

        if (t is ConstructorDeclaration) {
            methodName = "<init>"
        }

        val mv = cw.visitMethod(asmModifiers, methodName, "(" + asmParameters + ")" + t.returnType.orElse(PredefinedTypes.VOID).javaSpecName, signature, null)

        val vars = java.util.ArrayList<Variable>()

        if (modifiers.contains(CodeModifier.STATIC)) {
            ParameterUtil.parametersToVars(parameters, /* to */ vars)
        } else {
            vars.add(Variable("this", typeDeclaration, null, null))
            ParameterUtil.parametersToVars(parameters, /* to */ vars)
        }

        val mvData = MVData(mv, vars)

        visitorGenerator.generateTo(Annotable::class.java, t, extraData, null, mvData)

        for (i in parameters.indices) {
            val codeParameter = parameters[i]

            visitorGenerator.generateTo(Annotable::class.java, codeParameter, extraData, null, ParameterVisitor(mvData, i))
        }

        if (t.hasBody() || isConstructor) {
            mv.visitCode()
            val l0 = Label()
            mv.visitLabel(l0)

            val bodySource = bodyOpt.orElse(null)

            var methodSource: CodeSource? = if (bodySource == null) null else CodeSource.fromIterable(bodySource)

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
                        methodSource = CodeSourceUtil.insertAfter(
                                { part -> part is MethodInvocation && ConstructorUtil.isInitForThat(part) },
                                ConstructorUtil.generateFinalFields(typeDeclaration.body.orElseThrow(::NullPointerException)),
                                methodSource)
                    }
                }
            }

            if (methodSource != null) {
                visitorGenerator.generateTo(CodeSource::class.java, methodSource, extraData, null, mvData)
            }

            /**
             * Instructions here
             */

            val returnType = t.returnType.orElse(PredefinedTypes.VOID).javaSpecName
            if (returnType == "V") {
                mv.visitInsn(Opcodes.RETURN)
            }
            try {
                mv.visitMaxs(0, 0)
            } catch (e: Exception) {
                RuntimeException("An exception occurred during the call of 'MethodVisitor.visitMaxs(0, 0)' (stack count and frame generation) of method '$t'!", e).printStackTrace()
            }


            val end = Label()

            mv.visitLabel(end)

            mvData.visitVars(l0, end)
        }

        mv.visitEnd()
    }

}