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

import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.CallSite
import java.lang.invoke.StringConcatFactory

/**
 * Born by the [Da Vinci Machine Project](https://openjdk.java.net/projects/mlvm/) with the
 * [JSR 292](https://jcp.org/en/jsr/detail?id=292) specification, `invokedynamic` is a special kind of invocation
 * that uses a **bootstrap method** to link to **static methods**, improving JVM performance and support to
 * languages other than Java, mainly dynamic ones. And Java 11 brought improvements with the
 * [JEP 309](https://openjdk.java.net/jeps/309), that introduced **dynamic constants** which are resolved using a **bootstrap method**.
 *
 * `invokedynamic` is a kind of invocation that uses a **bootstrap method** to resolve the target method to link to,
 * once linked, all subsequent invocations are **directly** dispatched to the target method without calling the **boostrap
 * method** again. This allows dynamic method resolution without sacrificing JIT optimizations as well as other optimizations,
 * like method inlining.
 *
 * It is very likely to **lazy evaluation**, with the biggest advantage as being candidate to all optimizations that
 * would not be possible with runtime method resolution, like Groovy and other dynamic and static JVM languages used to do.
 *
 * Even though `invokedynamic` mainly purpose was to have a more language-agnostic JVM, as JVM has been a very mature
 * inviting ecosystem to write your own language or runtime of other languages like Python (with [Jython](https://github.com/jython/jython)),
 * Java Language itself is taking advantage of this opcode for various features. For example, **Lambda** and **Method references**
 * used to be implemented using **Anonymous Abstract Classes** in its early stage, before the integration of `invokedynamic`
 * into the JVM, after the implementation of the specification, Lambda and Method references started using the instruction
 * to generate the implementation at runtime and to link to the target method (through [LambdaMetafactory]),
 * performance-wise, the average is very close to anonymous classes performance.
 * Also, Java started to use the instruction to generate [string concatenation][IndyConcatLogic] through [StringConcatFactory],
 * which is more
 * easy to detect and optimize and to generate record `toString`, `equals` and `hashCode` methods implementation,
 * using the [ObjectMethods](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/runtime/ObjectMethods.html)
 * ([ref][java.lang.runtime.ObjectMethods]).
 *
 * ## How does Kores treats `invokedynamic`
 *
 * Kores uses `invokedynamic` to generate lambdas, method references and string concatenations (Kores currently does not support
 * record types), custom InvokeDynamic instructions are supported and are generated following the JVM Specification.
 *
 * InvokeDynamic is a very powerful tool for both programming languages and bytecode generation libraries, as it allows functions to be
 * resolved at runtime without sacrificing de average performance (first invocation is slower because of the initial resolution).
 *
 * ## How does `invokedynamic` works?
 *
 * When the JVM finds an `invokedynamic` instruction, it resolves the **bootstrap method** (which is statically resolved)
 * and invokes the resolved bootstrap method with additional information as well as with the boostrap arguments constants,
 * then the **bootstrap method** resolves the dynamic method and returns a [CallSite] that is linked to a method to invoke.
 * This [CallSite] holds the resolved method with its static information (localization, name, parameter and return types)
 * and other additional information that specifies how to invoke the method, like how to take the arguments and the instance
 * to used to invoke the method (if the method is not static nor a constructor). The [CallSite] object is linked to the instruction for future invocations.
 * After all resolution steps, the method itself is invoked with the arguments. From this moment, all invocations will simply skip the **bootstrap
 * resolution step** and invoke the method directly, and, from now, JIT sees this `invokedynamic` opcode just as a regular method
 * invocation, which corresponds to the resolved method invocation.
 *
 * **bootstrap method** does not have access to arguments that are passed to the resolved method, this is because **bootstrap methods**
 * behavior must never change based on arguments, the bootstrap is called once for the `invokedynamic` instruction, having
 * access to arguments will give the method access only to arguments provided at the resolution time, but not for subsequent invocations.
 *
 * @since 4.0.5.bytecode.5
 */
@Doc
object IndyGenerationLogic