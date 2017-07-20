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

import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass;
import com.github.jonathanxd.codeapi.bytecode.BytecodeOptions;
import com.github.jonathanxd.codeapi.bytecode.VisitLineType;
import com.github.jonathanxd.codeapi.bytecode.exception.ClassCheckException;
import com.github.jonathanxd.codeapi.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.iutils.annotation.Named;
import com.github.jonathanxd.iutils.exception.RethrowException;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class CommonBytecodeTest {

    public static @Named("Instance") Object test(Class<?> testClass, TypeDeclaration mainClass) {
        return CommonBytecodeTest.test(testClass, mainClass, UnaryOperator.identity());
    }

    public static @Named("Instance") Object test(Class<?> testClass, ClassDeclaration mainClass) {
        return CommonBytecodeTest.test(testClass, (TypeDeclaration) mainClass, UnaryOperator.identity());
    }

    public static @Named("Instance") Object test(Class<?> testClass, ClassDeclaration mainClass, UnaryOperator<TypeDeclaration> modifier) {
        return CommonBytecodeTest.test(testClass, (TypeDeclaration) mainClass, modifier);
    }

    public static @Named("Instance") <R> R test(Class<?> testClass, ClassDeclaration mainClass, UnaryOperator<TypeDeclaration> modifier, Function<Class<?>, R> function) {
        return CommonBytecodeTest.test(testClass, (TypeDeclaration) mainClass, modifier, function);
    }

    public static @Named("Instance") Object test(Class<?> testClass, TypeDeclaration mainClass, UnaryOperator<TypeDeclaration> modifier) {
        return test(testClass, mainClass, modifier, aClass -> {
            try {
                return aClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RethrowException(e, e.getCause());
            }
        });
    }

    public static @Named("Instance") <R> R test(Class<?> testClass,
                                                TypeDeclaration mainClass,
                                                UnaryOperator<TypeDeclaration> modifier,
                                                Function<Class<?>, R> function) {
        return test(testClass, mainClass, modifier, function, bytecodeProcessor -> {});
    }

    public static @Named("Instance") <R> R test(Class<?> testClass,
                                                TypeDeclaration mainClass,
                                                UnaryOperator<TypeDeclaration> modifier,
                                                Function<Class<?>, R> function,
                                                Consumer<BytecodeGenerator> bytecodeProcessorConsumer) {
        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        bytecodeGenerator.getOptions().set(BytecodeOptions.VISIT_LINES, VisitLineType.FOLLOW_CODE_SOURCE);
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
            TypeDeclaration type = bytecodeClass.getType();
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
