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
import com.github.jonathanxd.codeapi.CodeSource;
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.common.CodeParameter;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.operator.Operators;
import com.github.jonathanxd.iutils.annotation.Named;

import org.junit.Test;

import java.util.EnumSet;

import static com.github.jonathanxd.codeapi.CodeAPI.parameter;
import static com.github.jonathanxd.codeapi.CodeAPI.sourceOfParts;
import static com.github.jonathanxd.codeapi.Types.INT;
import static com.github.jonathanxd.codeapi.common.CodeModifier.PUBLIC;
import static com.github.jonathanxd.codeapi.factory.ClassFactory.aClass;
import static com.github.jonathanxd.codeapi.factory.MethodFactory.method;

public class NewIfTest {


    @Test
    public void newIfTest() throws Throwable {
        CodeSource source = sourceOfParts(
                method(EnumSet.of(PUBLIC), "test", INT, new CodeParameter[]{
                                parameter(Integer.TYPE, "x"),
                        },
                        sourceOfParts(
                                CodeAPI.ifStatement(
                                        CodeAPI.ifExprs(
                                                CodeAPI.check(CodeAPI.accessLocalVariable(Integer.TYPE, "x"), Operators.EQUAL_TO, Literals.INT(7)),
                                                Operators.OR,
                                                CodeAPI.check(CodeAPI.accessLocalVariable(Integer.TYPE, "x"), Operators.EQUAL_TO, Literals.INT(9))
                                        ),
                                        CodeAPI.source(
                                                CodeAPI.returnValue(INT, Literals.INT(0))
                                        ),
                                        CodeAPI.source(
                                                CodeAPI.returnValue(INT, Literals.INT(1))
                                        )
                                )
                        )
                )
        );

        TypeDeclaration decl = aClass(EnumSet.of(PUBLIC), "com.NewIf", source);

        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), decl, CodeAPI.source(decl));

        Class<?> define = test.getClass();

        int result = (int) define.getDeclaredMethod("test", Integer.TYPE).invoke(test, 7);

        System.out.println(result);


    }
}
