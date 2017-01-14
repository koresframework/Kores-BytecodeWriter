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
package com.github.jonathanxd.codeapi.bytecode.gen.visitor

import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.util.AnnotationUtil
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.base.Annotable
import com.github.jonathanxd.codeapi.base.AnnotationProperty
import com.github.jonathanxd.codeapi.util.CodeTypeUtil
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object AnnotationPropertyVisitor : VoidVisitor<AnnotationProperty, BytecodeClass, Any?> {

    override fun voidVisit(t: AnnotationProperty, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
        val cw = Util.find(TypeVisitor.CLASS_WRITER_REPRESENTATION, extraData, additional)

        val asmModifiers = Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT

        val type = CodeTypeUtil.codeTypeToFullAsm(t.type)
        val name = t.name
        val value = t.value

        val mv = cw.visitMethod(asmModifiers, name, "()" + type, null, null)

        if (value != null) {

            val annotationVisitor = mv.visitAnnotationDefault()

            AnnotationUtil.visitAnnotationValue(annotationVisitor, null, value)

            annotationVisitor.visitEnd()
        }

        val mvData = MVData(mv, mutableListOf())

        visitorGenerator.generateTo(Annotable::class.java, t, extraData, null, mvData)

        mv.visitEnd()
    }

}