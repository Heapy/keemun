package io.heapy.keemun

import java.util.ArrayDeque

object TraceService {
    fun describe(graph: KeemunGraph, targetId: String, fromId: String? = null): String {
        val normalized = graph.requireValid()
        val index = GraphIndex(normalized)
        val target = index.node(targetId) ?: throw IllegalArgumentException("Unknown node '$targetId'")
        if (fromId != null) {
            return describePath(index, fromId, target.id)
        }

        return buildString {
            appendLine("${target.type.wireName()}: ${target.title}")
            if (target.summary.isNotBlank()) {
                appendLine(target.summary)
            }
            appendLine()
            appendLine("How this node was reached:")
            appendIncoming(index, target.id, depth = 0, seenEdges = mutableSetOf())
            appendLine()
            appendLine("Consequences from this node:")
            appendOutgoing(index, target.id)
        }.trimEnd()
    }

    private fun describePath(index: GraphIndex, fromId: String, targetId: String): String {
        val from = index.node(fromId) ?: throw IllegalArgumentException("Unknown node '$fromId'")
        val target = index.node(targetId) ?: throw IllegalArgumentException("Unknown node '$targetId'")
        val path = findPath(index, from.id, target.id)
            ?: throw IllegalArgumentException("No path from '$fromId' to '$targetId'")

        return buildString {
            appendLine("Path from ${from.title} to ${target.title}:")
            if (path.isEmpty()) {
                appendLine("Already at ${target.title}.")
            }
            path.forEachIndexed { indexInPath, edge ->
                val source = index.node(edge.source)!!
                val next = index.node(edge.target)!!
                appendLine("${indexInPath + 1}. ${source.title} ${edge.type.wireName()} ${next.title} [${edge.polarity.wireName()}, weight ${edge.strengthLabel()}]")
                appendLine("   Rationale: ${edge.rationale}")
            }
        }.trimEnd()
    }

    private fun findPath(index: GraphIndex, fromId: String, targetId: String): List<GraphEdge>? {
        if (fromId == targetId) {
            return emptyList()
        }

        val queue = ArrayDeque<Pair<String, List<GraphEdge>>>()
        val seen = mutableSetOf(fromId)
        queue += fromId to emptyList()

        while (queue.isNotEmpty()) {
            val (current, path) = queue.removeFirst()
            for (edge in index.outgoing(current)) {
                if (edge.target in seen) {
                    continue
                }
                val nextPath = path + edge
                if (edge.target == targetId) {
                    return nextPath
                }
                seen += edge.target
                queue += edge.target to nextPath
            }
        }

        return null
    }

    private fun StringBuilder.appendIncoming(
        index: GraphIndex,
        nodeId: String,
        depth: Int,
        seenEdges: MutableSet<String>,
    ) {
        val incoming = index.incoming(nodeId)
        if (incoming.isEmpty() && depth == 0) {
            appendLine("- No incoming rationale recorded.")
            return
        }

        incoming.forEach { edge ->
            if (!seenEdges.add(edge.id)) {
                appendLine("${indent(depth)}- ${edge.id} closes a cycle already described.")
                return@forEach
            }
            val source = index.node(edge.source)!!
            val target = index.node(edge.target)!!
            appendLine("${indent(depth)}- ${source.title} ${edge.type.wireName()} ${target.title} [${edge.polarity.wireName()}, weight ${edge.strengthLabel()}]")
            appendLine("${indent(depth)}  Rationale: ${edge.rationale}")
            appendIncoming(index, source.id, depth + 1, seenEdges)
        }
    }

    private fun StringBuilder.appendOutgoing(index: GraphIndex, nodeId: String) {
        val outgoing = index.outgoing(nodeId)
        if (outgoing.isEmpty()) {
            appendLine("- No outgoing relationships recorded.")
            return
        }
        outgoing.forEach { edge ->
            val target = index.node(edge.target)!!
            appendLine("- ${edge.type.wireName()} ${target.title} [${edge.polarity.wireName()}, weight ${edge.strengthLabel()}]")
            appendLine("  Rationale: ${edge.rationale}")
        }
    }

    private fun indent(depth: Int): String = "  ".repeat(depth)
}

class GraphIndex(graph: KeemunGraph) {
    private val nodesById = graph.nodes.associateBy { it.id }
    private val incomingByTarget = graph.edges.groupBy { it.target }
    private val outgoingBySource = graph.edges.groupBy { it.source }

    fun node(id: String): GraphNode? = nodesById[id]

    fun incoming(id: String): List<GraphEdge> =
        incomingByTarget[id].orEmpty().sortedByTraversal()

    fun outgoing(id: String): List<GraphEdge> =
        outgoingBySource[id].orEmpty().sortedByTraversal()

    private fun List<GraphEdge>.sortedByTraversal(): List<GraphEdge> =
        sortedWith(compareBy<GraphEdge> { it.order ?: Int.MAX_VALUE }.thenBy { it.id })
}
