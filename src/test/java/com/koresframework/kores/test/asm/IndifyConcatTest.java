/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2021 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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
package com.koresframework.kores.test.asm;

import com.github.jonathanxd.iutils.annotation.Named;
import com.koresframework.kores.base.TypeDeclaration;
import com.koresframework.kores.bytecode.BytecodeOptions;
import com.koresframework.kores.bytecode.IndyConcatStrategy;
import com.koresframework.kores.test.ConcatTest_;
import org.junit.Test;

import java.util.function.UnaryOperator;

public class IndifyConcatTest {

    @Test
    public void indyConcatTest() {
        TypeDeclaration $ = ConcatTest_.$();
        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), $, UnaryOperator.identity(), aClass -> {
            try {
                return aClass.getConstructor(String.class).newInstance("Unknown");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, generator -> {
            generator.getOptions().set(BytecodeOptions.INDIFY_STRING_CONCAT, true);
            generator.getOptions().set(BytecodeOptions.INDY_CONCAT_STRATEGY, IndyConcatStrategy.INTERPOLATE);
        });
    }

}
