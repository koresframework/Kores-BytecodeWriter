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

import com.github.jonathanxd.codeapi.*
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.base.Annotation
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.CHECK
import com.github.jonathanxd.codeapi.bytecode.VISIT_LINES
import com.github.jonathanxd.codeapi.bytecode.VisitLineType
import com.github.jonathanxd.codeapi.bytecode.exception.ClassCheckException
import com.github.jonathanxd.codeapi.bytecode.extra.Dup
import com.github.jonathanxd.codeapi.bytecode.extra.Pop
import com.github.jonathanxd.codeapi.bytecode.processor.processors.*
import com.github.jonathanxd.codeapi.common.Stack
import com.github.jonathanxd.codeapi.literal.Literal
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.codeapi.processor.CodeValidator
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.VoidValidator
import com.github.jonathanxd.codeapi.sugar.SugarSyntaxProcessor
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.option.Options
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter

class BytecodeProcessor @JvmOverloads constructor(val sourceFile: (TypeDeclaration) -> String = { "${it.simpleName}.cai" }) //CodeAPI Instructions
    : CodeProcessor<List<BytecodeClass>> {

    override val options: Options = Options()
    private val map = mutableMapOf<Class<*>, Processor<*>>()

    override val validator: CodeValidator = VoidValidator

    init {
        registerProcessor(AccessProcessor, Access::class.java)
        registerProcessor(AnnotableProcessor, Annotable::class.java)
        registerProcessor(AnnotationProcessor, Annotation::class.java)
        registerProcessor(AnnotationPropertyProcessor, AnnotationProperty::class.java)
        registerProcessor(ArgumentsHolderProcessor, ArgumentsHolder::class.java)
        registerProcessor(ArrayAccessProcessor, ArrayAccess::class.java)
        registerProcessor(ArrayConstructProcessor, ArrayConstructor::class.java)
        registerProcessor(ArrayLengthProcessor, ArrayLength::class.java)
        registerProcessor(ArrayLoadProcessor, ArrayLoad::class.java)
        registerProcessor(ArrayStoreProcessor, ArrayStore::class.java)
        registerProcessor(CastProcessor, Cast::class.java)

        registerProcessorOfTypes(CodeSourceProcessor, arrayOf(
                CodeSource::class.java,
                ArrayCodeSource::class.java,
                CodeSourceView::class.java,
                ListCodeSource::class.java,
                MutableCodeSource::class.java
        ))

        registerProcessor(ConcatProcessor, Concat::class.java)
        registerProcessor(ControlFlowProcessor, ControlFlow::class.java)
        registerProcessor(ElementsHolderProcessor, ElementsHolder::class.java)
        registerProcessor(InnerTypesHolderProcessor, InnerTypesHolder::class.java)
        registerProcessor(EnumDeclarationProcessor, EnumDeclaration::class.java)
        registerProcessor(FieldAccessProcessor, FieldAccess::class.java)
        registerProcessor(FieldDeclarationProcessor, FieldDeclaration::class.java)
        registerProcessor(FieldDefinitionProcessor, FieldDefinition::class.java)
        registerProcessor(ForEachProcessor, ForEachStatement::class.java)
        registerProcessor(ForStatementProcessor, ForStatement::class.java)
        registerProcessor(IfExprProcessor, IfExpr::class.java)
        registerProcessor(IfStatementProcessor, IfStatement::class.java)
        registerProcessor(InstanceOfProcessor, InstanceOfCheck::class.java)
        registerProcessor(InstructionCodePart.InstructionCodePartVisitor, InstructionCodePart::class.java)

        registerProcessorOfTypes(InvokeDynamicProcessor, arrayOf(
                InvokeDynamicBase::class.java,
                InvokeDynamicBase.LambdaMethodRefBase::class.java,
                InvokeDynamicBase.LambdaLocalCodeBase::class.java,
                InvokeDynamic::class.java,
                InvokeDynamic.LambdaMethodRef::class.java,
                InvokeDynamic.LambdaLocalCode::class.java
        ))

        registerProcessor(LabelProcessor, Label::class.java)
        registerProcessor(LiteralProcessor, Literal::class.java)
        registerProcessor(MethodDeclarationProcessor, MethodDeclaration::class.java)
        registerProcessor(LocalCodeProcessor, LocalCode::class.java)
        registerProcessor(MethodDeclarationProcessor, ConstructorDeclaration::class.java)
        registerProcessor(MethodInvocationProcessor, MethodInvocation::class.java)
        registerProcessor(NewProcessor, New::class.java)
        registerProcessor(OperateProcessor, Operate::class.java)
        registerProcessor(ReturnProcessor, Return::class.java)
        registerProcessor(StaticBlockProcessor, StaticBlock::class.java)

        registerProcessor(SwitchProcessor, SwitchStatement::class.java)
        registerProcessor(SwitchProcessor.SwitchMarkerProcessor, SwitchProcessor.SwitchMarker::class.java)

        registerProcessor(SynchronizedProcessor, Synchronized::class.java)

        registerProcessor(ThrowExceptionProcessor, ThrowException::class.java)
        registerProcessor(TryStatementProcessor, TryStatement::class.java)
        registerProcessor(TryWithResourcesProcessor, TryWithResources::class.java)
        registerProcessor(TypeDeclarationProcessor, TypeDeclaration::class.java)

        registerProcessor(VariableAccessProcessor, VariableAccess::class.java)
        registerProcessor(VariableDeclarationProcessor, VariableDeclaration::class.java)
        registerProcessor(VariableDefinitionProcessor, VariableDefinition::class.java)
        registerProcessor(WhileStatementProcessor, WhileStatement::class.java)

        registerProcessor(TypeDeclarationProcessor, ClassDeclaration::class.java)
        registerProcessor(TypeDeclarationProcessor, InterfaceDeclaration::class.java)
        registerProcessor(TypeDeclarationProcessor, AnnotationDeclaration::class.java)

        // Extra
        registerProcessor(StackProcessor, Stack::class.java)
        registerProcessor(DupProcessor, Dup::class.java)
        registerProcessor(PopProcessor, Pop::class.java)

        // Literals
        registerProcessorOfTypes(LiteralProcessor, arrayOf(
                Literals.ClassLiteral::class.java,
                Literals.ByteLiteral::class.java,
                Literals.ShortLiteral::class.java,
                Literals.IntLiteral::class.java,
                Literals.BoolLiteral::class.java,
                Literals.FloatLiteral::class.java,
                Literals.DoubleLiteral::class.java,
                Literals.CharLiteral::class.java,
                Literals.StringLiteral::class.java
        ))
    }

    inline fun <reified T> registerProcessorOfTypes(processor: Processor<T>, types: Array<Class<out T>>) {
        types.forEach {
            registerProcessor(processor, it)
        }
    }

    override fun <T> registerProcessor(processor: Processor<T>, type: Class<T>) {
        this.map[type] = processor
    }

    override fun <T> registerSugarSyntaxProcessor(sugarSyntaxProcessor: SugarSyntaxProcessor<T>, type: Class<T>) {
        this.map[type] = sugarSyntaxProcessor
    }


    override fun createData(): TypedData {
        val data = TypedData()

        SOURCE_FILE_FUNCTION.set(data, sourceFile)
        BYTECODE_CLASS_LIST.set(data, mutableListOf())

        return data
    }

    override fun <T> process(type: Class<out T>, part: T, data: TypedData): List<BytecodeClass> {
        val mvDataOpt = METHOD_VISITOR.getOrNull(data)

        if (this.options.get(VISIT_LINES) == VisitLineType.INCREMENTAL
                && mvDataOpt != null) {

            val line = LINE.let {
                if (!it.contains(data)) {
                    it.set(data, 1)
                    0
                } else {
                    val get = it.require(data)
                    it.set(data, get + 1)
                    get
                }
            }

            val label = org.objectweb.asm.Label()

            mvDataOpt.methodVisitor.visitLabel(label)

            mvDataOpt.methodVisitor.visitLineNumber(line, label)
        }

        val searchType = if(this.map.containsKey(type))
            type
        else if(type.superclass != Any::class.java && type.interfaces.isEmpty())
            type.superclass
        else if(type.interfaces.size == 1)
            type.interfaces.single()
        else type

        @Suppress("UNCHECKED_CAST")
        val processor = this.map[searchType] as? Processor<T>
                ?: throw IllegalArgumentException("Cannot find processor of type '$type' (searchType: '$searchType') and part '$part'. Data: {$data}")

        processor.process(part, data, this)
        processor.endProcess(part, data, this)

        val classes = BYTECODE_CLASS_LIST.getOrNull(data) ?: mutableListOf()

        if(classes.isNotEmpty() && this.options.get(CHECK))
            check(classes)

        return classes
    }

    private fun check(classes: List<BytecodeClass>) {
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

}