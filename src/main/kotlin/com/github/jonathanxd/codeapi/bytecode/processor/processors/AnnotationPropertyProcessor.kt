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

import com.github.jonathanxd.codeapi.base.Annotable
import com.github.jonathanxd.codeapi.base.AnnotationProperty
import com.github.jonathanxd.codeapi.bytecode.common.MethodVisitorHelper
import com.github.jonathanxd.codeapi.bytecode.processor.ANNOTATION_VISITOR_CAPABLE
import com.github.jonathanxd.codeapi.bytecode.processor.CLASS_VISITOR
import com.github.jonathanxd.codeapi.bytecode.util.AnnotationUtil
import com.github.jonathanxd.codeapi.bytecode.util.AnnotationVisitorCapable
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.util.typeDesc
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.jwiutils.kt.inContext
import com.github.jonathanxd.jwiutils.kt.require
import org.objectweb.asm.Opcodes

object AnnotationPropertyProcessor : Processor<AnnotationProperty> {

    override fun process(part: AnnotationProperty, data: TypedData, processorManager: ProcessorManager<*>) {
        val cw = CLASS_VISITOR.require(data)

        val asmModifiers = Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT

        val type = part.type.typeDesc
        val name = part.name
        val value = part.defaultValue

        val mv = cw.visitMethod(asmModifiers, name, "()" + type, null, null)

        if (value != null) {

            val annotationVisitor = mv.visitAnnotationDefault()

            AnnotationUtil.visitAnnotationValue(annotationVisitor, null, value)

            annotationVisitor.visitEnd()
        }

        val helper = MethodVisitorHelper(mv, mutableListOf())

        ANNOTATION_VISITOR_CAPABLE.inContext(data, AnnotationVisitorCapable.MethodVisitorCapable(helper.methodVisitor)) {
            processorManager.process(Annotable::class.java, part, data)
        }

        mv.visitEnd()

    }

}