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
package com.github.jonathanxd.codeapi.test.asm;

import com.github.jonathanxd.codeapi.CodeAPI;
import com.github.jonathanxd.codeapi.MutableCodeSource;
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.ConstructorDeclaration;
import com.github.jonathanxd.codeapi.builder.ClassDeclarationBuilder;
import com.github.jonathanxd.codeapi.builder.ConstructorDeclarationBuilder;
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator;
import com.github.jonathanxd.codeapi.common.CodeArgument;
import com.github.jonathanxd.codeapi.common.CodeModifier;
import com.github.jonathanxd.codeapi.common.InvokeType;
import com.github.jonathanxd.codeapi.factory.FieldFactory;
import com.github.jonathanxd.codeapi.literal.Literals;

import org.junit.Test;

import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.util.EnumSet;

import static java.util.Collections.singletonList;

public class TestHelloBytecode {
    @Test
    public void testBytecode() {

        MutableCodeSource codeSource = new MutableCodeSource();

        MutableCodeSource clSource = new MutableCodeSource();

        ClassDeclaration codeClass = ClassDeclarationBuilder.builder()
                .withModifiers(CodeModifier.PUBLIC)
                .withQualifiedName("fullName." + this.getClass().getSimpleName())
                .withSuperClass(Types.OBJECT)
                .withBody(clSource)
                .build();

        clSource.add(FieldFactory.field(EnumSet.of(CodeModifier.PUBLIC, CodeModifier.STATIC, CodeModifier.FINAL),
                Types.INT,
                "DEFAULT_VALUE",
                Literals.INT(17)));

        ConstructorDeclaration codeConstructor = ConstructorDeclarationBuilder.builder()
                .withModifiers(CodeModifier.PUBLIC)
                .withBody(CodeAPI.source(
                        // Chama um metodo Virtual (metodos de instancia) na Classe PrintStream
                        CodeAPI.invoke(InvokeType.INVOKE_VIRTUAL, PrintStream.class,
                                // Acessa uma field estatica 'out' com tipo PrintStream na classe System
                                CodeAPI.accessStaticField(System.class, PrintStream.class, "out"),
                                // Especificação do metodo
                                // Informa que o metodo é println, e retorna um void
                                "println",
                                CodeAPI.typeSpec(Types.VOID, Types.STRING),
                                singletonList(new CodeArgument(Literals.STRING("Hello World"))))
                ))
                .build();


        clSource.add(codeConstructor);

        codeSource.add(codeClass);

        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        byte[] gen = bytecodeGenerator.gen(codeSource)[0].getBytecode();

        ResultSaver.save(this.getClass(), gen);

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
