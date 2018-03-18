/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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
package com.github.jonathanxd.kores.bytecode.processor.processors

import com.github.jonathanxd.kores.base.ArgumentsHolder
import com.github.jonathanxd.kores.base.InvokeDynamicBase
import com.github.jonathanxd.kores.base.LocalCode
import com.github.jonathanxd.kores.bytecode.processor.IN_EXPRESSION
import com.github.jonathanxd.kores.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.kores.bytecode.util.MethodInvocationUtil
import com.github.jonathanxd.kores.processor.Processor
import com.github.jonathanxd.kores.processor.ProcessorManager
import com.github.jonathanxd.kores.type.`is`
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require
import org.objectweb.asm.Opcodes

object InvokeDynamicProcessor : Processor<InvokeDynamicBase> {

    override fun process(
        part: InvokeDynamicBase,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {

        val mvHelper = METHOD_VISITOR.require(data)

        val dynamicMethod = part.dynamicMethod

        processorManager.process(ArgumentsHolder::class.java, part.dynamicMethod, data)

        val specification = dynamicMethod

        if (part is InvokeDynamicBase.LambdaMethodRefBase) {
            if (part is InvokeDynamicBase.LambdaLocalCodeBase) {
                // Registers the local code
                processorManager.process(LocalCode::class.java, part.localCode, data)
            }
        }

        MethodInvocationUtil.visitBootstrapInvocation(
            part,
            specification,
            data,
            mvHelper.methodVisitor
        )

        if (!part.dynamicMethod.typeSpec.returnType.`is`(Void.TYPE) && IN_EXPRESSION.require(data) == 0) {
            mvHelper.methodVisitor.visitInsn(Opcodes.POP)
        }
    }


}