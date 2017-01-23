package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.gen.visit.bytecode.visitor.TypeVisitor
import com.github.jonathanxd.codeapi.gen.visit.bytecode.visitor.Util
import com.github.jonathanxd.codeapi.helper.PredefinedTypes
import com.github.jonathanxd.codeapi.inspect.SourceInspect
import com.github.jonathanxd.codeapi.interfaces.*
import com.github.jonathanxd.codeapi.options.CodeOptions
import com.github.jonathanxd.codeapi.util.Variable
import com.github.jonathanxd.codeapi.util.asm.ParameterVisitor
import com.github.jonathanxd.codeapi.util.element.ElementUtil
import com.github.jonathanxd.codeapi.util.gen.*
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
                                { part -> part is MethodInvocation && ConstructorUtil.isInitForThat(typeDeclaration, part) },
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