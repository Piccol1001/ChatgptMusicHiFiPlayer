package com.piccol.hifi.ui

import com.piccol.hifi.data.LibraryGroup

data class FolderNode(val path: String, val label: String, val count: Int, val hasChildren: Boolean)

/** Presentation-only hierarchy over indexed real folder paths; no fabricated media. */
fun folderChildren(groups: List<LibraryGroup>, prefix: String): List<FolderNode> {
    val base = prefix.trim('/').let { if (it.isEmpty()) "" else "$it/" }
    return groups
        .filter { it.name.trim('/').startsWith(base) && it.name.trim('/') != prefix.trim('/') }
        .groupBy { it.name.trim('/').removePrefix(base).substringBefore('/') }
        .map { (name, members) ->
            val path = base + name
            FolderNode(
                path,
                name,
                members.sumOf { it.count },
                members.any { it.name.trim('/') != path },
            )
        }
        .sortedBy { it.label.lowercase() }
}
