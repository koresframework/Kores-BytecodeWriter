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
package com.github.jonathanxd.kores.bytecode.util

import com.github.jonathanxd.kores.base.TypeDeclaration
import com.github.jonathanxd.kores.bytecode.BytecodeClass
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

fun com.github.jonathanxd.kores.bytecode.BytecodeClass.save(
    directory: Path,
    disassemble: Boolean = false,
    alternativeDir: Boolean = false
) {

    val targetPath = this.toPath(directory)

    Files.createDirectories(targetPath)

    val name = (this.declaration as? TypeDeclaration)?.simpleName ?: this.declaration.name

    val classPath = targetPath.resolve("$name.class")

    if (Files.exists(classPath))
        Files.deleteIfExists(classPath)

    Files.write(classPath, this.bytecode, StandardOpenOption.CREATE)

    if (disassemble) {
        val base =
            if (alternativeDir) this.toPath(directory.resolve("disassembled")) else targetPath

        Files.createDirectories(base)
        val disassembledPath = base.resolve("$name.class.dissassembled")

        if (Files.exists(disassembledPath))
            Files.deleteIfExists(disassembledPath)

        Files.write(
            disassembledPath,
            this.disassembledCode.toByteArray(),
            StandardOpenOption.CREATE
        )
    }
}

fun com.github.jonathanxd.kores.bytecode.BytecodeClass.toPath(base: Path): Path =
    ((this.declaration as? TypeDeclaration)?.packageName ?: this.declaration.name)
        .split('.')
        .fold(base) { acc, s -> acc.resolve(s) }


fun com.github.jonathanxd.kores.bytecode.BytecodeClass.toPathWithName(base: Path): Path =
    this.toPath(base).resolve(
        (this.declaration as? TypeDeclaration)?.simpleName ?: this.declaration.name
    )

fun com.github.jonathanxd.kores.bytecode.BytecodeClass.toPathWithNameAnd(base: Path, str: String): Path =
    this.toPath(base).resolve(
        "${(this.declaration as? TypeDeclaration)?.simpleName ?: this.declaration.name}$str"
    )
