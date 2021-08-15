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
@file:JvmName("BytecodeOptions")

package com.koresframework.kores.bytecode

import com.koresframework.kores.base.Line
import com.koresframework.kores.bytecode.post.DeadCodeRemover
import com.koresframework.kores.bytecode.post.GotoOptimizer
import com.koresframework.kores.bytecode.post.MethodProcessor
import com.koresframework.kores.bytecode.pre.GenLineVisitor
import com.github.jonathanxd.iutils.option.Option
import com.github.jonathanxd.iutils.option.Options
import com.koresframework.kores.bytecode.doc.NestLogic
import com.koresframework.kores.bytecode.doc.IndyConcatLogic

/**
 * Calls [org.objectweb.asm.util.CheckClassAdapter] to check generated class.
 *
 * The Adapter is called after the visit ends because the CodeAPI let asm to calculate locals and
 * stack.
 */
@JvmField
val CHECK = Option(true)

/**
 * Enables post-processing tasks:
 *
 * - Dead Code removal
 * - Goto to goto removal.
 */
@JvmField
val POST_PROCESSING = Option(true)

/**
 * How many times post-processors should run.
 */
@JvmField
val POST_PROCESSING_LOOPS = Option(1)

/**
 * List of method post-processors
 */
@JvmField
val POST_PROCESSORS = Option<List<MethodProcessor>>(listOf(DeadCodeRemover, GotoOptimizer))

/**
 * Calls MethodVisitor.visitLine for each expression
 */
@JvmField
val VISIT_LINES = Option(VisitLineType.DISABLED)

/**
 * Validate constructor this() invocation.
 *
 * Default: true
 */
@JvmField
val VALIDATE_THIS = Option(true)

/**
 * Validate constructor super() invocation.
 *
 * Default: true
 */
@JvmField
val VALIDATE_SUPER = Option(true)

/**
 * Automatically generate bridge methods.
 *
 * The generation of bridge methods will slow down the Generator, if you mind the performance
 * don't change this option.
 *
 * The generation of bridge methods is very limited, the [Bridge Generator][com.koresframework.kores.bytecode.util.BridgeUtil]
 * will inspect super-classes (and super-interfaces) and find the overridden method.
 * The [Bridge Generator][com.koresframework.kores.bytecode.util.BridgeUtil] will only inspect
 * [Java type][Class] and [Kores Type][com.koresframework.kores.base.TypeDeclaration].
 * If the super-class and/or super-interfaces is of another type, [Bridge Generator][com.koresframework.kores.bytecode.util.BridgeUtil]
 * will ignore and the bridge method will not be generated.
 *
 * You could also bridge methods manually with [com.koresframework.kores.factory.bridgeMethod].
 */
@JvmField
val GENERATE_BRIDGE_METHODS = Option(false)

/**
 * Automatically generate synthetic accessors for private members in private inner classes.
 * Synthetic accessors will be only generated for private members which are accessed.
 *
 * An anonymous synthetic class may be generated for private constructors.
 */
@JvmField
val GENERATE_SYNTHETIC_ACCESS = Option(true)

/**
 * Automatically generate synthetic accessors for private members in private inner classes.
 * Synthetic accessors will be only generated for private members which are accessed.
 *
 * An anonymous synthetic class may be generated for private constructors.
 *
 * @since 4.0.5.bytecode.5
 */
@JvmField
val FORCE_GENERATE_SYNTHETIC_ACCESS = Option(false)

/**
 * When enabled, generate nests declarations as specified in [JEP 181](https://openjdk.java.net/jeps/181),
 * enabling this automatically disables [GENERATE_SYNTHETIC_ACCESS].
 *
 * To have both enabled, use [FORCE_GENERATE_SYNTHETIC_ACCESS]. This is not recommended since
 * [Nest-Based Access Control](https://openjdk.java.net/jeps/181) introduces JVM-Level access control
 * and removes the need of having bridge methods to access inner classes private members.
 *
 * @since 4.0.5.bytecode.5
 */
@JvmField
val GENERATE_NESTS = Option(true)

/**
 * Force generate [Nests](https://openjdk.java.net/jeps/181) even when emitting bytecode for Java 10 (54) or earlier.
 *
 * Note that forcing to generate Nests may result in two scenarios:
 *
 * - Generate Nests info but invoke with **synthetic accessors** when [FORCE_GENERATE_SYNTHETIC_ACCESS] is set.
 * - Generate Nests info and access members directly. JVM may not accept this kind of access when running under Java 10 or earlier.
 *
 * Read more at [GENERATE_NESTS].
 */
@JvmField
val FORCE_GENERATE_NESTS = Option(true)

/**
 * Uses `invokedynamic` instruction for string concatenation instead of [StringBuilder].
 *
 * This option is automatically disabled when emitting bytecode for Java 8 or earlier, to force use [FORCE_INDIFY_STRING_CONCAT].
 *
 * Read more at [IndyConcatLogic]
 */
@JvmField
val INDIFY_STRING_CONCAT = Option(true)

/**
 * Force the use of `invokedynamic` instruction for string concatenation instead of [StringBuilder].
 *
 * This applies even if the target bytecode version is lower or equal to 52 (Java 8), so the concatenation will
 * only work when running under Java 9 or newer.
 *
 * Read [INDIFY_STRING_CONCAT] for more information.
 */
@JvmField
val FORCE_INDIFY_STRING_CONCAT = Option(false)

