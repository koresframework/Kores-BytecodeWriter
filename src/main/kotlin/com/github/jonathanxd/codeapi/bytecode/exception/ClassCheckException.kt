package com.github.jonathanxd.codeapi.bytecode.exception

import com.github.jonathanxd.codeapi.bytecode.BytecodeClass

class ClassCheckException(message: String, cause: Throwable, val bytecodeClasses: Array<out BytecodeClass>, val failedBytecodeClass: BytecodeClass) : RuntimeException(message, cause)