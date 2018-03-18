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
import com.github.jonathanxd.kores.test.ComplexInnerClassTest_;
import com.github.jonathanxd.kores.test.ComplexStatic1InnerClassTest_;
import com.github.jonathanxd.kores.test.ComplexStatic2InnerClassTest_;
import com.github.jonathanxd.kores.test.InnerClassTest_;
import com.github.jonathanxd.iutils.annotation.Named;

import org.junit.Test;

public class InnerClassTest {

    @Test
    public void innerClass() {
        TypeDeclaration $ = InnerClassTest_.$();
        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), $);
        for (Class<?> aClass : test.getClass().getDeclaredClasses()) {
            System.out.println(aClass);
        }
    }


    @Test
    public void complexInnerClass() {
        TypeDeclaration $ = ComplexInnerClassTest_.$();
        @Named("Instance") Object test = CommonBytecodeTest.test(ComplexInnerClassTest_.class, $);
        for (Class<?> aClass : test.getClass().getDeclaredClasses()) {
            System.out.println(aClass);
        }
    }

    @Test
    public void complexStatic1InnerClass() {
        TypeDeclaration $ = ComplexStatic1InnerClassTest_.$();
        @Named("Instance") Object test = CommonBytecodeTest.test(ComplexStatic1InnerClassTest_.class, $);
        for (Class<?> aClass : test.getClass().getDeclaredClasses()) {
            System.out.println(aClass);
        }
    }

    @Test
    public void complexStatic2InnerClass() {
        TypeDeclaration $ = ComplexStatic2InnerClassTest_.$();
        @Named("Instance") Object test = CommonBytecodeTest.test(ComplexStatic2InnerClassTest_.class, $);
        for (Class<?> aClass : test.getClass().getDeclaredClasses()) {
            System.out.println(aClass);
        }
    }

}
