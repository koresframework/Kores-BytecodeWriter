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

import com.github.jonathanxd.codeapi.base.InnerTypesHolder
import com.github.jonathanxd.codeapi.base.LocalCode
import com.github.jonathanxd.codeapi.base.TypeDeclaration
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.Flow
import com.github.jonathanxd.codeapi.bytecode.common.MethodVisitorHelper
import com.github.jonathanxd.codeapi.bytecode.util.AnnotationVisitorCapable
import com.github.jonathanxd.codeapi.common.FieldRef
import com.github.jonathanxd.codeapi.util.typedKeyOf
import com.github.jonathanxd.iutils.`object`.TypedKey
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.map.ListHashMap
import org.objectweb.asm.ClassVisitor

/** Version of Java class to generate
 *  CodeAPI always generates latest version
 * This can be changed via [version][CLASS_VERSION] key
 */
const val VERSION = 52

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
val OUTER_TYPES_FIELDS = typedKeyOf<MutableList<OuterClassFields>>("OUTER_TYPE_FIELDS")

data class OuterClassFields(val typeDeclaration: TypeDeclaration, val fields: List<FieldRef>)

//val LOCAL_CODES = typedKeyOf<MutableList<LocalCode>>("LOCAL_CODES")

