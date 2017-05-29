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

import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.factory.VariableFactory;
import com.github.jonathanxd.codeapi.helper.Predefined;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.iutils.annotation.Named;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static com.github.jonathanxd.codeapi.base.CodeModifier.PUBLIC;
import static com.github.jonathanxd.codeapi.factory.Factories.accessVariable;
import static com.github.jonathanxd.codeapi.factory.Factories.catchStatement;
import static com.github.jonathanxd.codeapi.factory.Factories.constructorTypeSpec;
import static com.github.jonathanxd.codeapi.factory.Factories.throwException;
import static com.github.jonathanxd.codeapi.factory.Factories.tryStatement;
import static com.github.jonathanxd.codeapi.factory.InvocationFactory.invokeConstructor;
import static com.github.jonathanxd.codeapi.factory.PartFactory.classDec;
import static com.github.jonathanxd.codeapi.factory.PartFactory.constructorDec;
import static com.github.jonathanxd.codeapi.factory.PartFactory.source;

public class FinallyTest {

    @Test(expected = RuntimeException.class)
    public void test() {
        TypeDeclaration codeInterface = classDec().modifiers(PUBLIC).name("test.Btc")
                .constructors(constructorDec().modifiers(PUBLIC).body(source(
                        tryStatement(source(
                                throwException(invokeConstructor(RuntimeException.class,
                                        constructorTypeSpec(String.class),
                                        Collections.singletonList(Literals.STRING("EXCEPTION")))
                                )),
                                Collections.singletonList(
                                        catchStatement(Collections.singletonList(Exception.class),
                                                VariableFactory.variable(Types.EXCEPTION, "ex"),
                                                source(
                                                        throwException(
                                                                invokeConstructor(
                                                                        RuntimeException.class,
                                                                        constructorTypeSpec(String.class, Throwable.class),
                                                                        Arrays.asList(
                                                                                Literals.STRING("Rethrow"),
                                                                                accessVariable(Throwable.class, "ex")
                                                                        )
                                                                ))
                                                )
                                        )),
                                source(
                                        Predefined.invokePrintln(Literals.STRING("Finally"))
                                ))
                )).build())
                .build();


        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), codeInterface);
    }

}
