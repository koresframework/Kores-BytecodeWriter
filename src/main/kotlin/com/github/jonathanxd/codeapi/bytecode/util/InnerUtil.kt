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
package com.github.jonathanxd.codeapi.bytecode.util

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.base.impl.MethodSpecificationImpl
import com.github.jonathanxd.codeapi.builder.MethodInvocationBuilder
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.processor.visitor.TypeVisitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.util.source.CodeArgumentUtil
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import java.util.*

object InnerUtil {
    fun genOuterAccessor(outer: TypeDeclaration,
                         inner: InnerType?,
                         memberInfo: MemberInfo,
                         extraData: Data,
                         visitorGenerator: VisitorGenerator<BytecodeClass>,
                         isConstructor: Boolean) {

        if (!memberInfo.hasAccessibleMember() || isConstructor) {
            val memberInstance = memberInfo.memberInstance

            val gen = InnerUtil.generatePackagePrivateAccess(outer, extraData, memberInstance)

            if (inner == null) {
                visitorGenerator.generateTo(MethodDeclaration::class.java, gen, requireNotNull(extraData.parent), null)
            } else {
                val source = outer.body.toMutable()

                source.add(gen)

                inner.adaptedDeclaration = outer.builder().withBody(source).build()
            }

            memberInfo.accessibleMember = gen
        }
    }

    private fun generatePackagePrivateAccess(outer: TypeDeclaration, extraData: Data, element: CodePart): MethodDeclaration {

        if (element !is ModifiersHolder || element !is Typed)
            throw IllegalArgumentException("Element doesn't match requirements: extends Modifierable & Typed.")


        val type = element.type

        var isConstructor = false
        val isStatic = element.modifiers.contains(CodeModifier.STATIC)

        val modifiers = TreeSet<CodeModifier>()

        modifiers.add(CodeModifier.SYNTHETIC)
        if (isStatic) modifiers.add(CodeModifier.STATIC)

        val invk: CodePart
        val parameters = ArrayList<CodeParameter>()

        if (element is FieldDeclaration) {

            if (!isStatic) {
                invk = CodeAPI.accessField(outer, CodeAPI.accessThis(), element.type, element.name)
            } else {
                invk = CodeAPI.accessStaticField(outer, element.type, element.name)
            }
        } else if (element is MethodDeclaration) {

            parameters.addAll(element.parameters)

            isConstructor = element.name == "<init>"

            val invokeType = if (isStatic)
                InvokeType.INVOKE_STATIC
            else
                if (isConstructor)
                    InvokeType.INVOKE_SPECIAL
                else
                    if (outer.isInterface)
                        InvokeType.INVOKE_INTERFACE
                    else
                        InvokeType.INVOKE_VIRTUAL

            val arguments = CodeArgumentUtil.argumentsFromParameters(parameters)

            if (isConstructor) {
                val current: TypeDeclaration = extraData.getRequired(TypeVisitor.TYPE_DECLARATION_REPRESENTATION)
                val parameter = CodeAPI.parameter(current, CodeSourceUtil.getNewName("\$inner", parameters))

                parameters.add(parameter)
                //arguments.add(CodeAPI.argument(CodeAPI.accessThis(), current));
            }

            invk = MethodInvocationBuilder()
                    .build {
                        this.invokeType = invokeType
                        this.localization = outer
                        this.target = if (isStatic) outer else CodeAPI.accessThis()
                        this.spec = MethodSpecificationImpl(
                                methodType = if (isConstructor) MethodType.SUPER_CONSTRUCTOR else MethodType.METHOD,
                                methodName = element.name,
                                description = TypeSpec(element.returnType, element.parameters.map { it.type })
                        )
                        this.arguments = arguments
                    }

        } else {
            throw IllegalArgumentException("Cannot process: $element!")
        }

        if (!isConstructor) {
            return CodeAPI.methodBuilder()
                    .withName(CodeSourceUtil.getNewMethodName("invoke$000", outer.body))
                    .withModifiers(modifiers)
                    .withParameters(parameters)
                    .withReturnType(type)
                    .withBody(CodeAPI.sourceOfParts(
                            CodeAPI.returnValue(type, invk)
                    ))
                    .build()
        } else {
            return CodeAPI.constructorBuilder()
                    .withModifiers(modifiers)
                    .withParameters(parameters)
                    .withBody(CodeAPI.sourceOfParts(
                            invk
                    ))
                    .build()
        }
    }
}