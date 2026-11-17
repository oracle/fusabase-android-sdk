// Copyright (c) 2026, Oracle and/or its affiliates.
package com.oracle.mobile.fusabase.build

import com.sun.source.tree.AnnotationTree
import com.sun.source.tree.ClassTree
import com.sun.source.tree.CompilationUnitTree
import com.sun.source.util.JavacTask
import java.io.File
import javax.tools.ToolProvider

/** Selects whole Java source files whose top-level API is restricted to the SDK library group. */
internal fun dokkaRestrictedSources(files: Collection<File>): List<File> {
    if (files.isEmpty()) return emptyList()
    val compiler = checkNotNull(ToolProvider.getSystemJavaCompiler()) { "Dokka filtering requires a JDK" }
    compiler.getStandardFileManager(null, null, Charsets.UTF_8).use { manager ->
        val task = compiler.getTask(null, manager, null, listOf("-proc:none"), null,
            manager.getJavaFileObjectsFromFiles(files)) as JavacTask
        val units = task.parse().toList()
        val restrictedPackages = units.filter { unit ->
            unit.packageAnnotations.any { it.isLibraryGroup(unit) }
        }.map { it.packageName.toString() }.toSet()
        return units.filter { unit ->
            if ((unit.packageName?.toString() ?: "") in restrictedPackages) return@filter true
            val types = unit.typeDecls.filterIsInstance<ClassTree>()
            val restricted = types.filter { type ->
                type.modifiers.annotations.any { it.isLibraryGroup(unit) }
            }
            // Never hide unrelated declarations merely because they share a source file.
            check(restricted.isEmpty() || restricted.size == types.size) {
                "Mixed restricted/public top-level types in ${unit.sourceFile.name}; split the source before documenting it"
            }
            restricted.isNotEmpty()
        }.map { File(it.sourceFile.toUri()) }
    }
}

private fun AnnotationTree.isLibraryGroup(unit: CompilationUnitTree): Boolean {
    val imports = unit.imports.map { it.qualifiedIdentifier.toString() }.toSet()
    val name = annotationType.toString()
    val isRestrictTo = name == "androidx.annotation.RestrictTo" ||
        (name == "RestrictTo" && ("androidx.annotation.RestrictTo" in imports ||
            "androidx.annotation.*" in imports))
    if (!isRestrictTo) return false
    // Match the exact enum constant, including named arguments and annotation arrays.
    return arguments.any { argument ->
        Regex("""\b(?:androidx\.annotation\.)?RestrictTo\.Scope\.LIBRARY_GROUP\b""")
            .containsMatchIn(argument.toString()) ||
            (("androidx.annotation.RestrictTo.Scope" in imports) &&
                Regex("""\bScope\.LIBRARY_GROUP\b""").containsMatchIn(argument.toString())) ||
            (("androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP" in imports ||
                "androidx.annotation.RestrictTo.Scope.*" in imports) &&
                Regex("""\bLIBRARY_GROUP\b""").containsMatchIn(argument.toString()))
    }
}
