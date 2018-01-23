/*
 *      CodeAPI-BytecodeWriter - Translates CodeAPI Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
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
package com.github.jonathanxd.codeapi.test.asm;

import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.factory.Factories;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.operator.Operators;
import com.github.jonathanxd.iutils.annotation.Named;

import org.junit.Assert;
import org.junit.Test;

import java.util.function.UnaryOperator;

import static com.github.jonathanxd.codeapi.Types.INT;
import static com.github.jonathanxd.codeapi.base.CodeModifier.PUBLIC;
import static com.github.jonathanxd.codeapi.factory.PartFactory.classDec;
import static com.github.jonathanxd.codeapi.factory.PartFactory.methodDec;
import static com.github.jonathanxd.codeapi.factory.PartFactory.source;

public class NewIfTest {

    @Test
    public void newIfTest() throws Throwable {
        TypeDeclaration decl = classDec().modifiers(PUBLIC).name("com.NewIf")
                .methods(
                        methodDec().modifiers(PUBLIC)
                                .returnType(INT)
                                .parameters(Factories.parameter(Integer.TYPE, "x")).name("test")
                                .body(source(
                                        Factories.ifStatement(
                                                Factories.ifExprs(
                                                        Factories.check(Factories.accessVariable(Integer.TYPE, "x"), Operators.EQUAL_TO, Literals.INT(7)),
                                                        Operators.OR,
                                                        Factories.check(Factories.accessVariable(Integer.TYPE, "x"), Operators.EQUAL_TO, Literals.INT(9))
                                                ),
                                                source(
                                                        Factories.returnValue(INT, Literals.INT(0)) // x == 7 || x == 9
                                                ),
                                                source(
                                                        Factories.returnValue(INT, Literals.INT(1)) // x != 7 && x != 9
                                                )
                                        )
                                ))
                                .build()
                )
                .build();

        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), decl, UnaryOperator.identity());

        Class<?> define = test.getClass();

        int result = (int) define.getDeclaredMethod("test", Integer.TYPE).invoke(test, 7);
        int result2 = (int) define.getDeclaredMethod("test", Integer.TYPE).invoke(test, 8);

        Assert.assertEquals(0, result);
        Assert.assertEquals(1, result2);
    }

}
