package com.github.jonathanxd.codeapi.gen.bytecode

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.gen.ArrayAppender
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.bytecode.visitor.*
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.interfaces.*
import com.github.jonathanxd.codeapi.interfaces.Annotation
import com.github.jonathanxd.codeapi.literals.Literal
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.option.Options
import com.github.jonathanxd.iutils.type.AbstractTypeInfo

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