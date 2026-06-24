package io.heapy.keemun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
data class KeemunGraph(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("schema_version")
    val schemaVersion: Int = 1,
    val metadata: GraphMetadata = GraphMetadata(),
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
) {
    fun normalized(): KeemunGraph =
        copy(
            nodes = nodes
                .map { it.copy(tags = it.tags.distinct().sorted()) }
                .sortedBy { it.id },
            edges = edges
                .map { it.copy(criteria = it.criteria.distinct().sorted()) }
                .sortedWith(compareBy<GraphEdge> { it.order ?: Int.MAX_VALUE }.thenBy { it.id }),
        )

    fun requireValid(): KeemunGraph {
        val graph = normalized()
        val errors = graph.validationErrors()
        if (errors.isNotEmpty()) {
            throw GraphValidationException(errors)
        }
        return graph
    }

    fun validationErrors(): List<String> {
        val errors = mutableListOf<String>()
        if (schemaVersion != 1) {
            errors += "schema_version must be 1, got $schemaVersion"
        }

        val nodeIds = nodes.map { it.id }
        errors += duplicateErrors("node", nodeIds)
        errors += duplicateErrors("edge", edges.map { it.id })

        val nodeIdSet = nodeIds.toSet()
        nodes.forEach { node ->
            if (!stableIdPattern.matches(node.id)) {
                errors += "node '${node.id}' must use a stable id matching ${stableIdPattern.pattern}"
            }
            if (node.title.isBlank()) {
                errors += "node '${node.id}' must have a title"
            }
        }

        edges.forEach { edge ->
            if (!stableIdPattern.matches(edge.id)) {
                errors += "edge '${edge.id}' must use a stable id matching ${stableIdPattern.pattern}"
            }
            if (edge.source !in nodeIdSet) {
                errors += "edge '${edge.id}' references missing source '${edge.source}'"
            }
            if (edge.target !in nodeIdSet) {
                errors += "edge '${edge.id}' references missing target '${edge.target}'"
            }
            if (edge.rationale.isBlank()) {
                errors += "edge '${edge.id}' must include rationale"
            }
            if (edge.weight < 0.0 || edge.weight > 1.0 || edge.weight.isNaN()) {
                errors += "edge '${edge.id}' weight must be between 0.0 and 1.0"
            }
            if (edge.order != null && edge.order <= 0) {
                errors += "edge '${edge.id}' order must be positive"
            }
            if (edge.decidedAt != null && !isoDatePattern.matches(edge.decidedAt)) {
                errors += "edge '${edge.id}' decided_at must use YYYY-MM-DD"
            }
        }

        return errors.sorted()
    }

    private fun duplicateErrors(kind: String, ids: List<String>): List<String> =
        ids.groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .sorted()
            .map { "duplicate $kind id '$it'" }
}

internal val stableIdPattern = Regex("^[a-z0-9][a-z0-9._:-]*$")
internal val isoDatePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")

@Serializable
data class GraphMetadata(
    val title: String = "Keemun",
    val summary: String = "",
    val authors: List<String> = emptyList(),
)

@Serializable
data class GraphNode(
    val id: String,
    val type: NodeType,
    val title: String,
    val summary: String = "",
    val status: NodeStatus = NodeStatus.ACCEPTED,
    val tags: List<String> = emptyList(),
    val external: Boolean = false,
)

@Serializable
data class GraphEdge(
    val id: String,
    val source: String,
    val target: String,
    val type: EdgeType,
    val rationale: String,
    val status: EdgeStatus = EdgeStatus.ACCEPTED,
    val polarity: EdgePolarity = EdgePolarity.POSITIVE,
    val weight: Double = 1.0,
    val order: Int? = null,
    @SerialName("decided_at")
    val decidedAt: String? = null,
    val criteria: List<String> = emptyList(),
) {
    fun strengthLabel(): String =
        if (abs(weight - 1.0) < 0.0001) "1.0" else twoDecimalPlaces(weight)
}

private fun twoDecimalPlaces(value: Double): String {
    val scaled = (value * 100.0).roundToInt()
    val whole = scaled / 100
    val fraction = abs(scaled % 100).toString().padStart(2, '0')
    return "$whole.$fraction"
}

@Serializable
enum class NodeType {
    @SerialName("constraint")
    CONSTRAINT,

    @SerialName("decision")
    DECISION,

    @SerialName("question")
    QUESTION,

    @SerialName("option")
    OPTION,

    @SerialName("outcome")
    OUTCOME,
}

@Serializable
enum class NodeStatus {
    @SerialName("accepted")
    ACCEPTED,

    @SerialName("proposed")
    PROPOSED,

    @SerialName("rejected")
    REJECTED,

    @SerialName("deprecated")
    DEPRECATED,
}

@Serializable
enum class EdgeType {
    @SerialName("constrains")
    CONSTRAINS,

    @SerialName("enables")
    ENABLES,

    @SerialName("forbids")
    FORBIDS,

    @SerialName("conflicts")
    CONFLICTS,
}

@Serializable
enum class EdgeStatus {
    @SerialName("accepted")
    ACCEPTED,

    @SerialName("considered")
    CONSIDERED,

    @SerialName("rejected")
    REJECTED,
}

@Serializable
enum class EdgePolarity {
    @SerialName("positive")
    POSITIVE,

    @SerialName("negative")
    NEGATIVE,
}

class GraphValidationException(val errors: List<String>) :
    IllegalArgumentException(errors.joinToString(separator = "\n"))

fun NodeType.wireName(): String =
    when (this) {
        NodeType.CONSTRAINT -> "constraint"
        NodeType.DECISION -> "decision"
        NodeType.QUESTION -> "question"
        NodeType.OPTION -> "option"
        NodeType.OUTCOME -> "outcome"
    }

fun EdgeType.wireName(): String =
    when (this) {
        EdgeType.CONSTRAINS -> "constrains"
        EdgeType.ENABLES -> "enables"
        EdgeType.FORBIDS -> "forbids"
        EdgeType.CONFLICTS -> "conflicts"
    }

fun EdgeStatus.wireName(): String =
    when (this) {
        EdgeStatus.ACCEPTED -> "accepted"
        EdgeStatus.CONSIDERED -> "considered"
        EdgeStatus.REJECTED -> "rejected"
    }

fun EdgePolarity.wireName(): String =
    when (this) {
        EdgePolarity.POSITIVE -> "positive"
        EdgePolarity.NEGATIVE -> "negative"
    }