/**
 * Specifies the strategy to use to generate `invokedynamic` string concatenation, as specified in
 * [java.lang.invoke.StringConcatFactory.makeConcatWithConstants].
 *
 * Read more at [IndyConcatLogic].
 */
@JvmField
val INDY_CONCAT_STRATEGY = Option(IndyConcatStrategy.INTERPOLATE)

enum class IndyConcatStrategy {
    /**
     * Indify using interpolation, the produced `invokedynamic` instruction interpolates the constants in the
     * [recipe][java.lang.invoke.StringConcatFactory.makeConcatWithConstants] argument.
     *
     * For example, the following concatenation scenario:
     *
     * ```kotlin
     * fun concat(a: String, b: String) =
     *   a + "<:>" + b
     * ```
     *
     * Is translated into the following recipe:
     * ```
     * aload 0 // Load a
     * aload 1 // Load b
     * makeConcatWithConstants<invokedynamic>("\u0001<:>\u0001")
     * ```
     *
     */
    INTERPOLATE,

    /**
     * Indify providing constants to the [bootstrap method constants parameter][java.lang.invoke.StringConcatFactory.makeConcatWithConstants].
     * This results in an interpolation using `\u0001` for arguments in the stack and `\u0002` for constants in the **ConstantPool**.
     *
     * For example, the following concatenation scenario:
     *
     * ```kotlin
     * fun concat(a: String, b: String) =
     *   a + "<:>" + b
     * ```
     *
     * Is translated into the following recipe:
     * ```
     * aload 0 // Load a
     * aload 1 // Load b
     *
     * makeConcatWithConstants<invokedynamic>("\u0001\u0002\u0001", "<:>")
     * ```
     *
     * With `<:>` provided as an argument to the [bootstrap method constants parameter][java.lang.invoke.StringConcatFactory.makeConcatWithConstants].
     */
    CONSTANT,

    /**
     * Indify using only arguments in the stack. Constant values are pushed to the stack using `ldc` instruction.
     *
     * For example, the following concatenation scenario:
     *
     * ```kotlin
     * fun concat(a: String, b: String) =
     *   a + "<:>" + b
     * ```
     *
     * Is translated into the following recipe:
     * ```
     * aload 0 // Load a
     * ldc "<:>"
     * aload 1 // Load b
     * makeConcatWithConstants<invokedynamic>("\u0001\u0001\u0001")
     * ```
     */
    LDC
}

/**
 * Read more in [NestLogic]
 */
fun Options.nestAccessGenerationMode(version: Int) =
    when {
        this[FORCE_GENERATE_SYNTHETIC_ACCESS] == true && this[GENERATE_NESTS] == true && version >= 55 -> NestAccessGenerationMode.MIXED
        this[FORCE_GENERATE_SYNTHETIC_ACCESS] == true && this[FORCE_GENERATE_NESTS] == true -> NestAccessGenerationMode.MIXED
        this[FORCE_GENERATE_NESTS] == true -> NestAccessGenerationMode.NEST_BASED
        this[GENERATE_NESTS] == true && version >= 55 -> NestAccessGenerationMode.NEST_BASED
        this[GENERATE_SYNTHETIC_ACCESS] == true -> NestAccessGenerationMode.SYNTHETIC_ONLY
        else -> NestAccessGenerationMode.DISABLED
    }

/**
 * Read more in [NestLogic]
 */
enum class NestAccessGenerationMode {
    /**
     * Synthetic and Bridge based inner class private member access.
     */
    SYNTHETIC_ONLY,

    /**
     * Nest-based inner class private member access, as specified in [JEP 181](https://openjdk.java.net/jeps/181).
     */
    NEST_BASED,

    /**
     * Mixed, generate NestHost and NestMember declarations, as specified in [JEP 181](https://openjdk.java.net/jeps/181),
     * but use **Synthetic Bridge Methods** for access instead of direct access.
     *
     * Not recommended since [Nest-Based Access Control](https://openjdk.java.net/jeps/181) allows direct access to private
     * members without **bridge methods**. This should only be used in **very very very** specific cases.
     */
    MIXED,

    /**
     * Totally disabled. This may cause **class load-time** and/or **runtime exceptions** in most JVM Implementations
     * since classes are not allowed to access private members.
     */
    DISABLED
}

/**
 * Read more in [NestLogic]
 */
fun NestAccessGenerationMode.isSyntheticAccess() =
    this == NestAccessGenerationMode.SYNTHETIC_ONLY || this == NestAccessGenerationMode.MIXED

/**
 * Read more in [NestLogic]
 */
fun NestAccessGenerationMode.isToGenerateNests() =
    this == NestAccessGenerationMode.MIXED || this == NestAccessGenerationMode.NEST_BASED

enum class VisitLineType {
    /**
     * Disable line visit
     */
    DISABLED,

    /**
     * Incremental line visit
     */
    INCREMENTAL,

    /**
     * Follow Code Source indexes.
     */
    @Deprecated(message = "Imprecise approach, finally blocks receive buggy lines because of the way inlining code works.")
    FOLLOW_CODE_SOURCE,

    /**
     * Generates line instruction for each instruction *before* processing it. This introduces a little overhead.
     *
     * This uses a incremental line visiting (see [GenLineVisitor]).
     */
    GEN_LINE_INSTRUCTION,

    /**
     * Uses [Line] instructions. If this option is not set, [Line] instructions will be ignored.
     */
    LINE_INSTRUCTION
}