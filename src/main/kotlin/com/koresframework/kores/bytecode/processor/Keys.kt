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
package com.koresframework.kores.bytecode.processor

import com.koresframework.kores.KoresElement
import com.koresframework.kores.Instruction
import com.koresframework.kores.Instructions
import com.koresframework.kores.base.*
import com.koresframework.kores.bytecode.common.Flow
import com.koresframework.kores.bytecode.common.MethodVisitorHelper
import com.koresframework.kores.bytecode.common.Timed
import com.koresframework.kores.bytecode.util.AnnotationVisitorCapable
import com.koresframework.kores.common.FieldRef
import com.koresframework.kores.factory.invoke
import com.koresframework.kores.processor.ProcessorManager
import com.github.jonathanxd.iutils.`object`.TypedKey
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.containsKey
import com.github.jonathanxd.iutils.kt.require
import com.github.jonathanxd.iutils.kt.typedKeyOf
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import java.time.Instant

/**
 * Version of Java class to generate
 *
 * Kores always generates the latest version
 *
 * This can be changed via [version][CLASS_VERSION] key.
 *
 * Note that no compatibility is guaranteed. Depending on features that you use,
 * the generated class may or may not be compatible in a version lower than the version
 * that CodeAPI was designed to generate.
 */
const val VERSION = 60

/**
 * Indexes known inner classes to avoid name conflicts.
 */
val INNER_CLASSES = typedKeyOf<MutableList<TypeDeclaration>>("INNER_CLASSES")

/**
 * The NestHost as specified in [JEP 181](https://openjdk.java.net/jeps/181).
 */
val NEST_HOST = typedKeyOf<TypeDeclaration>("NEST_HOST")

fun TypedData.findNestHost(): TypeDeclaration? {
    var data: TypedData? = this
    while (data != null) {
        if (data.containsKey(NEST_HOST))
            return NEST_HOST.require(data)
        data = data.parent
    }

    return null
}

/**
 * Stores whether this is an expression or not. Used to generate `pop` instructions after unused values.
 *
 * This is set to true when the instruction is used as argument or receiver of a method, defined as values of a variable, used as
 * operand of operator, used as return value, and so on.
 */
val IN_EXPRESSION = typedKeyOf<Int>("IN_EXPRESSION")

val SOURCE_FILE_FUNCTION = typedKeyOf<(Named) -> String>("SOURCE_FILE_FUNCTION")

val LINE = typedKeyOf<Int>("LINE_POSITION")

val TYPE_DECLARATION = typedKeyOf<TypeDeclaration>("TYPE_DECLARATION")

val METHOD_DECLARATIONS = typedKeyOf<MutableList<MethodDeclarationBase>>("METHOD_DECLARATIONS")

val LOCATION = typedKeyOf<InnerTypesHolder>("INNER_LOCATION")

val CLASS_VISITOR = typedKeyOf<ClassVisitor>("CLASS_VISITOR")

val FLOWS = typedKeyOf<MutableList<Flow>>("CODE_FLOWS")

// MVData
val METHOD_VISITOR = typedKeyOf<MethodVisitorHelper>("METHOD_VISITOR_DATA")

val C_LINE = typedKeyOf<MutableList<CLine>>("CURRENT_LINE")

data class CLine(val line: Int, val label: Label)

// List<BytecodeClass>
val BYTECODE_CLASS_LIST = typedKeyOf<MutableList<com.koresframework.kores.bytecode.BytecodeClass>>("BYTECODE_CLASS_LIST")

val CLASS_VERSION = typedKeyOf<Int>("CLASS_VERSION")

val INDIFY_STRING_CONCATENATION = typedKeyOf<Boolean>("INDIFY_STRING_CONCATENATION")

fun TypedData.findClassVersion(): Int {
    var data: TypedData? = this
    while (data != null) {
        if (data.containsKey(CLASS_VERSION))
            return CLASS_VERSION.require(data)

        data = data.parent
    }

    return VERSION
}

fun TypedData.indifyEnabled(): Boolean {
    var data: TypedData? = this
    while (data != null) {
        if (data.containsKey(INDIFY_STRING_CONCATENATION))
            return INDIFY_STRING_CONCATENATION.require(data)

        data = data.parent
    }

    return false
}


val OUTER_DESC = typedKeyOf<String>("OUTER_METHOD_DESC")

val ANNOTATION_VISITOR_CAPABLE = typedKeyOf<AnnotationVisitorCapable>("ANNOTATION_VISITOR_CAPABLE")

val IN_INVOKE_DYNAMIC = typedKeyOf<Unit>("IN_INVOKE_DYNAMIC")

val TYPES = typedKeyOf<MutableList<TypeDeclaration>>("TYPES_VISIT")

/**
 * Keep track of outer fields added to inner class.
 */
val OUTER_TYPE_FIELD = typedKeyOf<OuterClassField>("OUTER_TYPE_FIELD")

data class LineBuf(val visited: Boolean, val line: Int)

val TRY_BLOCK_DATA = typedKeyOf<MutableList<TryBlockData>>("TRY_BLOCK_DATAS")

class TryBlockData(val startLabel: Label, val stm: TryStatementBase) : Timed {
    override val creationInstant: Instant = Instant.now()
    // List of labels of where finally was generated
    val labels: MutableList<FLabel> = mutableListOf()

    fun canGen(): Boolean = this.stm.finallyStatement.isNotEmpty

    fun visit(manager: ProcessorManager<*>, data: TypedData) {

        if (this.stm.finallyStatement.isNotEmpty) {

            val start = Label()
            val end = Label()

            METHOD_VISITOR.require(data).methodVisitor.visitLabel(start)

            manager.process(Instructions::class.java, this.stm.finallyStatement, data)

            METHOD_VISITOR.require(data).methodVisitor.visitLabel(end)

            this.labels.add(FLabel(start, end))
        }
    }

}

data class FLabel(val start: Label, val end: Label)

data class OuterClassField(val typeDeclaration: TypeDeclaration, val field: FieldRef)

val MEMBER_ACCESSES = typedKeyOf<MutableList<MemberAccess>>("MEMBER_ACCESSES")

data class MemberAccess(
    val from: TypeDeclaration,
    val member: KoresElement,
    val owner: TypeDeclaration,
    val newElementToAccess: MethodDeclarationBase
) {

    fun createInvokeToNewElement(target: Instruction, args: List<Instruction>) = invoke(
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
    data.set(this.key, this.require(data) + 1, this.type)
}

fun TypedKey<Int>.decrement(data: TypedData) {
    data.set(this.key, this.require(data) - 1, this.type)
}

inline fun TypedKey<Int>.incrementInContext(data: TypedData, context: () -> Unit) {
    this.increment(data)
    context()
    this.decrement(data)
}
