/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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
package com.github.jonathanxd.kores.test.asm;

import com.github.jonathanxd.kores.Instructions;
import com.github.jonathanxd.kores.base.ClassDeclaration;
import com.github.jonathanxd.kores.base.MethodDeclaration;
import com.github.jonathanxd.kores.base.TypeDeclaration;
import com.github.jonathanxd.kores.bytecode.BytecodeClass;
import com.github.jonathanxd.kores.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.kores.factory.Factories;
import com.github.jonathanxd.kores.helper.Predefined;

import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static com.github.jonathanxd.kores.Types.STRING;
import static com.github.jonathanxd.kores.Types.VOID;
import static com.github.jonathanxd.kores.base.KoresModifier.PUBLIC;
import static com.github.jonathanxd.kores.base.KoresModifier.STATIC;

@SuppressWarnings("Duplicates")
public class KoresTest_API {

    @Test
    public void codeAPITest() {

        String name = this.getClass().getCanonicalName() + "_Generated";

        ClassDeclaration codeClass = ClassDeclaration.Builder.builder()
                .modifiers(PUBLIC)
                .name(name)
                .methods(MethodDeclaration.Builder.builder()
                        .modifiers(PUBLIC, STATIC)
                        .name("printString")
                        .returnType(VOID)
                        .parameters(Factories.parameter(STRING, "string"))
                        .body(Instructions.fromPart(Predefined.invokePrintln(Factories.accessVariable(STRING, "string"))))
                        .build())
                .build();

        byte[] bytes = generate(codeClass);

        Class<?> define = new BCLoader().define(name, bytes);
        try {
            define.newInstance();

            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            MethodHandle println = lookup.findStatic(define, "printString", java.lang.invoke.MethodType.methodType(Void.TYPE, String.class));

            println.invoke((Object) "Test");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public byte[] generate(TypeDeclaration declaration) {
        BytecodeGenerator generator = new BytecodeGenerator();

        BytecodeClass bytecodeClass = generator.process(declaration).get(0);

        byte[] bytes = bytecodeClass.getBytecode();

        ResultSaver.save(this.getClass(), bytecodeClass);

        return bytes;
    }

    private static final class BCLoader extends ClassLoader {

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }

}
