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
import com.github.jonathanxd.codeapi.MutableCodeSource;
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.factory.ClassFactory;
import com.github.jonathanxd.codeapi.factory.ConstructorFactory;
import com.github.jonathanxd.codeapi.factory.FieldFactory;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.type.CodeType;
import com.github.jonathanxd.codeapi.type.LoadedCodeType;
import com.github.jonathanxd.iutils.exception.RethrowException;

import org.junit.Test;

import java.util.EnumSet;

import static com.github.jonathanxd.codeapi.common.CodeModifier.FINAL;
import static com.github.jonathanxd.codeapi.common.CodeModifier.PRIVATE;
import static com.github.jonathanxd.codeapi.common.CodeModifier.PUBLIC;
import static kotlin.collections.CollectionsKt.listOf;

public class FinalFieldWithThis {


    @Test
    public void finalFieldWithThis() {
        MutableCodeSource codeSource = new MutableCodeSource();

        LoadedCodeType<TestBox> testBoxJavaType = CodeAPI.getJavaType(TestBox.class);

        ClassDeclaration testField = ClassFactory.aClass(EnumSet.of(PUBLIC), "finalfieldwiththis.Test", testBoxJavaType, new CodeType[0],
                CodeAPI.sourceOfParts(
                        FieldFactory.field(EnumSet.of(PRIVATE, FINAL), testBoxJavaType, "testField",
                                CodeAPI.invokeConstructor(testBoxJavaType, CodeAPI.constructorTypeSpec(Types.OBJECT), listOf(CodeAPI.argument(CodeAPI.accessThis())))),
                        ConstructorFactory.constructor(EnumSet.of(PUBLIC), CodeAPI.sourceOfParts(
                                CodeAPI.invokeSuperConstructor(testBoxJavaType, CodeAPI.constructorTypeSpec(Types.OBJECT), listOf(CodeAPI.argument(Literals.NULL)))
                        ))
                ));

        codeSource.add(testField);

        CommonBytecodeTest.test(this.getClass(), testField, codeSource, aClass -> {
            try {
                return aClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RethrowException(e, e.getCause());
            }
        });
    }

    public static class TestBox {
        private final Object o;

        public TestBox(Object o) {
            this.o = o;
        }
    }
}
