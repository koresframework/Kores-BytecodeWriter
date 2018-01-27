/*
 *      Kores-BytecodeWriter - Translates CodeAPI Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
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
import com.github.jonathanxd.kores.Types;
import com.github.jonathanxd.kores.base.ClassDeclaration;
import com.github.jonathanxd.kores.base.KoresModifier;
import com.github.jonathanxd.kores.base.ConstructorDeclaration;
import com.github.jonathanxd.kores.base.FieldDeclaration;
import com.github.jonathanxd.kores.base.InvokeType;
import com.github.jonathanxd.kores.bytecode.BytecodeClass;
import com.github.jonathanxd.kores.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.kores.factory.Factories;
import com.github.jonathanxd.kores.factory.InvocationFactory;
import com.github.jonathanxd.kores.literal.Literals;

import org.junit.Test;

import java.io.PrintStream;
import java.lang.invoke.MethodHandles;

import static java.util.Collections.singletonList;

public class TestHelloBytecode {
    @Test
    public void testBytecode() {

        ClassDeclaration codeClass = ClassDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .qualifiedName("fullName." + this.getClass().getSimpleName())
                .superClass(Types.OBJECT)
                .fields(
                        FieldDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC, KoresModifier.STATIC, KoresModifier.FINAL)
                                .type(Types.INT)
                                .name("DEFAULT_VALUE")
                                .value(Literals.INT(17))
                                .build()
                )
                .constructors(
                        ConstructorDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC)
                                .body(Instructions.fromVarArgs(
                                        // Chama um metodo Virtual (metodos de instancia) na Classe PrintStream
                                        InvocationFactory.invoke(InvokeType.INVOKE_VIRTUAL, PrintStream.class,
                                                // Acessa uma field estatica 'out' com tipo PrintStream na classe System
                                                Factories.accessStaticField(System.class, PrintStream.class, "out"),
                                                // Especificação do metodo
                                                // Informa que o metodo é println, e retorna um void
                                                "println",
                                                Factories.typeSpec(Types.VOID, Types.STRING),
                                                singletonList(Literals.STRING("Hello World")))
                                ))
                                .build()
                )
                .build();


        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        BytecodeClass bytecodeClass = bytecodeGenerator.process(codeClass).get(0);

        byte[] gen = bytecodeClass.getBytecode();

        ResultSaver.save(this.getClass(), bytecodeClass);

        BCLoader bcLoader = new BCLoader();

        Class<?> define = bcLoader.define("fullName." + this.getClass().getSimpleName(), gen);

        try {
            define.newInstance();

            int i = (int) MethodHandles.lookup().findStaticGetter(define, "DEFAULT_VALUE", int.class).invoke();

            System.out.println("DEFAULT_VALUE = " + i);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }


    }

    private static final class BCLoader extends ClassLoader {

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }
}
