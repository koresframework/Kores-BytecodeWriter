package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.impl.CodeField
import com.github.jonathanxd.codeapi.interfaces.FieldDeclaration
import com.github.jonathanxd.codeapi.interfaces.StaticBlock
import com.github.jonathanxd.codeapi.interfaces.TypeDeclaration
import com.github.jonathanxd.codeapi.util.gen.CodeTypeUtil
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.optional.Require
import com.github.jonathanxd.iutils.type.TypeInfo
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object StaticBlockVisitor : VoidVisitor<StaticBlock, BytecodeClass, Any?> {

    val STATIC_BLOCKS = TypeInfo.a(StaticBlock::class.java).setUnique(true).build()

    fun generate(extraData: MapData, visitorGenerator: VisitorGenerator<*>, cw: ClassWriter, typeDeclaration: TypeDeclaration) {

        val mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)

        val mvData = MVData(mv, mutableListOf())

        mv.visitCode()

        // Variable Initialize

        val all = CodeSourceUtil.find<FieldDeclaration>(
                typeDeclaration.body.orElseThrow(::NullPointerException),
                { codePart ->
                    codePart is CodeField
                            && codePart.modifiers.contains(CodeModifier.STATIC)
                            && codePart.value.isPresent
                }
        ) { codePart -> codePart as CodeField }

        for (codeField in all) {

            val valueOpt = codeField.value

            if (valueOpt.isPresent) {

                val value = valueOpt.get()

                val labeln = Label()

                mv.visitLabel(labeln)

                visitorGenerator.generateTo(value.javaClass, value, extraData, null, mvData)

                val type = codeField.type

                mv.visitFieldInsn(Opcodes.PUTSTATIC, CodeTypeUtil.codeTypeToSimpleAsm(typeDeclaration), codeField.name, CodeTypeUtil.codeTypeToFullAsm(Require.require(type, "Field type required!")))
            }
        }


        // Static Blocks

        val staticBlocks = extraData.getAll(STATIC_BLOCKS)

        for (staticBlock in staticBlocks) {
            staticBlock.body.ifPresent { codeSource -> visitorGenerator.generateTo(CodeSource::class.java, codeSource, extraData, null, mvData) }
        }


        mv.visitInsn(Opcodes.RETURN)
        try {
            mv.visitMaxs(0, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mv.visitEnd()
    }

    override fun voidVisit(t: StaticBlock, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
        extraData.registerData(STATIC_BLOCKS, t)
    }

}