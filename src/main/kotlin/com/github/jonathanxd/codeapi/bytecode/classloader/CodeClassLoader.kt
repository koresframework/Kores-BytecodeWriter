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
package com.github.jonathanxd.codeapi.bytecode.classloader

import com.github.jonathanxd.codeapi.base.TypeDeclaration
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass

open class CodeClassLoader : ClassLoader {

    constructor() : super()
    constructor(parent: ClassLoader): super(parent)

    /**
     * Define type declaration class.
     *
     * @param typeDeclaration Type declaration.
     * @param bytes           Bytes.
     * @return Defined Class.
     */
    open fun define(typeDeclaration: TypeDeclaration, bytes: ByteArray): Class<*> {
        return super.defineClass(typeDeclaration.type, bytes, 0, bytes.size)
    }

    /**
     * Define [BytecodeClass] class.
     *
     * @param bytecodeClass Bytecode class.
     * @return Defined Class.
     */
    open fun define(bytecodeClass: BytecodeClass): Class<*> {
        val type = (bytecodeClass.declaration as? TypeDeclaration)
                ?: throw IllegalArgumentException("Non-TypeDeclaration loading is not supported yet. BytecodeClass: $bytecodeClass")

        return this.define(type, bytecodeClass.bytecode)
    }

    /**
     * Define [classes][BytecodeClass] and inner classes.
     *
     * Make sure that all elements in the `bytecodeClasses` is a inner type of first element.
     *
     * @param bytecodeClasses Bytecode class (first element) and inner classes (remaining).
     * @return First Defined Class.
     */
    open fun define(bytecodeClasses: Array<out BytecodeClass>): Class<*> {
        return this.define(bytecodeClasses.iterator())
    }

    /**
     * Define [classes][BytecodeClass] and inner classes.
     *
     * Make sure that all elements in the `bytecodeClasses` is a inner type of first element.
     *
     * @param bytecodeClasses Bytecode class (first element) and inner classes (remaining).
     * @return First Defined Class.
     */
    open fun define(bytecodeClasses: Collection<BytecodeClass>): Class<*> {
        return this.define(bytecodeClasses.iterator())
    }

    /**
     * Define [classes][BytecodeClass] and inner classes.
     *
     * Make sure that all elements in the `bytecodeClasses` is a inner type of first element.
     *
     * @param bytecodeClasses Bytecode class (first element) and inner classes (remaining).
     * @return First Defined Class.
     */
    open fun define(bytecodeClasses: Iterator<BytecodeClass>): Class<*> {
        if (!bytecodeClasses.hasNext()) {
            throw IllegalArgumentException("Empty 'bytecodeClasses' array")
        }

        val bytecodeClass = bytecodeClasses.next()

        val type = (bytecodeClass.declaration as? TypeDeclaration)
                ?: throw IllegalArgumentException("Non-TypeDeclaration loading is not supported yet. BytecodeClass: $bytecodeClass")

        val define = this.define(type, bytecodeClass.bytecode)

        bytecodeClasses.forEach {
            this.define(it)
        }

        return define
    }
}
