package io.heapy.keemun

/**
 * Renders a [GraphLog] to an agent-readable Markdown document: graph metadata, the
 * change-set history (so pending proposals are visible), the current accepted nodes
 * grouped by type, and every edge with its first-class rationale. This is the format
 * an agent reads to understand the architecture before proposing a change.
 */
class MarkdownRenderer {
    fun render(log: GraphLog): String {
        val graph = log.current().normalized()
        val changes = log.changes()
        val titles = graph.nodes.associate { it.id to it.title }

        return buildString {
            val meta = graph.metadata
            appendLine("# ${meta.title}")
            appendLine()
            if (meta.summary.isNotBlank()) {
                appendLine(meta.summary)
                appendLine()
            }
            if (meta.authors.isNotEmpty()) {
                appendLine("_Authors: ${meta.authors.joinToString(", ")}_")
                appendLine()
            }
            appendLine(
                "_${count(graph.nodes.size, "node")} · ${count(graph.edges.size, "edge")} · " +
                    "${count(changes.size, "change-set")} — this view reflects accepted change-sets only._",
            )
            appendLine()

            appendLine("## Change history")
            appendLine()
            appendLine("| # | Change | Status | Author | Contents | Summary |")
            appendLine("| --- | --- | --- | --- | --- | --- |")
            changes.forEach { change ->
                appendLine(
                    "| ${change.seq} | `${change.changeId}` | ${change.status.wireName()} | " +
                        "${cell(change.author).ifEmpty { "—" }} | ${contentSummary(change)} | ${cell(change.message)} |",
                )
            }
            appendLine()

            appendLine("## Nodes")
            appendLine()
            NODE_ORDER.forEach { type ->
                val nodes = graph.nodes.filter { it.type == type }
                if (nodes.isEmpty()) return@forEach
                appendLine("### ${heading(type)}")
                appendLine()
                nodes.forEach { node ->
                    append("- **${node.title}** (`${node.id}`)")
                    if (node.status != NodeStatus.ACCEPTED) append(" _(${node.status.name.lowercase()})_")
                    if (node.external) append(" _(external)_")
                    if (node.summary.isNotBlank()) append(" — ${node.summary}")
                    if (node.tags.isNotEmpty()) append("  ${node.tags.joinToString(" ") { "`$it`" }}")
                    appendLine()
                }
                appendLine()
            }

            if (graph.edges.isNotEmpty()) {
                appendLine("## Rationale (edges)")
                appendLine()
                graph.edges.forEach { edge ->
                    val source = titles[edge.source] ?: edge.source
                    val target = titles[edge.target] ?: edge.target
                    val polarity = if (edge.polarity == EdgePolarity.NEGATIVE) ", negative" else ""
                    appendLine(
                        "- **$source** —${edge.type.wireName()}→ **$target**  ·  " +
                            "`${edge.source}` → `${edge.target}`  ·  weight ${edge.strengthLabel()}$polarity",
                    )
                    if (edge.rationale.isNotBlank()) appendLine("  ${edge.rationale}")
                }
                appendLine()
            }
        }.trimEnd() + "\n"
    }

    private fun contentSummary(change: ChangeSet): String {
        val nodes = change.records.count { it is NodeRecord }
        val edges = change.records.count { it is EdgeRecord }
        val deletes = change.records.count { it is DeleteRecord }
        return buildList {
            if (nodes > 0) add(count(nodes, "node"))
            if (edges > 0) add(count(edges, "edge"))
            if (deletes > 0) add(count(deletes, "delete"))
        }.joinToString(", ").ifEmpty { "—" }
    }

    private fun count(n: Int, noun: String): String = "$n $noun${if (n == 1) "" else "s"}"

    private fun cell(value: String?): String =
        (value ?: "").replace("|", "\\|").replace("\n", " ").trim()

    private fun heading(type: NodeType): String =
        when (type) {
            NodeType.DECISION -> "Decisions"
            NodeType.CONSTRAINT -> "Constraints"
            NodeType.QUESTION -> "Questions"
            NodeType.OPTION -> "Options"
            NodeType.OUTCOME -> "Outcomes"
        }

    private companion object {
        private val NODE_ORDER = listOf(
            NodeType.DECISION,
            NodeType.CONSTRAINT,
            NodeType.QUESTION,
            NodeType.OPTION,
            NodeType.OUTCOME,
        )
    }
}
