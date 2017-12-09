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

import com.github.jonathanxd.codeapi.base.ModuleDeclaration
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.processor.BYTECODE_CLASS_LIST
import com.github.jonathanxd.codeapi.bytecode.processor.SOURCE_FILE_FUNCTION
import com.github.jonathanxd.codeapi.bytecode.util.ModifierUtil
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.util.internalName
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.jwiutils.kt.add
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

object ModuleDeclarationProcessor : Processor<ModuleDeclaration> {
    override fun process(part: ModuleDeclaration, data: TypedData, processorManager: ProcessorManager<*>) {
        val cw = ClassWriter(0)

        cw.visit(Opcodes.V9, Opcodes.ACC_MODULE, "module-info", null, null, null)

        SOURCE_FILE_FUNCTION.getOrNull(data)?.invoke(part)?.let {
            cw.visitSource(it, null)
        }


        val moduleVisitor = cw.visitModule(
                part.name,
                ModifierUtil.toAsmAccess(part.modifiers),
                part.version
        )

        // TODO: Review version. Maybe 9-ea currently
        moduleVisitor.visitRequire("java.base", Opcodes.ACC_MANDATED, "9") // TODO: Review version

        part.requires.filterNot { it.module.name == "java.base" }.forEach {
            moduleVisitor.visitRequire(
                    it.module.name,
                    ModifierUtil.toAsmAccess(it.modifiers),
                    it.version
            )
        }

        part.exports.forEach {
            val modules =
                    it.to.map { it.name.internal }.toTypedArray()

            moduleVisitor.visitExport(
                    it.module.name.internal,
                    ModifierUtil.toAsmAccess(it.modifiers),
                    *modules
            )
        }

        part.opens.forEach {
            val modules =
                    it.to.map { it.name.internal }.toTypedArray()

            moduleVisitor.visitOpen(
                    it.module.name.internal,
                    ModifierUtil.toAsmAccess(it.modifiers),
                    *modules
            )
        }

        part.uses.forEach {
            moduleVisitor.visitUse(it.name.internal)
        }

        part.provides.forEach {
            moduleVisitor.visitProvide(it.service.internalName, *it.with.map { it.internalName }.toTypedArray())
        }

        moduleVisitor.visitEnd()
        cw.visitEnd()

        BYTECODE_CLASS_LIST.add(data, BytecodeClass(part, cw.toByteArray()))
    }
}