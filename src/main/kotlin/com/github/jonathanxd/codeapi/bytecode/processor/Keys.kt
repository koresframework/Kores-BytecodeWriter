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
package com.github.jonathanxd.codeapi.bytecode.processor

import com.github.jonathanxd.codeapi.CodeElement
import com.github.jonathanxd.codeapi.CodeInstruction
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.Flow
import com.github.jonathanxd.codeapi.bytecode.common.MethodVisitorHelper
import com.github.jonathanxd.codeapi.bytecode.util.AnnotationVisitorCapable
import com.github.jonathanxd.codeapi.common.FieldRef
import com.github.jonathanxd.codeapi.factory.invoke
import com.github.jonathanxd.codeapi.util.typedKeyOf
import com.github.jonathanxd.iutils.`object`.TypedKey
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.ClassVisitor

/**
 * Version of Java class to generate
 *
 * CodeAPI always generates latest version
 *
 * This can be changed via [version][CLASS_VERSION] key.
 *
 * Note that no compatibility is guarantee. Depending on features that you use,
 * the generated class may or may not be compatible in a version lower than the version
 * that CodeAPI was designed to generate.
 */
const val VERSION = 52

/**
 * Indexes known inner classes to avoid name conflicts.
 */
val INNER_CLASSES = typedKeyOf<MutableList<TypeDeclaration>>("INNER_CLASSES")

/**
 * Stores whether this is an expression or not. Used to generate `pop` instructions after unused values.
 *
 * This is set to true when the instruction is used as argument or receiver of a method, defined as values of a variable, used as
 * operand of operator, used as return value, and so on.
 */
val IN_EXPRESSION = typedKeyOf<Int>("IN_EXPRESSION")

val SOURCE_FILE_FUNCTION = typedKeyOf<(TypeDeclaration) -> String>("SOURCE_FILE_FUNCTION")

val LINE = typedKeyOf<Int>("LINE_POSITION")

val TYPE_DECLARATION = typedKeyOf<TypeDeclaration>("TYPE_DECLARATION")

val LOCATION = typedKeyOf<InnerTypesHolder>("INNER_LOCATION")

val CLASS_VISITOR = typedKeyOf<ClassVisitor>("CLASS_VISITOR")

val FLOWS = typedKeyOf<MutableList<Flow>>("CODE_FLOWS")

// MVData
val METHOD_VISITOR = typedKeyOf<MethodVisitorHelper>("METHOD_VISITOR_DATA")

// List<BytecodeClass>
val BYTECODE_CLASS_LIST = typedKeyOf<MutableList<BytecodeClass>>("BYTECODE_CLASS_LIST")

val CLASS_VERSION = typedKeyOf<Int>("CLASS_VERSION")

val OUTER_DESC = typedKeyOf<String>("OUTER_METHOD_DESC")

val ANNOTATION_VISITOR_CAPABLE = typedKeyOf<AnnotationVisitorCapable>("ANNOTATION_VISITOR_CAPABLE")

val IN_INVOKE_DYNAMIC = typedKeyOf<Unit>("IN_INVOKE_DYNAMIC")

val TYPES = typedKeyOf<MutableList<TypeDeclaration>>("TYPES_VISIT")

/**
 * Keep track of outer fields added to inner class.
 */
val OUTER_TYPE_FIELD = typedKeyOf<OuterClassField>("OUTER_TYPE_FIELD")

data class OuterClassField(val typeDeclaration: TypeDeclaration, val field: FieldRef)

val MEMBER_ACCESSES = typedKeyOf<MutableList<MemberAccess>>("MEMBER_ACCESSES")

data class MemberAccess(val from: TypeDeclaration,
                        val member: CodeElement,
                        val owner: TypeDeclaration,
                        val newElementToAccess: MethodDeclarationBase) {

    fun createInvokeToNewElement(target: CodeInstruction, args: List<CodeInstruction>) = invoke(
            if (newElementToAccess is ConstructorDeclaration) InvokeType.INVOKE_SPECIAL
            else InvokeType.INVOKE_STATIC,
            owner,
            target,
            newElementToAccess.name,
            newElementToAccess.typeSpec,
            args
    )

}

fun TypedKey<Int>.get(data: TypedData): Int {
    return data.getOrSet(this.key, 0, this.type)
}

fun TypedKey<Int>.increment(data: TypedData) {
    data.set(this.key, data.getOrSet(this.key, 0, this.type) + 1, this.type)
}

fun TypedKey<Int>.decrement(data: TypedData) {
    data.set(this.key, data.getOrSet(this.key, 0, this.type) - 1, this.type)
}

inline fun TypedKey<Int>.incrementInContext(data: TypedData, context: () -> Unit) {
    this.increment(data)
    context()
    this.decrement(data)
}
