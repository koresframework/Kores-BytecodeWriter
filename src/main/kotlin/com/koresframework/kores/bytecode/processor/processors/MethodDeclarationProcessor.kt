/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2021 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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
package com.koresframework.kores.bytecode.processor.processors

import com.koresframework.kores.Instructions
import com.koresframework.kores.base.*
import com.koresframework.kores.bytecode.GENERATE_BRIDGE_METHODS
import com.koresframework.kores.bytecode.VALIDATE_SUPER
import com.koresframework.kores.bytecode.VALIDATE_THIS
import com.koresframework.kores.bytecode.common.MethodVisitorHelper
import com.koresframework.kores.bytecode.common.Variable
import com.koresframework.kores.bytecode.processor.*
import com.koresframework.kores.bytecode.util.*
import com.koresframework.kores.bytecode.util.asm.ParameterVisitor
import com.koresframework.kores.insertAfter
import com.koresframework.kores.processor.Processor
import com.koresframework.kores.processor.ProcessorManager
import com.koresframework.kores.safeForComparison
import com.koresframework.kores.type.internalName
import com.koresframework.kores.util.parametersAndReturnToInferredDesc
import com.koresframework.kores.util.typeDesc
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.add
import com.github.jonathanxd.iutils.kt.inContext
import com.github.jonathanxd.iutils.kt.require
import com.koresframework.kores.util.methodClassfileGenericSignature
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object MethodDeclarationProcessor : Processor<MethodDeclarationBase> {

    override fun process(
        part: MethodDeclarationBase,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {
        val old =
            if (IN_EXPRESSION.contains(data)) IN_EXPRESSION.require(data)
            else null

        METHOD_DECLARATIONS.add(data, part)
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

        if (!part.modifiers.contains(KoresModifier.BRIDGE) && genBridge) {
            val bridgeOpt = BridgeUtil.genBridgeMethod(typeDeclaration.value, part)

            bridgeOpt.forEach { bridgeMethod ->
                val methodSpec = bridgeMethod.getMethodSpec(typeDeclaration.value)

                val none =
                    (typeDeclaration.value.methods + METHOD_DECLARATIONS.require(data)).none {
                        it.getMethodSpec(typeDeclaration.value).isConcreteEq(methodSpec)
                    }

                if (none) {
                    processorManager.process(bridgeMethod::class.java, bridgeMethod, data)
                }
            }

        }

        val visitor = CLASS_VISITOR.require(data)

        val modifiers = ArrayList(part.modifiers)

        if (!isConstructor
                && !modifiers.contains(KoresModifier.ABSTRACT)
                && !modifiers.contains(KoresModifier.FINAL)
                && !modifiers.contains(KoresModifier.DEFAULT)
                && part.body.isEmpty
                && typeDeclaration.value.isInterface
        ) {
            modifiers.add(KoresModifier.ABSTRACT)
        }

        val parameters = part.parameters

        val isAbstract = modifiers.contains(KoresModifier.ABSTRACT)

        val asmModifiers = ModifierUtil.modifiersToAsm(modifiers)

        val signature = part.methodClassfileGenericSignature()

        val desc =
            parametersAndReturnToInferredDesc(typeDeclaration, part, parameters, part.returnType)

        val throws =
            if (part.throwsClause.isEmpty()) null else part.throwsClause.map { it.internalName }.toTypedArray()

        val mvHelper = MethodVisitorHelper(
            visitor.visitMethod(
                asmModifiers,
                part.name,
                desc,
                signature,
                throws
            ), mutableListOf()
        )

        ANNOTATION_VISITOR_CAPABLE.inContext(
            data,
            AnnotationVisitorCapable.MethodVisitorCapable(mvHelper.methodVisitor)
        ) {
            processorManager.process(Annotable::class.java, part, data)
        }

        for (i in parameters.indices) {
            val codeParameter = parameters[i]

            mvHelper.methodVisitor.visitParameter(codeParameter.name, 0)

            ANNOTATION_VISITOR_CAPABLE.inContext(
                data,
                AnnotationVisitorCapable.ParameterVisitorCapable(ParameterVisitor(mvHelper, i))
            ) {
                processorManager.process(Annotable::class.java, codeParameter, data)
            }
        }

        if (!isAbstract || isConstructor) {
            mvHelper.methodVisitor.visitCode()

            METHOD_VISITOR.inContext(data, mvHelper) {
                val startLabel = Label()
                mvHelper.methodVisitor.visitLabel(startLabel)

                if (!modifiers.contains(KoresModifier.STATIC)) {
                    mvHelper.addVar(Variable("this", typeDeclaration.value, startLabel, null))
                }

                ParameterUtil.parametersToVars(parameters, startLabel).forEach {
                    mvHelper.addVar(it)
                }

                val firstLabel = Label()

                mvHelper.methodVisitor.visitLabel(firstLabel)

                var methodSource: Instructions = Instructions.fromIterable(part.body)

                var isGenerated = false

                if (isConstructor) {
                    val initSuper = ConstructorUtil.searchForSuper(
                        typeDeclaration.value,
                        methodSource,
                        validateSuper
                    )
                    val initThis = ConstructorUtil.searchInitThis(
                        typeDeclaration.value,
                        methodSource,
                        validateThis
                    )

                    if (typeDeclaration.value is ClassDeclaration) {
                        if (!initSuper && !initThis) {
                            ConstructorUtil.generateSuperInvoke(
                                typeDeclaration.value,
                                mvHelper.methodVisitor
                            )
                            isGenerated = true
                        }
                    }

                    if (isGenerated) {
                        ConstructorUtil.declareFinalFields(
                            processorManager,
                            methodSource,
                            typeDeclaration.value,
                            mvHelper,
                            data,
                            validateThis
                        )
                    } else {
                        if (!initThis) {
                            val elementsHolder = typeDeclaration.value

                            methodSource = methodSource.insertAfter(
                                { testPart ->
                                    val safe = testPart.safeForComparison
                                    safe is MethodInvocation && ConstructorUtil.isInitForThat(safe)
                                },
                                ConstructorUtil.generateFinalFields(elementsHolder)
                            )
                        }
                    }
                }

                processorManager.process(Instructions::class.java, methodSource, data)

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
                    RuntimeException(
                        "An exception occurred during the call of 'MethodVisitor.visitMaxs(0, 0)' (stack count and frame generation) of method '$part'!",
                        e
                    ).printStackTrace()
                }
            }
        }

        IN_EXPRESSION.remove(data)

        if (old != null) IN_EXPRESSION.set(data, old)

        mvHelper.methodVisitor.visitEnd()

    }


}