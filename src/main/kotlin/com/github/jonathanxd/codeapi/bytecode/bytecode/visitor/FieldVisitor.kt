package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.interfaces.Annotable
import com.github.jonathanxd.codeapi.interfaces.FieldDeclaration
import com.github.jonathanxd.codeapi.interfaces.VariableDeclaration
import com.github.jonathanxd.codeapi.types.GenericType
import com.github.jonathanxd.codeapi.util.gen.CodeTypeUtil
import com.github.jonathanxd.codeapi.util.gen.ModifierUtil
import com.github.jonathanxd.iutils.data.MapData

object FieldVisitor : Visitor<FieldDeclaration, BytecodeClass, Any?> {

    override fun visit(t: FieldDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?): Array<BytecodeClass> {
        if (additional == null) {

            if (t.value.isPresent) { // Only initialize for fields with default value.
                // This fields will be inspected.
            }
        } else {
            if (additional is MVData) {

                visitorGenerator.generateTo(VariableDeclaration::class.java, t, extraData, null, additional)

                return emptyArray()
            }
        }

        val asm = ModifierUtil.modifiersToAsm(t.modifiers)

        val required = Util.find(TypeVisitor.CLASS_WRITER_REPRESENTATION, extraData, additional)/*extraData.getRequired(TypeVisitor.CLASS_WRITER_REPRESENTATION);*/

        var signature: String? = null

        val type = t.type.orElseThrow(::NullPointerException)

        if (type is GenericType) {
            signature = CodeTypeUtil.toAsm(type)
        }

        val fieldVisitor = required.visitField(asm, t.name, CodeTypeUtil.codeTypeToFullAsm(type), signature, null)

        visitorGenerator.generateTo(Annotable::class.java, t, extraData, null, fieldVisitor)

        return emptyArray()
    }

}