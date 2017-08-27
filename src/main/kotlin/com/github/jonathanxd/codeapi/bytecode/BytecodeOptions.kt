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
@file:JvmName("BytecodeOptions")

package com.github.jonathanxd.codeapi.bytecode

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.Line
import com.github.jonathanxd.codeapi.bytecode.post.DeadCodeRemover
import com.github.jonathanxd.codeapi.bytecode.post.GotoOptimizer
import com.github.jonathanxd.codeapi.bytecode.post.MethodProcessor
import com.github.jonathanxd.codeapi.bytecode.post.PostProcessor
import com.github.jonathanxd.codeapi.bytecode.pre.GenLineVisitor
import com.github.jonathanxd.iutils.option.Option

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
 * Validate constructor this() invocation.
 *
 * Known supported generators: `BytecodeGenerator`.
 */
@JvmField
val VALIDATE_SUPER = Option(true)

/**
 * Automatically generate bridge methods.
 *
 * The generation of bridge methods will slow down the Generator, if you mind the performance
 * don't change this option.
 *
 * The generation of bridge methods is very limited, the [Bridge Generator][com.github.jonathanxd.codeapi.bytecode.util.BridgeUtil]
 * will inspect super-classes (and super-interfaces) and find the overridden method.
 * The [Bridge Generator][com.github.jonathanxd.codeapi.bytecode.util.BridgeUtil] will only inspect
 * [Java type][Class] and [CodeAPI Type][com.github.jonathanxd.codeapi.base.TypeDeclaration],
 * if the super-class and/or super-interfaces is of another type, [Bridge Generator][com.github.jonathanxd.codeapi.bytecode.util.BridgeUtil]
 * will ignore and the bridge method will not be generated.
 *
 * You could also bridge methods manually with [com.github.jonathanxd.codeapi.CodeAPI.bridgeMethod].
 *
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
val GENERATE_SYNTHETIC_ACCESS = Option(true) // TODO: Fix: does not have effect

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
    @Deprecated(message = "Imprecise approach, finally blocks receive buggy lines because of the way that inlining works.")
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