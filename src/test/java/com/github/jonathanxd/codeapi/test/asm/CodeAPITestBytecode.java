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
import com.github.jonathanxd.codeapi.CodePart;
import com.github.jonathanxd.codeapi.CodeSource;
import com.github.jonathanxd.codeapi.MutableCodeSource;
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.CatchStatement;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.IfStatement;
import com.github.jonathanxd.codeapi.base.MethodDeclaration;
import com.github.jonathanxd.codeapi.base.VariableDeclaration;
import com.github.jonathanxd.codeapi.builder.ClassDeclarationBuilder;
import com.github.jonathanxd.codeapi.builder.MethodDeclarationBuilder;
import com.github.jonathanxd.codeapi.builder.VariableDeclarationBuilder;
import com.github.jonathanxd.codeapi.bytecode.processor.BytecodeProcessor;
import com.github.jonathanxd.codeapi.common.CodeModifier;
import com.github.jonathanxd.codeapi.common.CodeParameter;
import com.github.jonathanxd.codeapi.common.InvokeType;
import com.github.jonathanxd.codeapi.common.TypeSpec;
import com.github.jonathanxd.codeapi.factory.VariableFactory;
import com.github.jonathanxd.codeapi.helper.Predefined;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.operator.Operators;
import com.github.jonathanxd.codeapi.type.CodeType;
import com.github.jonathanxd.codeapi.type.LoadedCodeType;

import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import kotlin.collections.CollectionsKt;

@SuppressWarnings("Duplicates")
public class CodeAPITestBytecode {

    public final String b = "9";

    private static CodeSource rethrow(String variable) {
        MutableCodeSource source = new MutableCodeSource();

        source.add(Predefined.invokePrintln(Literals.STRING("Rethrow from var '" + variable + "'!")));

        source.add(CodeAPI.invoke(InvokeType.INVOKE_VIRTUAL, Throwable.class, CodeAPI.accessLocalVariable(Throwable.class, variable),
                "printStackTrace",
                new TypeSpec(Types.VOID),
                Collections.emptyList()));

        /*source.add(Helper.throwException(Helper.getJavaType(RuntimeException.class), new CodeArgument[]{
                new CodeArgument(Helper.accessLocalVariable(variable, Helper.getJavaType(Throwable.class)), false, Helper.getJavaType(Throwable.class))
        }));*/

        //throw new RuntimeException(e);

        return source;
    }

    private static CodePart invokePrintlnMethod(CodePart varToPrint) {
        return CodeAPI.invoke(
                InvokeType.INVOKE_VIRTUAL,
                CodeAPI.getJavaType(PrintStream.class),
                CodeAPI.accessField(CodeAPI.getJavaType(System.class), CodeAPI.accessStatic(), CodeAPI.getJavaType(PrintStream.class), "out"),
                "println",
                new TypeSpec(Types.VOID, CollectionsKt.listOf(Types.OBJECT)),
                CollectionsKt.listOf(varToPrint));
    }

