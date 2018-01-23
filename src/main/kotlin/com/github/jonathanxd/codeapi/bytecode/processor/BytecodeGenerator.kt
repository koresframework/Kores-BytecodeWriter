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
package com.github.jonathanxd.codeapi.bytecode.processor

import com.github.jonathanxd.codeapi.*
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.base.Annotation
import com.github.jonathanxd.codeapi.bytecode.*
import com.github.jonathanxd.codeapi.bytecode.exception.ClassCheckException
import com.github.jonathanxd.codeapi.bytecode.extra.Dup
import com.github.jonathanxd.codeapi.bytecode.extra.Pop
import com.github.jonathanxd.codeapi.bytecode.post.Processor
import com.github.jonathanxd.codeapi.bytecode.processor.processors.*
import com.github.jonathanxd.codeapi.bytecode.util.ASM_API
import com.github.jonathanxd.codeapi.common.Stack
import com.github.jonathanxd.codeapi.literal.Literal
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.processor.AbstractProcessorManager
import com.github.jonathanxd.codeapi.processor.ValidatorManager
import com.github.jonathanxd.codeapi.processor.VoidValidatorManager
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.option.Options
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter

class BytecodeGenerator @JvmOverloads constructor(
    val sourceFile: (Named) -> String = {
        when (it) {
            is TypeDeclaration -> "${Util.getOwner(it).simpleName}.cai"
            is ModuleDeclaration -> "module-info.cai" // Maybe module-info_${it.name}.cai ?
            else -> it.name
        }
    }
) //CodeAPI Instructions
    : AbstractProcessorManager<List<BytecodeClass>>() {

    override val options: Options = Options()

    override val validatorManager: ValidatorManager = VoidValidatorManager

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

        registerProcessorOfTypes(
            CodeSourceProcessor, arrayOf(
                CodeSource::class.java,
                ArrayCodeSource::class.java,
                CodeSourceView::class.java,
                ListCodeSource::class.java,
                MutableCodeSource::class.java
            )
        )

        registerProcessor(ConcatProcessor, Concat::class.java)
        registerProcessor(ControlFlowProcessor, ControlFlow::class.java)
        registerProcessor(ConstructorsHolderProcessor, ConstructorsHolder::class.java)
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
        registerProcessor(
            InstructionCodePart.InstructionCodePartVisitor,
            InstructionCodePart::class.java
        )

        registerProcessorOfTypes(
            InvokeDynamicProcessor, arrayOf(
                InvokeDynamicBase::class.java,
                InvokeDynamicBase.LambdaMethodRefBase::class.java,
                InvokeDynamicBase.LambdaLocalCodeBase::class.java,
                InvokeDynamic::class.java,
                InvokeDynamic.LambdaMethodRef::class.java,
                InvokeDynamic.LambdaLocalCode::class.java
            )
        )

        registerProcessor(LabelProcessor, Label::class.java)

        registerProcessorOfTypes(
            LineProcessor, arrayOf(
                Line::class.java,
                Line.NormalLine::class.java,
                Line.TypedLine::class.java
            )
        )

        registerProcessor(LiteralProcessor, Literal::class.java)
        registerProcessor(MethodDeclarationProcessor, MethodDeclaration::class.java)
        registerProcessor(LocalCodeProcessor, LocalCode::class.java)
        registerProcessor(MethodDeclarationProcessor, ConstructorDeclaration::class.java)
        registerProcessor(MethodInvocationProcessor, MethodInvocation::class.java)
        registerProcessor(ModuleDeclarationProcessor, ModuleDeclaration::class.java)
        registerProcessor(NewProcessor, New::class.java)
        registerProcessor(OperateProcessor, Operate::class.java)
        registerProcessor(ReturnProcessor, Return::class.java)
        registerProcessor(ScopeAccessProcessor, ScopeAccess::class.java)
        registerProcessor(StaticBlockProcessor, StaticBlock::class.java)

        registerProcessor(SwitchProcessor, SwitchStatement::class.java)
        registerProcessor(
            SwitchProcessor.SwitchMarkerProcessor,
            SwitchProcessor.SwitchMarker::class.java
        )

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
        registerProcessorOfTypes(
            LiteralProcessor, arrayOf(
                Literals.ClassLiteral::class.java,
                Literals.ByteLiteral::class.java,
                Literals.ShortLiteral::class.java,
                Literals.IntLiteral::class.java,
                Literals.BoolLiteral::class.java,
                Literals.FloatLiteral::class.java,
                Literals.DoubleLiteral::class.java,
                Literals.CharLiteral::class.java,
                Literals.StringLiteral::class.java
            )
        )
    }

    // Will not be called because here we use void validator.
    override fun printFailMessage(message: String) {
    }

    override fun createData(): TypedData {
        val data = TypedData()

        SOURCE_FILE_FUNCTION.set(data, sourceFile)
        BYTECODE_CLASS_LIST.set(data, mutableListOf())

        return data
    }

    override fun process(part: Any): List<BytecodeClass> {
        return super.process(LineProcessor.visitLineICT(part, this))
    }

    override fun <T> process(type: Class<out T>, part: T, data: TypedData): List<BytecodeClass> {
        LineProcessor.visitLineIC(this, data)

        val processor = getProcessorOf(type, part, data)

        processor.process(part, data, this)
        processor.endProcess(part, data, this)

        return getFinalValue(data)
    }

    // Called by version above.
    override fun getFinalValue(data: TypedData): List<BytecodeClass> {
        val classes = BYTECODE_CLASS_LIST.getOrNull(data) ?: mutableListOf()

        val checkClasses = if (this.options[POST_PROCESSING]) {
            classes.map {
                BytecodeClass(
                    it.declaration,
                    try {
                        Processor(
                            ASM_API,
                            this.options[POST_PROCESSORS],
                            this.options[POST_PROCESSING_LOOPS]
                        )
                            .process(it.bytecode)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        it.bytecode
                    }
                )
            }
        } else classes

        if (checkClasses.isNotEmpty() && this.options.get(CHECK))
            check(checkClasses)

        return checkClasses
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
                            throw ClassCheckException(
                                "Failed to check bytecode of declaration ${it.declaration.name}",
                                t,
                                classes,
                                it
                            )
                        }
                    }
                }
            }
        }
    }

}