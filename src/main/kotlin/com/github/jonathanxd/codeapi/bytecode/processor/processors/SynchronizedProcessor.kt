package com.github.jonathanxd.codeapi.bytecode.processor.processors

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.Synchronized
import com.github.jonathanxd.codeapi.base.TryStatement
import com.github.jonathanxd.codeapi.base.VariableDeclaration
import com.github.jonathanxd.codeapi.bytecode.extra.Dup
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.processor.require
import com.github.jonathanxd.codeapi.factory.accessVariable
import com.github.jonathanxd.codeapi.factory.variable
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.util.typeOrNull
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Opcodes

object SynchronizedProcessor : Processor<Synchronized> {

    override fun process(part: Synchronized, data: TypedData, codeProcessor: CodeProcessor<*>) {

        val mvHelper = METHOD_VISITOR.require(data)
        val visitor = mvHelper.methodVisitor

        val name = mvHelper.getUniqueVariableName("\$sync_var#0")
        val type = part.instruction.typeOrNull ?: Types.OBJECT

        val variable = variable(type, name, Dup(part.instruction))

        codeProcessor.process(VariableDeclaration::class.java, variable, data)

        visitor.visitInsn(Opcodes.MONITORENTER)

        TryStatement.Builder.builder()
                .body(part.body)
                .finallyStatement(CodeSource.fromVarArgs(
                        accessVariable(variable),
                        InstructionCodePart.create { _, _, _ ->
                            visitor.visitInsn(Opcodes.MONITOREXIT)
                        }
                ))
                .build().let {
            codeProcessor.process(TryStatement::class.java, it, data)
        }
    }
}