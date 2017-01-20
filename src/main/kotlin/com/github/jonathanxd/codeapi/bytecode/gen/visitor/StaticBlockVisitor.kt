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

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.base.FieldDeclaration
import com.github.jonathanxd.codeapi.base.StaticBlock
import com.github.jonathanxd.codeapi.base.TypeDeclaration
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

object StaticBlockVisitor : VoidVisitor<StaticBlock, BytecodeClass, Any?> {

    // StaticBlock
    val STATIC_BLOCKS = "STATIC_BLOCK"

    fun generate(extraData: Data, visitorGenerator: VisitorGenerator<*>, cw: ClassVisitor, typeDeclaration: TypeDeclaration) {

        val mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)

        val mvData = MVData(mv, mutableListOf())

        mv.visitCode()

        // Variable Initialize

        val all = CodeSourceUtil.find<FieldDeclaration>(
                typeDeclaration.body,
                { codePart ->
                    codePart is FieldDeclaration
                            && codePart.modifiers.contains(CodeModifier.STATIC)
                            && codePart.value != null
                }
        ) { codePart -> codePart as FieldDeclaration }

        val body = MutableCodeSource()

        for (fieldDeclaration in all) {

            val value = fieldDeclaration.value

            if (value != null) {

                val def = CodeAPI.setStaticField(typeDeclaration, fieldDeclaration.type, fieldDeclaration.name, fieldDeclaration.value)

                body.add(def)
            }
        }


        if (body.isNotEmpty)
            visitorGenerator.generateTo(CodeSource::class.java, body, extraData, mvData)

        // Static Blocks

        val staticBlocks = extraData.getAllAsList<StaticBlock>(STATIC_BLOCKS)

        for (staticBlock in staticBlocks) {
            visitorGenerator.generateTo(CodeSource::class.java, staticBlock.body, extraData, mvData)
        }


        mv.visitInsn(Opcodes.RETURN)
        try {
            mv.visitMaxs(0, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mv.visitEnd()
    }

    override fun voidVisit(t: StaticBlock, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
        extraData.registerData(STATIC_BLOCKS, t)
    }

}