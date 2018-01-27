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
import com.github.jonathanxd.kores.base.Access;
import com.github.jonathanxd.kores.base.ClassDeclaration;
import com.github.jonathanxd.kores.base.KoresModifier;
import com.github.jonathanxd.kores.base.TypeDeclaration;
import com.github.jonathanxd.kores.factory.Factories;
import com.github.jonathanxd.kores.factory.PartFactory;
import com.github.jonathanxd.kores.literal.Literals;
import com.github.jonathanxd.iutils.annotation.Named;

import org.junit.Test;

import java.util.ArrayList;

public class SelfPrivateAccessBugTest {


    @SuppressWarnings("unchecked")
    @Test
    public void selfPrivateAccessBugTest() throws Throwable {
        TypeDeclaration decl = ClassDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .specifiedName("com.MyClass")
                .fields(new ArrayList<>())
                .constructors(new ArrayList<>())
                .methods(new ArrayList<>())
                .build();

        decl.getFields().add(PartFactory.fieldDec()
                .modifiers(KoresModifier.PRIVATE, KoresModifier.STATIC, KoresModifier.FINAL)
                .type(String.class)
                .name("f1")
                .value(Literals.STRING("Test"))
                .build());

        decl.getFields().add(PartFactory.fieldDec()
                .modifiers(KoresModifier.PRIVATE, KoresModifier.FINAL)
                .type(Integer.TYPE)
                .name("f2")
                .value(Literals.INT(10))
                .build());

        decl.getMethods().add(PartFactory.methodDec()
                .modifiers(KoresModifier.PUBLIC, KoresModifier.STATIC)
                .returnType(String.class)
                .name("getStaticF1")
                .body(Instructions.fromPart(
                        Factories.returnValue(String.class,
                                Factories.accessStaticField(decl, String.class, "f1"))
                ))
                .build());

        decl.getMethods().add(PartFactory.methodDec()
                .publicModifier()
                .returnType(Integer.TYPE)
                .name("getF2")
                .body(Instructions.fromPart(
                        Factories.returnValue(Integer.TYPE,
                                Factories.accessField(decl, Access.THIS, Integer.TYPE, "f2"))
                ))
                .build());

        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), decl);
    }
}
