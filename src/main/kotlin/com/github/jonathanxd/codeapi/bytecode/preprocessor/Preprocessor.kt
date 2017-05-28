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
package com.github.jonathanxd.codeapi.bytecode.preprocessor

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.util.ReflectType
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.util.codeType
import com.github.jonathanxd.codeapi.util.typedKeyOf
import com.github.jonathanxd.iutils.`object`.TypedKey
import com.github.jonathanxd.iutils.data.TypedData
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 * A fast pre-processor which finds out all inner type declarations which is outside of explicit declaration.
 *
 * A Utility to generate this automatic should be good.
 */
object Preprocessor {

    // Dev annotation
    // I'm trying to get all inner TypeDeclaration in a faster way using method handles and visit pattern

    val LOOKUP = MethodHandles.publicLookup()

    inline fun preProcess(part: () -> ReflectType, data: TypedData) {
        return preProcess(part().codeType, data)
    }

    fun preProcess(part: CodePart, data: TypedData) {

        val type = part::class.java

        val virtual = LOOKUP.findVirtual(
                Preprocessor::class.java,
                "preprocess",
                MethodType.methodType(Unit::class.java, type, TypedData::class.java))

        virtual.invokeWithArguments(part, data)
    }

    // Special

    fun CodeType.preprocess(data: TypedData) {

    }

    fun CodeSource.preprocess(data: TypedData) {
        this.forEach {
            preProcess(it, data)
        }
    }

    // Base

    fun AnnotationDeclaration.preprocess(data: TypedData) {

    }

    fun AnnotationProperty.preprocess(data: TypedData) {

    }

    fun AnonymousClass.preprocess(data: TypedData) {

    }

    fun ArrayConstructor.preprocess(data: TypedData) {

    }

    fun ArrayLength.preprocess(data: TypedData) {

    }

    fun ArrayLoad.preprocess(data: TypedData) {

    }

    fun ArrayStore.preprocess(data: TypedData) {

    }

    fun Case.preprocess(data: TypedData) {

    }

    fun Cast.preprocess(data: TypedData) {

    }

    fun CatchStatement.preprocess(data: TypedData) {

    }

    fun ClassDeclaration.preprocess(data: TypedData) {

    }

    fun CodeParameter.preprocess(data: TypedData) {

    }

    fun Concat.preprocess(data: TypedData) {

    }

    fun ConstructorDeclaration.preprocess(data: TypedData) {

    }

    fun ControlFlow.preprocess(data: TypedData) {

    }

    fun EnumDeclaration.preprocess(data: TypedData) {

    }

    fun EnumEntry.preprocess(data: TypedData) {

    }

    fun EnumValue.preprocess(data: TypedData) {

    }

    fun FieldAccess.preprocess(data: TypedData) {

    }

    fun FieldDeclaration.preprocess(data: TypedData) {

    }

    fun FieldDefinition.preprocess(data: TypedData) {

    }

    fun ForEachStatement.preprocess(data: TypedData) {

    }

    fun ForStatement.preprocess(data: TypedData) {

    }

    fun IfExpr.preprocess(data: TypedData) {

    }

    fun IfGroup.preprocess(data: TypedData) {

    }

    fun IfStatement.preprocess(data: TypedData) {

    }

    fun InstanceOfCheck.preprocess(data: TypedData) {

    }

    fun InterfaceDeclaration.preprocess(data: TypedData) {

    }

    fun Label.preprocess(data: TypedData) {

    }

    fun LocalCode.preprocess(data: TypedData) {

    }

    fun MethodInvocation.preprocess(data: TypedData) {

    }

    fun New.preprocess(data: TypedData) {

    }

    fun Operate.preprocess(data: TypedData) {

    }

    fun Return.preprocess(data: TypedData) {

    }

    fun StaticBlock.preprocess(data: TypedData) {

    }

    fun Synchronized.preprocess(data: TypedData) {

    }

    fun ThrowException.preprocess(data: TypedData) {

    }

    fun TryStatement.preprocess(data: TypedData) {

    }

    fun TryWithResources.preprocess(data: TypedData) {

    }

    fun TypeSpec.preprocess(data: TypedData) {

    }

    fun VariableAccess.preprocess(data: TypedData) {

    }

    fun VariableDeclaration.preprocess(data: TypedData) {

    }

    fun VariableDefinition.preprocess(data: TypedData) {

    }

    fun WhileStatement.preprocess(data: TypedData) {

    }
}

val INNER_TYPE_INFO = typedKeyOf<MutableList<InnerTypeInfo>>("INNER_TYPES_INFO")


data class InnerTypeInfo(val outer: TypeDeclaration, val inner: TypeDeclaration)