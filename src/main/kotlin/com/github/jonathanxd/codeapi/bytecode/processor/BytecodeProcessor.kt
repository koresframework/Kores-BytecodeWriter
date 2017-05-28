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
package com.github.jonathanxd.codeapi.bytecode.processor

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.base.Annotation
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.CHECK
import com.github.jonathanxd.codeapi.bytecode.VISIT_LINES
import com.github.jonathanxd.codeapi.bytecode.VisitLineType
import com.github.jonathanxd.codeapi.bytecode.common.MethodVisitorHelper
import com.github.jonathanxd.codeapi.bytecode.exception.ClassCheckException
import com.github.jonathanxd.codeapi.bytecode.extra.Dup
import com.github.jonathanxd.codeapi.bytecode.extra.Pop
import com.github.jonathanxd.codeapi.bytecode.processor.visitor.*
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.exception.ProcessingException
import com.github.jonathanxd.codeapi.gen.ArrayAppender
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.literal.Literal
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.sugar.SugarSyntaxProcessor
import com.github.jonathanxd.iutils.data.Data
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.option.Options
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import java.util.function.Consumer

class BytecodeProcessor @JvmOverloads constructor(val sourceFile: (TypeDeclaration) -> String = { "${it.simpleName}.cai" }) //CodeAPI Instructions
    : CodeProcessor<List<BytecodeClass>> {

    override val options: Options = Options()
    private val map = mutableMapOf<Class<*>, Processor<*>>()
    
    init {
        registerProcessor(AccessVisitor, Access::class.java)
        registerProcessor(AnnotableVisitor, Annotable::class.java)
        registerProcessor(TypeVisitor, AnnotationDeclaration::class.java)
        registerProcessor(AnnotationPropertyVisitor, AnnotationProperty::class.java)
        registerProcessor(AnnotationVisitor, Annotation::class.java)
        registerProcessor(ArgumentHolderVisitor, ArgumentHolder::class.java)
        registerProcessor(ArrayAccessVisitor, ArrayAccess::class.java)
        registerProcessor(ArrayConstructVisitor, ArrayConstructor::class.java)
        registerProcessor(ArrayLengthVisitor, ArrayLength::class.java)
        registerProcessor(ArrayLoadVisitor, ArrayLoad::class.java)
        registerProcessor(ArrayStoreVisitor, ArrayStore::class.java)
        registerProcessor(CastVisitor, Cast::class.java)
        registerProcessor(CodeSourceVisitor, CodeSource::class.java)
        registerProcessor(ConcatVisitor, Concat::class.java)
        registerProcessor(MethodDeclarationVisitor, ConstructorDeclaration::class.java)
        registerProcessor(ControlFlowVisitor, ControlFlow::class.java)
        registerProcessor(EnumVisitor, EnumDeclaration::class.java)
        registerProcessor(FieldAccessVisitor, FieldAccess::class.java)
        registerProcessor(FieldDefinitionVisitor, FieldDefinition::class.java)
        registerProcessor(FieldVisitor, FieldDeclaration::class.java)
        registerProcessor(ForEachVisitor, ForEachStatement::class.java)
        registerProcessor(ForIVisitor, ForStatement::class.java)
        registerProcessor(IfExprVisitor, IfExpr::class.java)
        registerProcessor(IfStatementVisitor, IfStatement::class.java)
        registerProcessor(InstanceOfVisitor, InstanceOfCheck::class.java)
        registerProcessor(InstructionCodePart.InstructionCodePartVisitor, InstructionCodePart::class.java)
        registerProcessor(LabelVisitor, Label::class.java)
        registerProcessor(LiteralVisitor, Literal::class.java)
        registerProcessor(MethodDeclarationVisitor, MethodDeclaration::class.java)
        registerProcessor(MethodFragmentVisitor, MethodFragment::class.java)
        registerProcessor(MethodInvocationVisitor, MethodInvocation::class.java)
        registerProcessor(OperateVisitor, Operate::class.java)
        registerProcessor(ReturnVisitor, Return::class.java)
        registerProcessor(StaticBlockVisitor, StaticBlock::class.java)
        registerProcessor(SwitchVisitor, SwitchStatement::class.java)
        registerProcessor(ThrowExceptionVisitor, ThrowException::class.java)
        registerProcessor(TryStatementVisitor, TryStatement::class.java)
        registerProcessor(TryWithResourcesVisitor, TryWithResources::class.java)
        registerProcessor(TypeVisitor, TypeDeclaration::class.java)
        registerProcessor(VariableAccessVisitor, VariableAccess::class.java)
        registerProcessor(VariableDeclarationVisitor, VariableDeclaration::class.java)
        registerProcessor(VariableDefinitionVisitor, VariableDefinition::class.java)
        registerProcessor(WhileStatementVisitor, WhileStatement::class.java)

        registerProcessor(TypeVisitor, ClassDeclaration::class.java)
        registerProcessor(TypeVisitor, InterfaceDeclaration::class.java)

        // Extra
        registerProcessor(DupVisitor, Dup::class.java)
        registerProcessor(PopVisitor, Pop::class.java)
    }

    override fun <T> registerProcessor(processor: Processor<T>, type: Class<T>) {
        this.map[type] = processor
    }

    override fun <T> registerSugarSyntaxProcessor(sugarSyntaxProcessor: SugarSyntaxProcessor<T>, type: Class<T>) {
        this.map[type] = sugarSyntaxProcessor
    }
    
    

    override fun createData(): TypedData {
        val data = TypedData()

        data.set(SOURCE_FILE_FUNCTION, sourceFile)
        data.set(ConstantDatas.BYTECODE_CLASS_LIST, mutableListOf<BytecodeClass>())

        return data
    }

    override fun <T> process(type: Class<out T>, part: T, data: TypedData): List<BytecodeClass> {
        val mvDataOpt = data.getOptionalAs<MethodVisitorHelper>(ConstantDatas.METHOD_VISITOR_DATA)

        if (this.options.get(VISIT_LINES) == VisitLineType.INCREMENTAL
                && mvDataOpt.isPresent) {

            val line = data.getOptionalAs<Int>(LINE).let {
                if (!it.isPresent) {
                    data.set(LINE, 1)
                    0
                } else {
                    val get = it.get()
                    data.set(LINE, get + 1)
                    get
                }
            }

            val label = org.objectweb.asm.Label()

            val mvData = mvDataOpt.get()

            mvData.methodVisitor.visitLabel(label)

            mvData.methodVisitor.visitLineNumber(line, label)
        }

        @Suppress("UNCHECKED_CAST")
        val processor = this.map[type] as? Processor<T> ?: throw IllegalArgumentException("Cannot find processor of type '$type' and part '$part'. Data: {$data}")

        processor.process(part, data, this)
        processor.endProcess(part, data, this)

    }

    override fun <C : CodePart> generateTo(partClass: Class<out C>, codePart: C, extraData: Data, consumer: Consumer<Array<out BytecodeClass>>?, additional: Any?): Array<out BytecodeClass> {

        if (this.options.get(VISIT_LINES) == VisitLineType.INCREMENTAL
                && additional != null
                && additional is MethodVisitorHelper) {
            val line = extraData.getOptionalAs<Int>(LINE).let {
                if (!it.isPresent) {
                    extraData.set(LINE, 1)
                    0
                } else {
                    val get = it.get()
                    extraData.set(LINE, get + 1)
                    get
                }
            }

            val lbl = org.objectweb.asm.Label()

            additional.methodVisitor.visitLabel(lbl)

            additional.methodVisitor.visitLineNumber(line, lbl)

        }
        try {
            return super.generateTo(partClass, codePart, extraData, consumer, additional).let {
                check(it)
                it
            }
        }catch (e: Throwable) {
            throw e
        }
    }

    private fun check(classes: Array<out BytecodeClass>) {
        if (this.options[CHECK]) {
            if (classes.isNotEmpty()) {
                classes.forEach {
                    val bytecode = it.bytecode
                    if (bytecode.isNotEmpty()) {
                        try {
                            ClassReader(bytecode).accept(CheckClassAdapter(ClassNode(), true), 0)
                        } catch (t: Throwable) {
                            throw ClassCheckException("Failed to check bytecode of class ${it.type.qualifiedName}", t, classes, it)
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