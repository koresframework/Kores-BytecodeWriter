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
package com.github.jonathanxd.codeapi.bytecode.processor.processors

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.GENERATE_BRIDGE_METHODS
import com.github.jonathanxd.codeapi.bytecode.VALIDATE_SUPER
import com.github.jonathanxd.codeapi.bytecode.VALIDATE_THIS
import com.github.jonathanxd.codeapi.bytecode.common.MethodVisitorHelper
import com.github.jonathanxd.codeapi.bytecode.common.Variable
import com.github.jonathanxd.codeapi.bytecode.processor.*
import com.github.jonathanxd.codeapi.bytecode.util.*
import com.github.jonathanxd.codeapi.bytecode.util.asm.ParameterVisitor
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.util.*
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.jwiutils.kt.inContext
import com.github.jonathanxd.jwiutils.kt.require
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object MethodDeclarationProcessor : Processor<MethodDeclarationBase> {

    override fun process(part: MethodDeclarationBase, data: TypedData, processorManager: ProcessorManager<*>) {
        val old =
                if (IN_EXPRESSION.contains(data)) IN_EXPRESSION.require(data)
                else null

        IN_EXPRESSION.set(data, 0)
        val validateSuper = processorManager.options.getOrElse(VALIDATE_SUPER, true)
        val validateThis = processorManager.options.getOrElse(VALIDATE_THIS, true)
        val genBridge = processorManager.options.getOrElse(GENERATE_BRIDGE_METHODS, false)

        val isConstructor = part is ConstructorDeclaration

        if (isConstructor && TYPE_DECLARATION.getOrNull(data)?.isInterface == true)
            throw IllegalArgumentException("Cannot declare a constructor in an interface.")

        val typeDeclaration: Lazy<TypeDeclaration> = lazy {
            TYPE_DECLARATION.require(data)
        }

        if (!part.modifiers.contains(CodeModifier.BRIDGE) && genBridge) {
            val bridgeOpt = BridgeUtil.genBridgeMethod(typeDeclaration.value, part)

            bridgeOpt.forEach { bridgeMethod ->
                val methodSpec = bridgeMethod.getMethodSpec(typeDeclaration.value)

                val any = typeDeclaration.value.methods.any {
                    it.getMethodSpec(typeDeclaration.value).compareTo(methodSpec) == 0
                }

                if (!any) {
                    processorManager.process(bridgeMethod::class.java, bridgeMethod, data)
                }
            }

        }

        val visitor = CLASS_VISITOR.require(data)

        val modifiers = ArrayList(part.modifiers)

        if (!isConstructor
                && !modifiers.contains(CodeModifier.ABSTRACT)
                && !modifiers.contains(CodeModifier.FINAL)
                && !modifiers.contains(CodeModifier.DEFAULT)
                && part.body.isEmpty
                && typeDeclaration.value.isInterface) {
            modifiers.add(CodeModifier.ABSTRACT)
        }

        val parameters = part.parameters

        val isAbstract = modifiers.contains(CodeModifier.ABSTRACT)

        val asmModifiers = ModifierUtil.modifiersToAsm(modifiers)

        val signature = part.methodGenericSignature()

        val desc = parametersAndReturnToInferredDesc(typeDeclaration, part, parameters, part.returnType)

        val throws = if (part.throwsClause.isEmpty()) null else part.throwsClause.map { it.internalName }.toTypedArray()

        val mvHelper = MethodVisitorHelper(visitor.visitMethod(asmModifiers, part.name, desc, signature, throws), mutableListOf())

        ANNOTATION_VISITOR_CAPABLE.inContext(data, AnnotationVisitorCapable.MethodVisitorCapable(mvHelper.methodVisitor)) {
            processorManager.process(Annotable::class.java, part, data)
        }

        for (i in parameters.indices) {
            val codeParameter = parameters[i]

            mvHelper.methodVisitor.visitParameter(codeParameter.name, 0)

            ANNOTATION_VISITOR_CAPABLE.inContext(data, AnnotationVisitorCapable.ParameterVisitorCapable(ParameterVisitor(mvHelper, i))) {
                processorManager.process(Annotable::class.java, codeParameter, data)
            }
        }

        if (!isAbstract || isConstructor) {
            mvHelper.methodVisitor.visitCode()

            METHOD_VISITOR.inContext(data, mvHelper) {
                val startLabel = Label()
                mvHelper.methodVisitor.visitLabel(startLabel)

                if (!modifiers.contains(CodeModifier.STATIC)) {
                    mvHelper.addVar(Variable("this", typeDeclaration.value, startLabel, null))
                }

                ParameterUtil.parametersToVars(parameters, startLabel).forEach {
                    mvHelper.addVar(it)
                }

                val firstLabel = Label()

                mvHelper.methodVisitor.visitLabel(firstLabel)

                var methodSource: CodeSource = CodeSource.fromIterable(part.body)

                var isGenerated = false

                if (isConstructor) {
                    val initSuper = ConstructorUtil.searchForSuper(typeDeclaration.value, methodSource, validateSuper)
                    val initThis = ConstructorUtil.searchInitThis(typeDeclaration.value, methodSource, validateThis)

                    if (typeDeclaration.value is ClassDeclaration) {
                        if (!initSuper && !initThis) {
                            ConstructorUtil.generateSuperInvoke(typeDeclaration.value, mvHelper.methodVisitor)
                            isGenerated = true
                        }
                    }

                    if (isGenerated) {
                        ConstructorUtil.declareFinalFields(processorManager, methodSource, typeDeclaration.value, mvHelper, data, validateThis)
                    } else {
                        if (!initThis) {
                            val elementsHolder = typeDeclaration.value

                            methodSource = insertAfter(
                                    { testPart ->
                                        val safe = testPart.safeForComparison
                                        safe is MethodInvocation && ConstructorUtil.isInitForThat(safe)
                                    },
                                    ConstructorUtil.generateFinalFields(elementsHolder),
                                    methodSource)
                        }
                    }
                }

                processorManager.process(CodeSource::class.java, methodSource, data)

                val returnType = part.returnType.typeDesc

                if (returnType == "V" && methodSource.lastOrNull() !is Return) {
                    mvHelper.methodVisitor.visitInsn(Opcodes.RETURN)
                }

                val end = Label()

                mvHelper.methodVisitor.visitLabel(end)

                mvHelper.visitVars(end)

                try {
                    mvHelper.methodVisitor.visitMaxs(0, 0)
                } catch (e: Exception) {
                    RuntimeException("An exception occurred during the call of 'MethodVisitor.visitMaxs(0, 0)' (stack count and frame generation) of method '$part'!", e).printStackTrace()
                }
            }
        }

        IN_EXPRESSION.remove(data)

        if (old != null) IN_EXPRESSION.set(data, old)

        mvHelper.methodVisitor.visitEnd()

    }


}