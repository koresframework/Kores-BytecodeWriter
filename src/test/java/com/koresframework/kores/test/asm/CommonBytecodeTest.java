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

import com.koresframework.kores.base.ClassDeclaration;
import com.koresframework.kores.base.TypeDeclaration;
import com.koresframework.kores.bytecode.BytecodeClass;
import com.koresframework.kores.bytecode.BytecodeOptions;
import com.koresframework.kores.bytecode.VisitLineType;
import com.koresframework.kores.bytecode.exception.ClassCheckException;
import com.koresframework.kores.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.iutils.annotation.Named;
import com.github.jonathanxd.iutils.exception.RethrowException;
import com.github.jonathanxd.iutils.function.checked.function.CFunction;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class CommonBytecodeTest {

    public static @Named("Instance") Object test(Class<?> testClass, TypeDeclaration mainClass) {
        return CommonBytecodeTest.test(testClass, mainClass, UnaryOperator.identity());
    }

    public static @Named("Instance") Object testWithOptions(Class<?> testClass,
                                                            TypeDeclaration mainClass,
                                                            Consumer<BytecodeGenerator> bytecodeProcessorConsumer) {
        return CommonBytecodeTest.test(testClass, mainClass, UnaryOperator.identity(), aClass -> {
            try {
                return aClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw RethrowException.rethrow(e);
            }
        }, bytecodeProcessorConsumer);
    }

    public static @Named("Instance") Object test(Class<?> testClass, ClassDeclaration mainClass) {
        return CommonBytecodeTest.test(testClass, (TypeDeclaration) mainClass, UnaryOperator.identity());
    }

    public static @Named("Instance") Object test(Class<?> testClass, ClassDeclaration mainClass, UnaryOperator<TypeDeclaration> modifier) {
        return CommonBytecodeTest.test(testClass, (TypeDeclaration) mainClass, modifier);
    }

    public static @Named("Instance") <R> R test(Class<?> testClass, ClassDeclaration mainClass, UnaryOperator<TypeDeclaration> modifier, CFunction<Class<?>, R> function) {
        return CommonBytecodeTest.test(testClass, (TypeDeclaration) mainClass, modifier, function);
    }

    public static @Named("Instance") Object test(Class<?> testClass, TypeDeclaration mainClass, UnaryOperator<TypeDeclaration> modifier) {
        return test(testClass, mainClass, modifier, aClass -> {
            try {
                return aClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw RethrowException.rethrow(e);
            }
        });
    }

    public static @Named("Instance") <R> R test(Class<?> testClass,
                                                TypeDeclaration mainClass,
                                                UnaryOperator<TypeDeclaration> modifier,
                                                CFunction<Class<?>, R> function) {
        return test(testClass, mainClass, modifier, function, bytecodeProcessor -> {
        });
    }

    public static @Named("Instance") <R> R test(Class<?> testClass,
                                                TypeDeclaration mainClass,
                                                UnaryOperator<TypeDeclaration> modifier,
                                                CFunction<Class<?>, R> function,
                                                Consumer<BytecodeGenerator> bytecodeProcessorConsumer) {
        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        bytecodeGenerator.getOptions().set(BytecodeOptions.VISIT_LINES, VisitLineType.GEN_LINE_INSTRUCTION);
        bytecodeGenerator.getOptions().set(BytecodeOptions.INDIFY_STRING_CONCAT, false);
        bytecodeProcessorConsumer.accept(bytecodeGenerator);
        BCLoader bcLoader = new BCLoader();

        List<? extends BytecodeClass> bytecodeClasses;

        mainClass = modifier.apply(mainClass);

        try {
            bytecodeClasses = bytecodeGenerator.process(mainClass);
        } catch (ClassCheckException e) {
            bytecodeClasses = e.getBytecodeClasses();
        }

        Class<?> first = null;

        for (BytecodeClass bytecodeClass : bytecodeClasses) {
            TypeDeclaration type = (TypeDeclaration) bytecodeClass.getDeclaration();// TODO: Support modules
            byte[] bytecode = bytecodeClass.getBytecode();

            ResultSaver.save(testClass, type.getSimpleName(), bytecodeClass);

            Class<?> define = bcLoader.define(type, bytecode);

            if (mainClass != null && type.is(mainClass))
                first = define;
            else if (first == null)
                first = define;
        }

        return function.apply(first);
    }

}
