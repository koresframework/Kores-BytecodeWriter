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
 * Java 9 introduced [JEP 280](https://openjdk.java.net/jeps/280), which is a JDK Enhancement Proposal around
 * building the ground to **String Concatenation Optimization** without changing the Java-to-bytecode compiler
 * or introducing new instructions to the bytecode.
 *
 * ## Why?
 *
 * #### By my understanding reading the JEP and based on my knowledge about the Java Ecosystem:
 *
 * With this change, JIT, optimizers and post-processors in general (this includes bytecode post-processors like Proguard)
 * could easily detect string concatenation through `invokedynamic`
 * and apply their optimizations strategies, as well as implement new optimizations that would easily apply to concatenation.
 *
 * Even though it is not hard to detect [StringBuilder] based string concatenation, it needs more steps to detect them, and
 * compilers other than the **javac** itself, like **Groovy Compiler**, **Scala Compiler**, **Kotlin Compiler**, and
 * a bunch of other compilers, could generate a set of instructions that looks like a simple concatenation but instructions
 * added in the middle can mess with the **concatenation detection** and prevent the optimizations to take place.
 *
 * This is not only a **Java Optimization**, this optimization applies to all current and future languages that compiles
 * to Bytecode, yes, they need to implement **Concatenation Indify** to take advantage of future optimizations made around
 * this mechanism, and current optimizations around [StringBuilder] may still be applied for various years from this new
 * approach, until the JVM team consider it does not worth keeping it anymore.
 *
 * Also, its is important to remember that Java and the JVM updates tries to improve the performance not only of new code,
 * but older code as well, without the need to recompile the older code. So, optimizations applied to JVM implementations
 * are always better than optimizations made by compilers and to compilers, because compiler optimizations only affects newer code.
 *
 * This feature changes the emitted bytecode for **String Concatenation**, so only new code may take advantage (but
 * the JVM could easily convert [StringBuilder] based concatenation to `invokedynamic` in special cases,
 * but I don't know if it does).
 * Future improvements made to **String Concatenation** will affect both new and old code (if they use `invokedynamic`)
 * without the need to recompile the classes.
 *
 * ## Kores-BytecodeWriter
 *
 * Kores-BytecodeWriter tries to follow closely the **javac** path, to always produce bytecode as performant as **javac**
 * produced ones. And most of JVM implementations are **javac** oriented, their optimizations easily recognizes
 * Bytecode produced by **javac** and have less trouble in optimizing them, so Kores produces likely the same
 * bytecode as **javac** would produce for **String Concatenation**.
 *
 * ## How
 *
 * Given the following concatenation scenario:
 *
 * ```kotlin
 * fun concat(a: String, b: String, c: String): String {
 *     return a + b + c
 * }
 * ```
 *
 * Kores produces the following `invokedynamic` instruction:
 *
 * ```
 * invokedynamic makeConcatWithConstants(java.lang.String, int, double, java.lang.Object[])java.lang.String [
 *     // Bootstrap method
 *     java.lang.invoke.StringConcatFactory.makeConcatWithConstants(java.lang.invoke.MethodHandles$Lookup, java.lang.String, java.lang.invoke.MethodType, java.lang.String, java.lang.Object[])java.lang.invoke.CallSite (tag: h_invokestatic, itf: false) [
 *       // Arguments
 *       "\u0001\u0001\u0001"
 *     ]
 * ]
 * ```
 *
 * @since 4.0.5.bytecode.5
 */
@Doc
object IndyConcatLogic