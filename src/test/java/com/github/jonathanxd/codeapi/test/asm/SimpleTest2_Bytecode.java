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
import com.github.jonathanxd.codeapi.base.ConstructorDeclaration;
import com.github.jonathanxd.codeapi.base.FieldDeclaration;
import com.github.jonathanxd.codeapi.builder.ClassDeclarationBuilder;
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator;
import com.github.jonathanxd.codeapi.common.CodeArgument;
import com.github.jonathanxd.codeapi.common.CodeModifier;
import com.github.jonathanxd.codeapi.common.CodeParameter;
import com.github.jonathanxd.codeapi.common.InvokeType;
import com.github.jonathanxd.codeapi.factory.ConstructorFactory;
import com.github.jonathanxd.codeapi.factory.FieldFactory;
import com.github.jonathanxd.codeapi.helper.Predefined;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.operator.Operators;
import com.github.jonathanxd.codeapi.type.CodeType;

import org.junit.Test;

import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.EnumSet;

public class SimpleTest2_Bytecode {

    // antes da refatoração: ~890ms
    @Test
    public void simpleTest() throws NoSuchFieldException {
        // Crio um novo 'código-fonte' (não um arquivo, mas sim, uma coleção de instruções, que formam um código fonte)
        MutableCodeSource source = new MutableCodeSource();

        // Cria o 'codigo-fonte' da classe
        MutableCodeSource classSource = new MutableCodeSource();

        // Crio uma classe com nome de SimpleTest2_bytecode
        ClassDeclaration codeClass = ClassDeclarationBuilder.builder()
                // Adiciona o modifier publico
                .withModifiers(CodeModifier.PUBLIC)
                // Defino o nome
                .withName("me.jonathanscripter.codeapi.test.SimpleTest2_bytecode")
                // Defino o super tipo
                .withSuperClass(Types.OBJECT)
                // Defino o corpo
                .withBody(classSource)
                // Construo uma instancia
                .build();


        // Adiciono a classe ao codigo fonte
        source.add(codeClass);


        // Obtem um CodeType a partir de uma classe Java. Obs: Todas classes do CodeAPI são CodeType
        CodeType stringType = CodeAPI.getJavaType(String.class);

        // Cria uma field (campo)
        FieldDeclaration codeField = FieldFactory.field(
                // Adiciona os modificadores public final
                EnumSet.of(CodeModifier.PUBLIC, CodeModifier.FINAL),
                // Define o tipo da field como String
                stringType,
                // Defino o nome
                "myField");


        // Adiciona a field ao codigo fonte da classe
        classSource.add(codeField);


        // Cria um construtor para a classe 'codeClass' que criamos. CodeConstructor recebe CodeType
        // como parametro
        ConstructorDeclaration codeConstructor = ConstructorFactory.constructor(
                // Adiciona o modificador publico
                EnumSet.of(CodeModifier.PUBLIC),

                // Adiciona um parametro 'myField' do tipo String ao construtor
                new CodeParameter[]{new CodeParameter(stringType, "myField")},

                // Define o corpo (codigo fonte) do metodo
                // Classe CodeAPI é usada pelo menos em 70% do código, ela ajuda em tarefas comuns.
                CodeAPI.source(
                        CodeAPI.setThisField(stringType, "myField", CodeAPI.accessLocalVariable(stringType, "myField")),
                        CodeAPI.ifStatement(
                                CodeAPI.check(CodeAPI.accessLocalVariable(stringType, "myField"), Operators.NOT_EQUAL_TO, Literals.NULL),
                                CodeAPI.source(
                                        CodeAPI.invoke(InvokeType.INVOKE_VIRTUAL, PrintStream.class,
                                                CodeAPI.accessStaticField(CodeAPI.getJavaType(System.class), CodeAPI.getJavaType(PrintStream.class), "out"),
                                                "println",
                                                CodeAPI.typeSpec(Types.VOID, Types.STRING),
                                                Collections.singletonList(new CodeArgument(CodeAPI.accessLocalVariable(stringType, "myField")))
                                        )), CodeAPI.source(
                                        CodeAPI.invoke(
                                                InvokeType.INVOKE_VIRTUAL,
                                                PrintStream.class,
                                                CodeAPI.accessStaticField(CodeAPI.getJavaType(System.class), CodeAPI.getJavaType(PrintStream.class), "out"),
                                                "println",
                                                CodeAPI.typeSpec(Types.VOID, Types.STRING),
                                                Collections.singletonList(new CodeArgument(
                                                        CodeAPI.cast(stringType, stringType, Literals.STRING("NULL VALUE"))
                                                )))
                                )),
                        CodeAPI.ifStatement(
                                CodeAPI.ifExprs(CodeAPI.check(Literals.LONG(5894567987L), Operators.LESS_THAN, Literals.LONG(89859845678798L))),
                                CodeAPI.sourceOfParts(Predefined.invokePrintlnStr(Literals.STRING("First < Second"))),
                                CodeAPI.sourceOfParts(Predefined.invokePrintlnStr(Literals.STRING("First >= Second")))
                        )
                ));

        // Adiciona o construtor ao codigo fonte da classe
        classSource.add(codeConstructor);


        // Algumas classes são Singleton, então você não precisa instanciar.
        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        byte[] bytes = bytecodeGenerator.gen(source)[0].getBytecode();

        ResultSaver.save(this.getClass(), bytes);

        BCLoader bcLoader = new BCLoader();

        Class<?> define = bcLoader.define("me.jonathanscripter.codeapi.test.SimpleTest2_bytecode", bytes);

        try {
            Object o = define.getConstructor(String.class).newInstance((String) null);

            MethodHandle getMyField = MethodHandles.publicLookup().findGetter(define, "myField", String.class).bindTo(o);

            System.out.println("Field value = " + (String) getMyField.invoke());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

    }

    private static final class BCLoader extends ClassLoader {

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }

}
