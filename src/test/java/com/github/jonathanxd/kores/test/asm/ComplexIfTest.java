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

import com.github.jonathanxd.kores.base.TypeDeclaration;
import com.github.jonathanxd.kores.bytecode.BytecodeOptions;
import com.github.jonathanxd.kores.test.ComplexIfTest2_;
import com.github.jonathanxd.kores.test.ComplexIfTest3_;
import com.github.jonathanxd.kores.test.ComplexIfTest4_;
import com.github.jonathanxd.kores.test.ComplexIfTest_;
import com.github.jonathanxd.iutils.annotation.Named;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.function.UnaryOperator;

public class ComplexIfTest {

    @Test
    public void testComplexIf() throws Exception {
        TypeDeclaration $ = ComplexIfTest_.$();

        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), $, UnaryOperator.identity(), aClass -> {
            try {
                return aClass.getConstructor(Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE)
                        .newInstance(true, true, false, true);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @Test
    public void testComplexIf2() throws Exception {
        TypeDeclaration $ = ComplexIfTest2_.$();

        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), $, UnaryOperator.identity(), aClass -> {
            try {
                return aClass.getConstructor(Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE)
                        .newInstance(true, true, false, true, true, false);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @Test
    public void testComplexIf3() throws Exception {
        TypeDeclaration $ = ComplexIfTest3_.$();

        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), $, UnaryOperator.identity(), aClass -> {
            try {
                return aClass.getConstructor(Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE)
                        .newInstance(true, true, false, true, true, false);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }, bytecodeGenerator -> {
            bytecodeGenerator.getOptions().set(BytecodeOptions.POST_PROCESSING, Boolean.FALSE);
        });

    }

    @Test
    public void testComplexIf4() throws Exception {
        TypeDeclaration $ = ComplexIfTest4_.$();

        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), $, UnaryOperator.identity(), aClass -> {
            try {
                return aClass.getConstructor(Boolean.TYPE, Boolean.TYPE, Boolean.TYPE)
                        .newInstance(true, true, false);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }, bytecodeGenerator -> {
            bytecodeGenerator.getOptions().set(BytecodeOptions.POST_PROCESSING, Boolean.FALSE);
        });

    }
}
