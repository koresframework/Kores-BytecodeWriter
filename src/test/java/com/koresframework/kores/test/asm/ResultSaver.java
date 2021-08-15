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
package com.koresframework.kores.test.asm;

import com.github.jonathanxd.bytecodedisassembler.Disassembler;
import com.koresframework.kores.base.TypeDeclaration;
import com.koresframework.kores.bytecode.BytecodeClass;
import com.github.jonathanxd.iutils.array.PrimitiveArrayConverter;
import com.koresframework.kores.bytecode.BytecodeClass;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class ResultSaver {

    public static final boolean IS_GRADLE_ENVIRONMENT;

    static {
        String prop = System.getProperty("env");

        IS_GRADLE_ENVIRONMENT = false;//prop != null && prop.equals("gradle");

        if (IS_GRADLE_ENVIRONMENT) {
            System.out.println("Gradle environment property defined!");
        }
    }

    public static void save(Class<?> ofClass, BytecodeClass[] result) {
        for (BytecodeClass bytecodeClass : result) { // TODO: Support modules
            save(ofClass, ((TypeDeclaration) bytecodeClass.getDeclaration()).getSimpleName(), bytecodeClass);
        }
    }

    public static void save(Class<?> ofClass, List<? extends BytecodeClass> result) {
        for (BytecodeClass bytecodeClass : result) { // TODO: Support modules
            save(ofClass, ((TypeDeclaration) bytecodeClass.getDeclaration()).getSimpleName(), bytecodeClass);
        }
    }

    public static void save(Class<?> ofClass, String tag, BytecodeClass[] result) {
        for (BytecodeClass bytecodeClass : result) { // TODO: Support modules
            save(ofClass, tag + "_" + ((TypeDeclaration) bytecodeClass.getDeclaration()).getSimpleName(), bytecodeClass);
        }
    }

    public static void save(Class<?> ofClass, String tag, BytecodeClass result) {
        if (IS_GRADLE_ENVIRONMENT)
            return;

        save(ofClass, tag, result.getBytecode());
    }

    public static void save(Class<?> ofClass, String tag, byte[] result) {
        if (IS_GRADLE_ENVIRONMENT)
            return;

        try {
            Path path = Paths.get("src", "test", "resources");

            if (!Files.exists(path))
                Files.createDirectories(path);

            String simpleName = ofClass.getSimpleName() + (tag != null ? "_" + tag : "") + "_Result.class";

            Path file = path.resolve(simpleName);

            Files.deleteIfExists(file);

            Files.write(file, result, StandardOpenOption.CREATE);

            Path disassemblePath = path.resolve("disassembled");
            Path savedPath = disassemblePath.resolve(simpleName + ".disassembled");

            if (!Files.exists(disassemblePath))
                Files.createDirectories(disassemblePath);

            Files.deleteIfExists(savedPath);

            try {
                String disassemble = Disassembler.disassemble(file, false, true);

                Files.write(savedPath, disassemble.getBytes(Charset.forName("UTF-8")), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

                Process start = new ProcessBuilder("git", "add", savedPath.toAbsolutePath().toString())
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .redirectErrorStream(true)
                        .start();

                start.waitFor();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void save(Class<?> ofClass, BytecodeClass bytecodeClass) {
        if (IS_GRADLE_ENVIRONMENT)
            return;

        save(ofClass, null, bytecodeClass);

    }

    public static void save(Class<?> ofClass, byte[] result) {
        if (IS_GRADLE_ENVIRONMENT)
            return;

        save(ofClass, null, result);

    }

    public static void save(Class<?> ofClass, Byte[] result) {
        save(ofClass, PrimitiveArrayConverter.toPrimitive(result));

    }

}
