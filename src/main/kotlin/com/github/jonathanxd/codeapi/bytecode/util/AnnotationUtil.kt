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
package com.github.jonathanxd.codeapi.bytecode.util

import com.github.jonathanxd.codeapi.interfaces.Annotation
import com.github.jonathanxd.codeapi.interfaces.EnumValue
import com.github.jonathanxd.codeapi.types.CodeType
import com.github.jonathanxd.codeapi.util.ArrayUtility
import org.objectweb.asm.Type

object AnnotationUtil {
    fun visitAnnotation(annotation: Annotation, annotationVisitorCapable: AnnotationVisitorCapable) {
        val annotationTypeAsm = CodeTypeUtil.codeTypeToFullAsm(annotation.type.orElseThrow(::NullPointerException))
        val annotationVisitor = annotationVisitorCapable.visitAnnotation(annotationTypeAsm, annotation.isVisible)

        val values = annotation.values

        for ((key, value) in values) {
            AnnotationUtil.visitAnnotationValue(annotationVisitor, key, value)
        }

        annotationVisitor.visitEnd()
    }

    fun visitAnnotationValue(annotationVisitor: org.objectweb.asm.AnnotationVisitor, key: String?, value: Any) {
        var value = value

        if (value.javaClass.isArray) {
            val values = ArrayUtility.toObjectArray(value)

            val annotationVisitor1 = annotationVisitor.visitArray(key)

            for (o in values) {
                AnnotationUtil.visitAnnotationValue(annotationVisitor1, "", o)
            }

            annotationVisitor1.visitEnd()

            return
        }

        if (value is EnumValue) {
            annotationVisitor.visitEnum(value.name, CodeTypeUtil.codeTypeToFullAsm(value.enumType), value.enumValue)

            return
        }

        if (value is Annotation) {
            val asmType = CodeTypeUtil.codeTypeToFullAsm(value.type.orElseThrow(::NullPointerException))

            val visitor2 = annotationVisitor.visitAnnotation(key, asmType)

            for ((key1, value1) in value.values) {
                AnnotationUtil.visitAnnotationValue(visitor2, key1, value1)
            }

            visitor2.visitEnd()
        }

        if (value is CodeType) {
            value = Type.getType(CodeTypeUtil.codeTypeToFullAsm(value))
        }

        annotationVisitor.visit(key, value)
    }
}