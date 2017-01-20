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

import com.github.jonathanxd.codeapi.base.MethodDeclaration
import com.github.jonathanxd.codeapi.base.MethodFragment
import com.github.jonathanxd.codeapi.base.MethodInvocation
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor

object MethodFragmentVisitor : VoidVisitor<MethodFragment, BytecodeClass, Any?> {

    // MethodFragment
    @JvmStatic
    val FRAGMENT_TYPE_INFO = "METHOD_FRAGMENT"

    @JvmStatic
    fun <T : Any> visitFragmentsGeneration(visitorGenerator: VisitorGenerator<T>, extraData: Data) {
        val all = extraData.getAllAsList<MethodFragment>(MethodFragmentVisitor.FRAGMENT_TYPE_INFO)

        if (!all.isEmpty()) {
            for (methodFragment in all) {
                visitorGenerator.generateTo(MethodFragment::class.java, methodFragment, extraData, null, null)
            }
        }
    }

    @JvmStatic
    fun newFragment(t: MethodFragment, extraData: Data): MethodFragment =
            Util.getNewName("fragment$$", t.declaration, extraData).let { name ->
                t.builder().withSpec(t.spec.builder().withName(name).build()).withDeclaration(t.declaration.builder().withName(name).build()).build()
            }

    override fun voidVisit(t: MethodFragment, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
        if (additional != null && additional is MVData) {

            val fragment = newFragment(t, extraData)

            /*val declaration = t.declaration.builder().withName().build()
            t.builder().withDeclaration()*/

            /*extraData.registerData(MethodFragmentVisitor.FRAGMENT_TYPE_INFO, t)
            visitorGenerator.generateTo(MethodInvocation::class.java, t, extraData, null, additional)*/

            extraData.registerData(MethodFragmentVisitor.FRAGMENT_TYPE_INFO, fragment)
            visitorGenerator.generateTo(MethodInvocation::class.java, fragment, extraData, null, additional)
        } else {
            visitorGenerator.generateTo(MethodDeclaration::class.java, t.declaration, extraData, null, null)
        }
    }

}