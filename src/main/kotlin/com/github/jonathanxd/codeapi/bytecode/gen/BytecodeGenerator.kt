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
package com.github.jonathanxd.codeapi.bytecode.gen

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.base.Annotation
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.CHECK
import com.github.jonathanxd.codeapi.bytecode.VISIT_LINES
import com.github.jonathanxd.codeapi.bytecode.VisitLineType
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.gen.visitor.*
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.exception.ProcessingException
import com.github.jonathanxd.codeapi.gen.ArrayAppender
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.literal.Literal
import com.github.jonathanxd.iutils.option.Options
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import java.util.function.Consumer

class BytecodeGenerator @JvmOverloads constructor(val sourceFile: (TypeDeclaration) -> String = { "${it.simpleName}.cai" }) //CodeAPI Instructions
    : VisitorGenerator<BytecodeClass>() {

    private val options_ = Options()
    override val emptyArray: Array<out BytecodeClass> = kotlin.emptyArray()

    init {
        addVisitor(Access::class.java, AccessVisitor)
        addVisitor(Annotable::class.java, AnnotableVisitor)
        addVisitor(AnnotationDeclaration::class.java, AnnotationDeclarationVisitor)
        addVisitor(AnnotationProperty::class.java, AnnotationPropertyVisitor)
        addVisitor(Annotation::class.java, AnnotationVisitor)
        addVisitor(ArgumentHolder::class.java, ArgumentHolderVisitor)
        addVisitor(ArrayAccess::class.java, ArrayAccessVisitor)
        addVisitor(ArrayConstructor::class.java, ArrayConstructVisitor)
        addVisitor(ArrayLength::class.java, ArrayLengthVisitor)
        addVisitor(ArrayLoad::class.java, ArrayLoadVisitor)
        addVisitor(ArrayStore::class.java, ArrayStoreVisitor)
        addVisitor(Cast::class.java, CastVisitor)
        addVisitor(CodeSource::class.java, CodeSourceVisitor)
        addVisitor(Concat::class.java, ConcatVisitor)
        addVisitor(ConstructorDeclaration::class.java, ConstructorVisitor)
        addVisitor(ControlFlow::class.java, ControlFlowVisitor)
        addVisitor(EnumDeclaration::class.java, EnumVisitor)
        addVisitor(FieldAccess::class.java, FieldAccessVisitor)
        addVisitor(FieldDefinition::class.java, FieldDefinitionVisitor)
        addVisitor(FieldDeclaration::class.java, FieldVisitor)
        addVisitor(ForEachStatement::class.java, ForEachVisitor)
        addVisitor(ForStatement::class.java, ForIVisitor)
        addVisitor(IfExpr::class.java, IfExprVisitor)
        addVisitor(IfStatement::class.java, IfStatementVisitor)
        addVisitor(InstanceOfCheck::class.java, InstanceOfVisitor)
        addVisitor(InstructionCodePart::class.java, InstructionCodePart.InstructionCodePartVisitor)
        addVisitor(Label::class.java, LabelVisitor)
        addVisitor(Literal::class.java, LiteralVisitor)
        addVisitor(MethodDeclaration::class.java, MethodDeclarationVisitor)
        addVisitor(MethodFragment::class.java, MethodFragmentVisitor)
        addVisitor(MethodInvocation::class.java, MethodInvocationVisitor)
        addVisitor(Operate::class.java, OperateVisitor)
        addVisitor(Return::class.java, ReturnVisitor)
        addVisitor(StaticBlock::class.java, StaticBlockVisitor)
        addVisitor(SwitchStatement::class.java, SwitchVisitor)
        addVisitor(ThrowException::class.java, ThrowExceptionVisitor)
        addVisitor(TryStatement::class.java, TryStatementVisitor)
        addVisitor(TryWithResources::class.java, TryWithResourcesVisitor)
        addVisitor(TypeDeclaration::class.java, TypeVisitor)
        addVisitor(VariableAccess::class.java, VariableAccessVisitor)
        addVisitor(VariableDeclaration::class.java, VariableDeclarationVisitor)
        addVisitor(VariableDefinition::class.java, VariableDefinitionVisitor)
        addVisitor(WhileStatement::class.java, WhileStatementVisitor)

        addUncheckedVisitor(ClassDeclaration::class.java, TypeVisitor)
        addUncheckedVisitor(InterfaceDeclaration::class.java, TypeVisitor)

    }

    override val options: Options = this.options_

    override fun makeData(): Data {
        val data = Data()

        data.registerData(SOURCE_FILE_FUNCTION, sourceFile)

        return data
    }

    override fun createAppender(): ArrayAppender<BytecodeClass> = ByteAppender()


    override fun <C : CodePart> generateTo(partClass: Class<out C>, codePart: C, extraData: Data, consumer: Consumer<Array<out BytecodeClass>>?, additional: Any?): Array<out BytecodeClass> {

        if (this.options.get(VISIT_LINES).get() == VisitLineType.INCREMENTAL
                && additional != null
                && additional is MVData) {
            val line = extraData.getOptional<Int>(LINE).let {
                if (!it.isPresent) {
                    extraData.registerData(LINE, 1)
                    0
                } else {
                    val get = it.get()
                    extraData.registerData(LINE, get + 1)
                    get
                }
            }

            val lbl = org.objectweb.asm.Label()

            additional.methodVisitor.visitLabel(lbl)

            additional.methodVisitor.visitLineNumber(line, lbl)

        }

        return super.generateTo(partClass, codePart, extraData, consumer, additional).let {
            check(it)
            it
        }
    }

    private fun check(classes: Array<out BytecodeClass>) {
        if (this.options[CHECK].get()) {
            if (classes.isNotEmpty()) {
                classes.forEach {
                    val bytecode = it.bytecode
                    if (bytecode.isNotEmpty()) {
                        try {
                            ClassReader(bytecode).accept(CheckClassAdapter(ClassNode(), true), 0)
                        } catch (t: Throwable) {
                            throw ProcessingException("Failed to check bytecode of class ${it.type.qualifiedName}", t)
                        }
                    }
                }
            }
        }
    }

    companion object {
        //(TypeDeclaration) -> String
        @JvmStatic
        val SOURCE_FILE_FUNCTION = "SOURCE_FILE_FUNCTION"

        // Int
        @JvmField
        val LINE = "LINE_POSITION"
    }

    private class ByteAppender internal constructor() : ArrayAppender<BytecodeClass>() {

        private val bytecodeClassList = java.util.ArrayList<BytecodeClass>()

        override fun add(elem: Array<out BytecodeClass>) {
            for (bytecodeClass in elem) {
                bytecodeClassList.add(bytecodeClass)
            }

        }

        override fun get(): Array<BytecodeClass> {
            return this.bytecodeClassList.toTypedArray()
        }
    }
}