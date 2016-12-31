/*
 *      CodeAPI-BytecodeWriter - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2016 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
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
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.VISIT_LINES
import com.github.jonathanxd.codeapi.bytecode.LINE
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.gen.visitor.*
import com.github.jonathanxd.codeapi.gen.ArrayAppender
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.interfaces.*
import com.github.jonathanxd.codeapi.interfaces.Annotation
import com.github.jonathanxd.codeapi.literals.Literal
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.option.Options
import com.github.jonathanxd.iutils.type.AbstractTypeInfo
import org.objectweb.asm.Label
import java.util.function.Consumer

class BytecodeGenerator @JvmOverloads constructor(val sourceFile: (TypeDeclaration) -> String = { "${it.simpleName}.cai" }) //CodeAPI Instructions
    : VisitorGenerator<BytecodeClass>() {

    private val options_ = Options()

    init {
        addVisitor(PackageDeclaration::class.java, PackageVisitor)
        addVisitor(TypeDeclaration::class.java, TypeVisitor)
        addUncheckedVisitor(ClassDeclaration::class.java, TypeVisitor)
        addVisitor(FieldDeclaration::class.java, FieldVisitor)
        addVisitor(CodeSource::class.java, CodeSourceVisitor)
        addVisitor(ConstructorDeclaration::class.java, ConstructorVisitor)
        addVisitor(Literal::class.java, LiteralVisitor)
        addVisitor(MethodInvocation::class.java, MethodInvocationVisitor)
        addVisitor(VariableAccess::class.java, VariableAccessVisitor)
        addVisitor(Argumenterizable::class.java, ArgumenterizabeVisitor)
        addVisitor(MethodDeclaration::class.java, CodeMethodVisitor)
        addVisitor(Access::class.java, AccessVisitor)
        addVisitor(TryBlock::class.java, TryBlockVisitor())
        addVisitor(IfBlock::class.java, IfBlockVisitor)
        addVisitor(Return::class.java, ReturnVisitor)
        addVisitor(VariableDeclaration::class.java, VariableDeclarationVisitor)
        addVisitor(ThrowException::class.java, ThrowExceptionVisitor)
        addVisitor(Casted::class.java, CastedVisitor)
        addVisitor(Operate::class.java, OperateVisitor)
        addVisitor(VariableOperate::class.java, VariableOperateVisitor)
        addVisitor(InstructionCodePart::class.java, InstructionCodePart.InstructionCodePartVisitor)
        addVisitor(WhileBlock::class.java, WhileVisitor)
        addVisitor(DoWhileBlock::class.java, DoWhileVisitor)
        addVisitor(ForBlock::class.java, ForIVisitor)
        addVisitor(StaticBlock::class.java, StaticBlockVisitor)
        addVisitor(ArrayConstructor::class.java, ArrayConstructVisitor)
        addVisitor(ArrayStore::class.java, ArrayStoreVisitor)
        addVisitor(ArrayLoad::class.java, ArrayLoadVisitor)
        addVisitor(ArrayAccess::class.java, ArrayAccessVisitor)
        addVisitor(ArrayLength::class.java, ArrayLengthVisitor)
        addVisitor(TagLine::class.java, TagLineVisitor)
        addVisitor(ForEachBlock::class.java, ForEachVisitor)
        addVisitor(MethodFragment::class.java, MethodFragmentVisitor)
        addVisitor(Annotable::class.java, AnnotableVisitor)
        addVisitor(Annotation::class.java, AnnotationVisitor)
        addVisitor(TryWithResources::class.java, TryWithResourcesVisitor())
        addVisitor(InstanceOf::class.java, InstanceOfVisitor)
        addVisitor(IfExpr::class.java, IfExprVisitor) /* Sugar Syntax to a IfBlock */
        addVisitor(Break::class.java, BreakVisitor)
        addVisitor(Continue::class.java, ContinueVisitor)
        addVisitor(Switch::class.java, SwitchVisitor)

        addVisitor(EnumDeclaration::class.java, EnumVisitor)

        addVisitor(AnnotationDeclaration::class.java, TypeAnnotationVisitor)
        addVisitor(AnnotationProperty::class.java, AnnotationPropertyVisitor)

        addVisitor(Concat::class.java, ConcatVisitor)
    }

    override fun getOptions(): Options = this.options_

    override fun makeData(): MapData {
        val data = MapData()

        data.registerData(SOURCE_FILE_FUNCTION, sourceFile)

        return data
    }

    override fun createAppender(): ArrayAppender<BytecodeClass> = ByteAppender()

    override fun generateTo(partClass: Class<out CodePart>?, codePart: CodePart?, extraData: MapData?, consumer: Consumer<Array<BytecodeClass>>?, additional: Any?): Array<BytecodeClass>? {

        if(extraData != null
                && this.options.getOrElse(VISIT_LINES, false)
                && additional != null
                && additional is MVData) {
            val line = extraData.getOptional(LINE).let {
                if(!it.isPresent) {
                    extraData.registerData(LINE, 1)
                    0
                } else {
                    val get = it.get()
                    extraData.registerData(LINE, get + 1)
                    get
                }
            }

            val lbl = Label()

            additional.methodVisitor.visitLabel(lbl)

            additional.methodVisitor.visitLineNumber(line, lbl)

        }

        return super.generateTo(partClass, codePart, extraData, consumer, additional)
    }

    companion object {
        @JvmStatic
        val SOURCE_FILE_FUNCTION = object: AbstractTypeInfo<(TypeDeclaration) -> String>(true) {}
    }

    private class ByteAppender internal constructor() : ArrayAppender<BytecodeClass>() {

        private val bytecodeClassList = java.util.ArrayList<BytecodeClass>()

        override fun add(elem: Array<BytecodeClass>?) {
            if (elem == null)
                return

            for (bytecodeClass in elem) {
                bytecodeClassList.add(bytecodeClass)
            }

        }

        override fun get(): Array<BytecodeClass> {
            return this.bytecodeClassList.toTypedArray()
        }
    }
}