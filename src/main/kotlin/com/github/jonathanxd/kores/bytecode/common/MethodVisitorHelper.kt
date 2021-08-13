/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2021 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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
package com.github.jonathanxd.kores.bytecode.common

import com.github.jonathanxd.kores.type.GenericType
import com.github.jonathanxd.kores.util.descriptor
import com.github.jonathanxd.kores.util.typeDesc
import com.github.jonathanxd.iutils.map.ListHashMap
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import java.lang.reflect.Type
import java.util.*

/**
 * Internal class that holds a [MethodVisitor] and data about current stored variables (stack)
 * and tag lines.
 *
 * This class doesn't generate bytecode instructions, this class only hold information
 * about variables.
 *
 * @param methodVisitor ASM Method visitor
 * @param variables Variables in stack (including `this`).
 */
class MethodVisitorHelper constructor(
    val methodVisitor: MethodVisitor,
    variables: MutableList<Variable>
) {

    // Tracks amount of values put to stack that is missing to be popped.
    private var stackValues = 0
    private var variableHistory = ListHashMap<Int, Variable>()

    private var frame_: Frame? = Frame(null, variables)

    private var frame: Frame
        get() = this.frame_ ?: throw IllegalStateException("Visit end")
        set(value) {
            this.frame_ = value
        }

    fun enterNewFrame() {
        val end = Label()

        /*
                val start = this.frame.parent?.immutableVariableList?.size ?: 0
                val endSize = this.frame.immutableVariableList.size

                val subList = this.frame.immutableVariableList.subList(start, endSize)

                this.variableHistory.addAll(subList)
        */

        this.frame = Frame(this.frame, emptyList(), end)
    }

    fun exitFrame() {
        this.frame.endLabel?.let {
            methodVisitor.visitLabel(it)
        }

        val start = this.frame.parent?.immutableVariableList?.size ?: 0
        val endSize = this.frame.immutableVariableList.size

        val subList = this.frame.immutableVariableList.subList(start, endSize)

        subList.forEachIndexed { i, variable ->
            this.variableHistory.putToList(start + i, variable)
        }

        //this.variableHistory.addAll(subList)

        this.frame = this.frame.parent ?: throw IllegalStateException("Cannot exit from main frame")
    }

    private fun exitAllFrames() {
        var f = this.frame.parent
        while (f != null) {
            exitFrame()
            f = this.frame.parent
        }
    }

    /**
     * Get variable at stack pos `i`.
     *
     * @param i Index in the stack.
     * @return Variable or [Optional.empty] if not present.
     */
    fun getVar(i: Int): Optional<Variable> = Optional.ofNullable(this.frame.getVar(i))

    /**
     * Get a variable by name.
     *
     * @param name Name of the variable.
     * @return Variable or [Optional.empty] if not present.
     */
    fun getVarByName(name: String): Optional<Variable> =
        Optional.ofNullable(this.frame.getVarByName(name))

    /**
     * Get a variable by name and type.
     *
     * @param name Name of the variable.
     * @param type Type of the variable.
     * @return Variable or [Optional.empty] if not present.
     */
    fun getVar(name: String, type: Type?): Optional<Variable> =
        Optional.ofNullable(this.frame.getVar(name, type))

    /**
     * Gets the position of a variable instance
     *
     * @param variable Variable instance
     * @return Position of variable if exists, or [OptionalInt.empty] otherwise.
     */
    fun getVarPos(variable: Variable): OptionalInt = this.frame.getVarPos(variable)

    /**
     * Add variables
     */
    fun addVar(variable: Variable) = this.frame.add(variable)

    /**
     * Store a variable in stack "table".
     *
     * @param name       Name of variable
     * @param type       Type of variable
     * @param startLabel Start label (first occurrence of variable).
     * @param endLabel   End label (last usage of variable).
     * @return [OptionalInt] holding the position, or empty if failed to store.
     * @throws RuntimeException if variable is already defined.
     */
    fun storeVar(name: String, type: Type, startLabel: Label, endLabel: Label?): OptionalInt =
        this.frame.storeVar(name, type, startLabel, endLabel)

    /**
     * Store a internal variable. (internal variables doesn't have their names generated in
     * LocalVariableTable).
     *
     * Name generation could also be avoided using '#' symbol in the variable name.
     *
     * Position of internal variables couldn't be getted by [.storeVar].
     *
     * Internal variables could be freely redefined and has no restrictions about the redefinition.
     *
     * @param name       Name of variable
     * @param type       Type of variable
     * @param startLabel Start label (first occurrence of variable).
     * @param endLabel   End label (last usage of variable).
     * @return [OptionalInt] holding the position, or empty if failed to store.
     */
    fun storeInternalVar(
        name: String,
        type: Type,
        startLabel: Label,
        endLabel: Label?
    ): OptionalInt =
        this.frame.storeInternalVar(name, type, startLabel, endLabel)

    /**
     * Return last position in stack map.
     *
     * @return Last position in stack map.
     */
    fun currentPos(): Int = this.frame.currentPos()

    /**
     * Gets a immutable list with all variables.
     *
     * @return Immutable list with all variables.
     */
    fun getVariables(): List<Variable> = this.frame.immutableVariableList

    /**
     * Create a unique name of variable based on [base] name.
     */
    fun getUniqueVariableName(base: String): String = this.frame.getUniqueVariableName(base)

    fun hasVar(varName: String) = this.frame.hasVar(varName)

    /**
     * Generate LocalVariableTable
     *
     * @param start Start of the method.
     * @param end   End of the method.
     */
    fun visitVars(end: Label) {
        this.exitAllFrames()

        this.frame.immutableVariableList.forEachIndexed { i, variable ->
            this.variableHistory.putToList(i, variable)
        }

        val variables = variableHistory

        this.frame_ = null


        for (i in variables.keys.sorted().asReversed()) {
            val varis = variables[i]!!

            varis.forEach { variable ->
                if (!variable.isVisible || variable.isTemp || variable.name.contains("#"))
                // Internal variables
                    return@forEach

                val varStart = variable.startLabel
                val varEnd = variable.endLabel ?: end

                val type = variable.type.typeDesc

                var signature: String? = null

                if (variable.type is GenericType) {
                    signature = variable.type.descriptor
                }

                methodVisitor.visitLocalVariable(
                    variable.name,
                    type,
                    signature,
                    varStart,
                    varEnd,
                    i
                )
            }
        }
    }

    /**
     * Generate "find fail" exception to a variable.
     *
     * @param variable Variable failed to find.
     * @return Exception
     */
    fun failFind(variable: Variable): IllegalStateException {
        return IllegalStateException("Cannot find variable '" + variable + "' in stack table: " + this.getVariables())
    }

    /**
     * Generate "store fail" exception to a variable.
     *
     * @param o Object failed to be stored.
     * @return Exception
     */
    fun failStore(o: Any): IllegalStateException {
        return IllegalStateException("Couldn't store '" + o + "' in stack table: " + this.getVariables())
    }
}
