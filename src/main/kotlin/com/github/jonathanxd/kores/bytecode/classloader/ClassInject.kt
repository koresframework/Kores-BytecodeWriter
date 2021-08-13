/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2021 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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
package com.github.jonathanxd.kores.bytecode.classloader

import com.github.jonathanxd.kores.base.TypeDeclaration
import com.github.jonathanxd.kores.bytecode.BytecodeClass

/**
 * Class injection utility.
 *
 * **This code may not work in Java 9+*
 */
object ClassInject {

    /**
     * Inject class in [this classloader][ClassLoader].
     *
     * @param bytecodeClass Bytecode class to inject
     * @return Defined class
     * @throws IllegalStateException if the injection fails.
     */
    fun ClassLoader.inject(bytecodeClass: com.github.jonathanxd.kores.bytecode.BytecodeClass): Class<*> {
        val type = (bytecodeClass.declaration as? TypeDeclaration)?.type
                ?: throw IllegalArgumentException("Non-TypeDeclaration loading is not supported yet. BytecodeClass: $bytecodeClass")

        return this.inject(type, bytecodeClass.bytecode)
    }

    /**
     * Inject class and inner classes of this class in [this classloader][ClassLoader].
     *
     * @param bytecodeClasses Bytecode classes to inject (first element must be the outer class).
     * @return Defined class.
     * @throws IllegalStateException if the injection fails or [bytecode classes array][bytecodeClasses] is empty.
     */
    fun ClassLoader.inject(bytecodeClasses: Array<out com.github.jonathanxd.kores.bytecode.BytecodeClass>): Class<*> {
        if (bytecodeClasses.isEmpty()) {
            throw IllegalArgumentException("Empty 'bytecodeClasses' array")
        }

        val bytecodeClass = bytecodeClasses[0]

        val define = this.inject(bytecodeClass)

        for (i in 1..bytecodeClasses.size - 1) {
            this.inject(bytecodeClasses[i])
        }

        return define
    }


    /**
     * Inject class in [this classloader][ClassLoader].
     *
     * @param name Class name
     * @param bytes Class bytes
     * @return Defined class
     * @throws IllegalStateException if the injection fails.
     */
    fun ClassLoader.inject(name: String, bytes: ByteArray): Class<*> {
        try {
            val defineClass = ClassLoader::class.java.getDeclaredMethod(
                "defineClass",
                String::class.java,
                ByteArray::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )

            defineClass.isAccessible = true

            return defineClass.invoke(this, name, bytes, 0, bytes.size) as Class<*>
        } catch (e: Exception) {
            throw IllegalStateException(
                "Injection of class '$name' in class loader '$this' failed!",
                e
            )
        }

    }

}