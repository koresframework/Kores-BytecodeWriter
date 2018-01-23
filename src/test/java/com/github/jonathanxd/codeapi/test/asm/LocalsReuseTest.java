/*
 *      CodeAPI-BytecodeWriter - Translates CodeAPI Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
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
package com.github.jonathanxd.codeapi.test.asm;

import com.github.jonathanxd.codeapi.CodeSource;
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.CodeModifier;
import com.github.jonathanxd.codeapi.base.MethodDeclaration;
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass;
import com.github.jonathanxd.codeapi.bytecode.BytecodeOptions;
import com.github.jonathanxd.codeapi.bytecode.VisitLineType;
import com.github.jonathanxd.codeapi.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.codeapi.bytecode.util.ConstsKt;
import com.github.jonathanxd.codeapi.factory.Factories;
import com.github.jonathanxd.codeapi.factory.VariableFactory;
import com.github.jonathanxd.codeapi.literal.Literals;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class LocalsReuseTest {

    @SuppressWarnings("unchecked")
    @Test
    public void test() {
        ClassDeclaration declaration = ClassDeclaration.Builder.builder()
                .modifiers(CodeModifier.PUBLIC)
                .qualifiedName("codeapi.LocalsReuse")
                .methods(
                        MethodDeclaration.Builder.builder()
                                .modifiers(CodeModifier.PUBLIC)
                                .name("test")
                                .returnType(Types.INT)
                                .parameters(Factories.parameter(Types.BOOLEAN, "bool"))
                                .body(CodeSource.fromPart(
                                        Factories.ifStatement(Factories.checkTrue(Factories.accessVariable(Types.BOOLEAN, "bool")),
                                                CodeSource.fromVarArgs(
                                                        VariableFactory.variable(Types.INT, "i", Literals.INT(10)),
                                                        Factories.returnValue(Types.INT, Factories.accessVariable(Types.INT, "i"))
                                                ),
                                                CodeSource.fromVarArgs(
                                                        VariableFactory.variable(Types.INT, "i", Literals.INT(17)),
                                                        Factories.returnValue(Types.INT, Factories.accessVariable(Types.INT, "i"))
                                                ))
                                ))
                                .build()

                )
                .build();

        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        bytecodeGenerator.getOptions().set(BytecodeOptions.VISIT_LINES, VisitLineType.FOLLOW_CODE_SOURCE);

        List<? extends BytecodeClass> gen = bytecodeGenerator.process(declaration);

        ResultSaver.save(this.getClass(), gen);

        ClassReader cr = new ClassReader(gen.get(0).getBytecode());

        ClassNode cn = new ClassNode(ConstsKt.ASM_API);
        cr.accept(cn, 0);

        MethodNode node = ((List<MethodNode>) cn.methods).stream().filter(methodNode -> methodNode.name.equals("test")).findFirst().orElseThrow(NullPointerException::new);

        Assert.assertEquals("Locals reuse", 3, node.maxLocals);
    }

}
