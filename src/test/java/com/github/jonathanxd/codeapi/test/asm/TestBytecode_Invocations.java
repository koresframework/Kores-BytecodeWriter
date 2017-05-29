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

import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.test.InvocationsTest_;
import com.github.jonathanxd.iutils.annotation.Named;

import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.UnaryOperator;

@SuppressWarnings("Duplicates")
public class TestBytecode_Invocations {

    @Test
    public void testBytecode() {
        TypeDeclaration $ = InvocationsTest_.$();

        @Named("Instance") Class<?> define = CommonBytecodeTest.test(this.getClass(), $, UnaryOperator.identity(), aClass -> aClass);

        System.out.println("Class -> " + Modifier.toString(define.getModifiers()) + " " + define);

        Object o;
        try {
            o = define.newInstance();
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            MethodHandle printIt = lookup.findVirtual(define, "printIt", MethodType.methodType(Void.TYPE, Object.class)).bindTo(o);

            try {
                System.out.println("NAO DEVE FALAR HELLO");
                printIt.invoke((Object) null);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

            MethodHandle check = lookup.findVirtual(define, "check", MethodType.methodType(Boolean.TYPE, Integer.TYPE)).bindTo(o);

            try {
                System.out.println("CHECK NINE");
                boolean invoke = (boolean) check.invoke(9);

                System.out.println("Invoke = " + invoke);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        for (Field field : define.getDeclaredFields()) {
            try {
                System.out.println("Field -> " + field + " = " + field.get(o));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

}