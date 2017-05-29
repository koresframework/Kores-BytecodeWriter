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
import com.github.jonathanxd.codeapi.base.FieldDeclaration
import com.github.jonathanxd.codeapi.bytecode.processor.ANNOTATION_VISITOR_CAPABLE
import com.github.jonathanxd.codeapi.bytecode.processor.CLASS_VISITOR
import com.github.jonathanxd.codeapi.bytecode.util.AnnotationVisitorCapable
import com.github.jonathanxd.codeapi.bytecode.util.ModifierUtil
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.type.GenericType
import com.github.jonathanxd.codeapi.util.genericTypeToDescriptor
import com.github.jonathanxd.codeapi.util.typeDesc
import com.github.jonathanxd.iutils.data.TypedData

object FieldDeclarationProcessor : Processor<FieldDeclaration> {

    override fun process(part: FieldDeclaration, data: TypedData, codeProcessor: CodeProcessor<*>) {
        val visitor = CLASS_VISITOR.getOrNull(data)!!

        val access = ModifierUtil.modifiersToAsm(part.modifiers)
        val signature = (part.type as? GenericType)?.genericTypeToDescriptor()

        visitor.visitField(access, part.name, part.type.typeDesc, signature, null).let {
            ANNOTATION_VISITOR_CAPABLE.set(data, AnnotationVisitorCapable.FieldVisitorCapable(it))
            codeProcessor.process(Annotable::class.java, part, data)
            ANNOTATION_VISITOR_CAPABLE.remove(data)
            it.visitEnd()
        }
    }

}