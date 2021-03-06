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

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.ForEachStatement
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.util.MVDataSugarEnvironment
import com.github.jonathanxd.codeapi.bytecode.util.ObjectCache
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.gen.visit.SugarSyntaxVisitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.sugar.SugarEnvironment
import com.github.jonathanxd.iutils.data.MapData

/**
 * This visitor requires a [SugarSyntaxVisitor.ENVIRONMENT] data.
 */
object ForEachVisitor : VoidVisitor<ForEachStatement, BytecodeClass, MVData> {

    override fun voidVisit(t: ForEachStatement, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {

        val env = extraData.getOptional<SugarEnvironment>(SugarSyntaxVisitor.ENVIRONMENT).orElse(MVDataSugarEnvironment(additional))

        val iterationType = t.iterationType
        val start = iterationType.createGenerator(env)

        val generated = start.generate(t, this)
        additional.enterNewFrame()
        visitorGenerator.generateTo(CodeSource::class.java, generated, extraData, null, additional)
        additional.exitFrame()
    }

}