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
package com.koresframework.kores.bytecode.doc

import com.koresframework.kores.bytecode.GENERATE_NESTS
import com.koresframework.kores.bytecode.GENERATE_SYNTHETIC_ACCESS

/**
 * [Nest-Based Access Control](https://openjdk.java.net/jeps/181) is a [Java 11 feature](https://openjdk.java.net/projects/jdk/11/)
 * that enables access to private members in inner classes without the need of **synthetic-bridge access methods**.
 * Synthetic bridge methods was generated before because **private members** used to be only accessible from the same class
 * they are declared. However, this approach does have some drawbacks, it involves additional compilation steps that are not
 * so easy to implement (for lambdas for example) and more prone to other problems when not implemented correctly, and
 * introduces performance impact (even with inlining and performance improvements made by the JVM).
 *
 * With [JEP 181](https://openjdk.java.net/jeps/181), synthetic methods does not need to be generated, and private member
 * access is validated by the JVM at the class loading time, this reduces class file sizes and improves performance, as
 * private field access used require bridge methods as well.
 *
 * [Nest-Based Access Control](https://openjdk.java.net/jeps/181) is enabled by default if class version
 * is configured to Java 11 (55) or later. This can be changed through [GENERATE_NESTS] option.
 *
 * If you want to enforce no access to private members, you must disable both [GENERATE_NESTS] and [GENERATE_SYNTHETIC_ACCESS]
 * options. Disabling only [GENERATE_NESTS] fallbacks to old synthetic-bridge access method generation.
 *
 * ## How does it work?
 *
 * It does generate **NestHost** and **NestMembers** in ClassFile descriptors.
 *
 * Given the following class structure:
 *
 * ```
 * class A -> B, C
 * class B -> X
 * class C -> P
 * class P -> L
 * ```
 *
 * Or
 *
 * ```kotlin
 * class A {
 *   class B {
 *     class X
 *   }
 *   class C {
 *     class P {
 *       class L
 *     }
 *   }
 * }
 * ```
 *
 * The main class (i.e. root class) is the NestHost,
 * so it declares every inner class (regardless deepness) as NestMembers:
 * ```
 * class A {
 *   NestMember B
 *   NestMember X
 *   NestMember C
 *   NestMember P
 *   NestMember L
 * }
 * ```
 *
 * And every NestMember declares the main class as NestHost:
 *
 * ```
 * class B {
 *   NestHost A
 * }
 * class X {
 *   NestHost A
 * }
 * class C {
 *   NestHost A
 * }
 * class P {
 *   NestHost A
 * }
 * class L {
 *   NestHost A
 * }
 * ```
 *
 * @since 4.0.5.bytecode.5
 */
@Doc
object NestLogic