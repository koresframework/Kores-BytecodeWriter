package com.github.jonathanxd.codeapi.bytecode

import com.github.jonathanxd.bytecodedisassembler.Disassembler
import com.github.jonathanxd.codeapi.base.ModuleDeclaration
import com.github.jonathanxd.codeapi.base.TypeDeclaration

class BytecodeModule constructor(val module: ModuleDeclaration, private val bytecode_: ByteArray) {

    val disassembledCode: String by lazy {
        Disassembler.disassemble(bytes = this.bytecode, appendHash = true)
    }

    val bytecode get() = bytecode_.clone()

}