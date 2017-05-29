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

import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.builder.build
import com.github.jonathanxd.codeapi.bytecode.processor.LOCATION
import com.github.jonathanxd.codeapi.bytecode.processor.inContext
import com.github.jonathanxd.codeapi.factory.constructorDec
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.iutils.data.TypedData

object ElementsHolderProcessor : Processor<ElementsHolder> {

    override fun process(part: ElementsHolder, data: TypedData, codeProcessor: CodeProcessor<*>) {

        part.fields.forEach {
            it.visitHolder(data, codeProcessor)
        }

        part.constructors.forEach {
            it.visitHolder(data, codeProcessor)
        }

        part.methods.forEach {
            it.visitHolder(data, codeProcessor)
        }

        if(part is TypeDeclaration && !part.isInterface && part.constructors.isEmpty()) {
            val defaultConstructor = constructorDec().build {
                this.modifiers += CodeModifier.PUBLIC
            }

            codeProcessor.process(ConstructorDeclaration::class.java, defaultConstructor, data)
        }

        part.staticBlock.visitHolder(data, codeProcessor)

        codeProcessor.process(InnerTypesHolder::class.java, part, data)
    }


    inline fun <reified T : InnerTypesHolder> T.visitHolder(data: TypedData, codeProcessor: CodeProcessor<*>) {
        codeProcessor.process(InnerTypesHolder::class.java, this, data)
        codeProcessor.process(T::class.java, this, data)
    }

}


object InnerTypesHolderProcessor : Processor<InnerTypesHolder> {
    override fun process(part: InnerTypesHolder, data: TypedData, codeProcessor: CodeProcessor<*>) {
        LOCATION.inContext(data, part) {
            part.innerTypes.forEach {
                codeProcessor.process(it::class.java, it, data)
            }
        }
    }
}