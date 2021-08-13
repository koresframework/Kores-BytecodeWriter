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
package com.github.jonathanxd.kores.bytecode.doc

import com.github.jonathanxd.iutils.option.Options

/**
 * Annotates an `object` as In-Code Documentation Member.
 *
 * ## In-Code Documentation
 *
 * Kores mix comments and **In-Code Object Markers** that are solely part of the documentation (in other words,
 * objects that are not used for anything more than explaining how things works).
 *
 * Because of that, Kores is always shipped with its source code.
 *
 * ## Features
 *
 * It is important to keep in mind that Kores is a complex library, and its **generators/processors** are powerful tools
 * that are always taking advantage of new features provided by JVM automatically, so there is a bunch of mechanisms that
 * are enabled automatically when you update to recent Kores Versions, these things are well documented and can be disabled
 * through [Options].
 */
@Retention(AnnotationRetention.BINARY)
annotation class Doc()
