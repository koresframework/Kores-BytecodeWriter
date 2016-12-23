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

import com.github.jonathanxd.codeapi.bytecode.util.asm.ParameterVisitor
import org.objectweb.asm.*

/**
 * Internal class. Universalise asm annotation visitors.
 */
interface AnnotationVisitorCapable {
    fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor


    fun visitTypeAnnotation(typeRef: Int,
                            typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor

    fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor

    class ClassWriterVisitorCapable(private val classWriter: ClassWriter) : AnnotationVisitorCapable {

        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
            return this.classWriter.visitAnnotation(desc, visible)
        }

        override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor {
            return this.classWriter.visitTypeAnnotation(typeRef, typePath, desc, visible)
        }

        override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor {
            throw UnsupportedOperationException("Classes doesn't have parameter annotations!")
        }
    }

    class MethodVisitorCapable(private val methodVisitor: MethodVisitor) : AnnotationVisitorCapable {

        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
            return this.methodVisitor.visitAnnotation(desc, visible)

        }

        override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor {
            return this.methodVisitor.visitTypeAnnotation(typeRef, typePath, desc, visible)
        }

        override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor {
            return this.methodVisitor.visitParameterAnnotation(parameter, desc, visible)
        }
    }

    class FieldVisitorCapable(private val fieldVisitor: FieldVisitor) : AnnotationVisitorCapable {

        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
            return this.fieldVisitor.visitAnnotation(desc, visible)

        }

        override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor {
            return this.fieldVisitor.visitTypeAnnotation(typeRef, typePath, desc, visible)
        }

        override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor {
            throw UnsupportedOperationException("Fields doesn't have parameter annotations!")
        }
    }

    class ParameterVisitorCapable(private val parameterVisitor: ParameterVisitor) : AnnotationVisitorCapable {

        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
            return this.parameterVisitor.visitAnnotation(desc, visible)

        }

        override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor {
            throw UnsupportedOperationException("Parameters doesn't have type annotations!")
        }

        override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor {
            throw UnsupportedOperationException("Parameters doesn't have parameter annotations!")
        }
    }
}
