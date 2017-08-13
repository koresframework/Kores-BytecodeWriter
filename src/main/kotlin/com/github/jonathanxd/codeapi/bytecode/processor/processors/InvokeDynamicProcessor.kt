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

import com.github.jonathanxd.codeapi.base.InvokeDynamicBase
import com.github.jonathanxd.codeapi.base.LocalCode
import com.github.jonathanxd.codeapi.base.MethodInvocation
import com.github.jonathanxd.codeapi.bytecode.processor.IN_INVOKE_DYNAMIC
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.util.MethodInvocationUtil
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.util.require
import com.github.jonathanxd.iutils.data.TypedData
import java.lang.reflect.Type

object InvokeDynamicProcessor : Processor<InvokeDynamicBase> {

    override fun process(part: InvokeDynamicBase, data: TypedData, processorManager: ProcessorManager<*>) {

        val mvHelper = METHOD_VISITOR.require(data)

        val invocation = part.invocation

        val localization: Type = Util.resolveType(invocation.localization, data)

        IN_INVOKE_DYNAMIC.set(data, Unit, true)

        processorManager.process(MethodInvocation::class.java, invocation, data)

        val specification = invocation.spec

        // Generate lambda 'invokeDynamic'
        if (part is InvokeDynamicBase.LambdaMethodRefBase) {

            val spec = if (part is InvokeDynamicBase.LambdaLocalCodeBase) {
                // Register fragment to gen
                processorManager.process(LocalCode::class.java, part.localCode, data)

                specification.builder().withMethodName(part.localCode.declaration.name).build()
            } else specification


            MethodInvocationUtil.visitLambdaInvocation(part, part.invocation.invokeType, localization, spec, mvHelper.methodVisitor)

        } else {
            // Generate bootstrap 'invokeDynamic'
            // Visit bootstrap invoke dynamic
            MethodInvocationUtil.visitBootstrapInvocation(part, specification, data, mvHelper.methodVisitor)
        }

    }


}