    private static MethodDeclaration createMethod() {

        MutableCodeSource methodSource = new MutableCodeSource();

        // Declare 'println' method
        MethodDeclaration codeMethod = MethodDeclarationBuilder.builder()
                .withName("println")
                // Add parameter 'Object msg'
                .withParameters(Collections.singletonList(new CodeParameter(Types.OBJECT, "msg")))
                // Add 'public static' modifier
                .withModifiers(CodeModifier.PUBLIC, CodeModifier.STATIC)
                // Set 'void' return type
                .withReturnType(Types.VOID)
                // Set method source
                .withBody(methodSource)
                .build();

        LoadedCodeType<CodeAPITestBytecode> javaType = CodeAPI.getJavaType(CodeAPITestBytecode.class);

        methodSource.add(VariableFactory.variable(javaType, "test", CodeAPI.invokeConstructor(javaType)));
        methodSource.add(Predefined.invokePrintln(
                CodeAPI.accessField(javaType, CodeAPI.accessLocalVariable(javaType, "test"), Types.STRING, "b")
        ));

        // Create method body source
        MutableCodeSource source = new MutableCodeSource();

        IfStatement ifBlock = CodeAPI.ifStatement(
                CodeAPI.check(CodeAPI.accessLocalVariable(Types.OBJECT, "msg"), Operators.NOT_EQUAL_TO, Literals.NULL),
                CodeAPI.source(invokePrintlnMethod(CodeAPI.accessLocalVariable(Types.OBJECT, "msg")))
        );

        source.add(ifBlock);

        // 'Localization' of the field
        CodeType localization = CodeAPI.getJavaType(System.class);

        // Access field in 'localization'
        CodePart variable = CodeAPI.accessField(localization, CodeAPI.accessStatic(), CodeAPI.getJavaType(PrintStream.class), "out");

        // ref local field
        VariableDeclaration cf = VariableDeclarationBuilder.builder()
                .withName("ref")
                // Type is Object
                .withType(Types.OBJECT)
                // Value = variable (System.out)
                .withValue(variable)
                .build();

        source.add(cf);

        LoadedCodeType<IllegalStateException> exceptionType = CodeAPI.getJavaType(IllegalStateException.class);

        source.add(CodeAPI.throwException(
                CodeAPI.invokeConstructor(
                        exceptionType,
                        CodeAPI.constructorTypeSpec(Types.STRING),
                        CollectionsKt.listOf(Literals.STRING("Error"))
                )
        ));

        // Access Local Variable 'msg'
        CodePart msgVar = CodeAPI.accessLocalVariable(Types.OBJECT, "msg");

        // Add Invocation of println method declared in 'System.out' ('variable')
        source.add(CodeAPI.invoke(InvokeType.INVOKE_VIRTUAL, CodeAPI.getJavaType(PrintStream.class), variable,
                "println",
                new TypeSpec(Types.VOID, CollectionsKt.listOf(Types.OBJECT)),
                // with argument 'msgVar' (Method msg parameter)
                CollectionsKt.listOf(msgVar))
        );


        List<CodeType> catchExceptions = Arrays.asList(CodeAPI.getJavaType(IllegalArgumentException.class), CodeAPI.getJavaType(IllegalStateException.class));

        List<CodeType> catchExceptions2 = Arrays.asList(CodeAPI.getJavaType(IOException.class), CodeAPI.getJavaType(ClassNotFoundException.class));

        // Finally block

        CodeSource finallySource = CodeAPI.source(Predefined.invokePrintln(Literals.STRING("Finally!")));

        CatchStatement catchStatement = CodeAPI.catchStatement(catchExceptions, VariableFactory.variable(Types.THROWABLE, "thr"), rethrow("thr"));

        CatchStatement catchStatement2 = CodeAPI.catchStatement(catchExceptions2, VariableFactory.variable(Types.THROWABLE, "tlr"), rethrow("tlr"));

        // Surround 'source' with 'try-catch'
        CodePart surround = CodeAPI.tryStatement(source, Arrays.asList(catchStatement,
                catchStatement2),
                finallySource);

        // Add body to method source
        methodSource.add(surround);

        return codeMethod;
    }

    @Test
    public void codeAPITest() {


        // Create source of 'codeClass'
        MutableCodeSource codeClassSource = new MutableCodeSource();

        // Define a interface
        ClassDeclaration codeClass = ClassDeclarationBuilder.builder()
                .withQualifiedName("github.com." + this.getClass().getSimpleName())
                // Add 'public' modifier
                .withModifiers(CodeModifier.PUBLIC)
                // CodeClass Source
                .withBody(codeClassSource)
                .build();

        // Create a list of CodePart (source)
        CodeSource mySource = CodeAPI.sourceOfParts(codeClass);

        MethodDeclaration method = createMethod();

        // Add method to body
        codeClassSource.add(method);

        BytecodeProcessor generator = new BytecodeProcessor();

        byte[] bytes = generator.gen(mySource)[0].getBytecode();

        ResultSaver.save(this.getClass(), bytes);

        Class<?> define = new BCLoader().define("github.com." + this.getClass().getSimpleName(), bytes);

        try {
            define.newInstance();

            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            MethodHandle println = lookup.findStatic(define, "println", java.lang.invoke.MethodType.methodType(Void.TYPE, Object.class));

            println.invoke((Object) "nano");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static final class BCLoader extends ClassLoader {

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }
    // TODO: LOW LEVEL TASK -> UPDATE

}
