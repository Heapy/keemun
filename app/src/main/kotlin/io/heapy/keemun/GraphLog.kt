package io.heapy.keemun

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The on-disk format is an append-only JSONL log. Every line is one [LogRecord]
 * tagged with a `change_id`; records sharing a `change_id` form one [ChangeSet] —
 * "a single unit of design". A change-set carries a review [ChangeStatus], so
 * proposed edits can be reviewed before they count. The current graph is a
 * projection (fold) over accepted change-sets; updating a node is just appending
 * a new full-state row, and a [DeleteRecord] is a tombstone.
 */

@Serializable
enum class ChangeStatus {
    @SerialName("proposed")
    PROPOSED,

    @SerialName("accepted")
    ACCEPTED,

    @SerialName("rejected")
    REJECTED,
}

fun ChangeStatus.wireName(): String =
    when (this) {
        ChangeStatus.PROPOSED -> "proposed"
        ChangeStatus.ACCEPTED -> "accepted"
        ChangeStatus.REJECTED -> "rejected"
    }

@Serializable
enum class EntityKind {
    @SerialName("node")
    NODE,

    @SerialName("edge")
    EDGE,
}

/** One JSONL line. The `kind` discriminator is supplied by the JSON format. */
@Serializable
sealed class LogRecord

/** First line of the file; records the format version and the project repo. */
@Serializable
@SerialName("header")
data class HeaderRecord(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("schema_version")
    val schemaVersion: Int = 1,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val repo: String = KEEMUN_REPO,
) : LogRecord()

const val KEEMUN_REPO: String = "https://github.com/Heapy/keemun"

/**
 * Declares or transitions a change-set. Fields merge across records for the same
 * `change_id` (status: last wins; message/author/created_at: last non-null wins),
 * so a proposed → accepted transition is just an appended status-only line.
 */
@Serializable
@SerialName("change")
data class ChangeRecord(
    @SerialName("change_id")
    val changeId: String = "",
    val status: ChangeStatus? = null,
    val message: String? = null,
    val author: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
) : LogRecord()

/** Versioned graph metadata; full-state replace, last accepted wins. */
@Serializable
@SerialName("meta")
data class MetaRecord(
    @SerialName("change_id")
    val changeId: String = "",
    val title: String = "Keemun",
    val summary: String = "",
    val authors: List<String> = emptyList(),
) : LogRecord() {
    fun toMetadata(): GraphMetadata = GraphMetadata(title, summary, authors)
}

/** A full node revision. A new row is a new full version (replace, not field-merge). */
@Serializable
@SerialName("node")
data class NodeRecord(
    @SerialName("change_id")
    val changeId: String = "",
    val id: String,
    val type: NodeType,
    val title: String,
    val summary: String = "",
    val status: NodeStatus = NodeStatus.ACCEPTED,
    val tags: List<String> = emptyList(),
    val external: Boolean = false,
) : LogRecord() {
    fun toNode(): GraphNode = GraphNode(id, type, title, summary, status, tags, external)
}

/** A full edge revision. */
@Serializable
@SerialName("edge")
data class EdgeRecord(
    @SerialName("change_id")
    val changeId: String = "",
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
) : LogRecord() {
    fun toEdge(): GraphEdge =
        GraphEdge(id, source, target, type, rationale, status, polarity, weight, order, decidedAt, criteria)
}

/** Tombstone for a node or edge id. */
@Serializable
@SerialName("delete")
data class DeleteRecord(
    @SerialName("change_id")
    val changeId: String = "",
    val entity: EntityKind,
    val id: String,
) : LogRecord()

fun LogRecord.changeId(): String? =
    when (this) {
        is HeaderRecord -> null
        is ChangeRecord -> changeId
        is MetaRecord -> changeId
        is NodeRecord -> changeId
        is EdgeRecord -> changeId
        is DeleteRecord -> changeId
    }

fun LogRecord.withChangeId(id: String): LogRecord =
    when (this) {
        is HeaderRecord -> this
        is ChangeRecord -> copy(changeId = id)
        is MetaRecord -> copy(changeId = id)
        is NodeRecord -> copy(changeId = id)
        is EdgeRecord -> copy(changeId = id)
        is DeleteRecord -> copy(changeId = id)
    }

fun GraphNode.toRecord(changeId: String): NodeRecord =
    NodeRecord(changeId, id, type, title, summary, status, tags, external)

fun GraphEdge.toRecord(changeId: String): EdgeRecord =
    EdgeRecord(changeId, id, source, target, type, rationale, status, polarity, weight, order, decidedAt, criteria)

/** A change-set: its review metadata plus the content records (meta/node/edge/delete) it carries. */
@Serializable
data class ChangeSet(
    @SerialName("change_id")
    val changeId: String,
    val status: ChangeStatus,
    val message: String? = null,
    val author: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val seq: Int,
    val records: List<LogRecord> = emptyList(),
)

/** Serialized view of the whole log, grouped into change-sets. Embedded in HTML and returned by /api/log. */
@Serializable
data class GraphLogView(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("schema_version")
    val schemaVersion: Int = 1,
    val changes: List<ChangeSet> = emptyList(),
)

/** Request payload for proposing a new change-set (CLI `propose`, `POST /api/changes`). */
@Serializable
data class ChangeProposal(
    @SerialName("change_id")
    val changeId: String? = null,
    val status: ChangeStatus = ChangeStatus.PROPOSED,
    val message: String? = null,
    val author: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val records: List<LogRecord> = emptyList(),
)

class GraphLog(val records: List<LogRecord>) {
    val schemaVersion: Int =
        records.filterIsInstance<HeaderRecord>().firstOrNull()?.schemaVersion ?: 1

    /** Change-sets in first-seen (append) order, with change metadata merged across records. */
    fun changes(): List<ChangeSet> {
        val order = mutableListOf<String>()
        val status = mutableMapOf<String, ChangeStatus>()
        val message = mutableMapOf<String, String>()
        val author = mutableMapOf<String, String>()
        val createdAt = mutableMapOf<String, String>()
        val content = linkedMapOf<String, MutableList<LogRecord>>()

        records.forEach { record ->
            val id = record.changeId() ?: return@forEach
            content.getOrPut(id) { order += id; mutableListOf() }
            when (record) {
                is ChangeRecord -> {
                    record.status?.let { status[id] = it }
                    record.message?.let { message[id] = it }
                    record.author?.let { author[id] = it }
                    record.createdAt?.let { createdAt[id] = it }
                }
                is MetaRecord, is NodeRecord, is EdgeRecord, is DeleteRecord -> content.getValue(id) += record
                is HeaderRecord -> Unit
            }
        }

        return order.mapIndexed { index, id ->
            ChangeSet(
                changeId = id,
                status = status[id] ?: ChangeStatus.PROPOSED,
                message = message[id],
                author = author[id],
                createdAt = createdAt[id],
                seq = index + 1,
                records = content.getValue(id),
            )
        }
    }

    fun view(): GraphLogView = GraphLogView(schemaVersion, changes())

    /** Fold the included change-sets (in seq order) into a graph; last write wins, deletes remove. */
    fun project(include: (ChangeSet) -> Boolean): KeemunGraph {
        val nodes = LinkedHashMap<String, GraphNode>()
        val edges = LinkedHashMap<String, GraphEdge>()
        var metadata = GraphMetadata()
        changes().filter(include).forEach { changeSet ->
            changeSet.records.forEach { record ->
                when (record) {
                    is NodeRecord -> nodes[record.id] = record.toNode()
                    is EdgeRecord -> edges[record.id] = record.toEdge()
                    is MetaRecord -> metadata = record.toMetadata()
                    is DeleteRecord -> when (record.entity) {
                        EntityKind.NODE -> nodes.remove(record.id)
                        EntityKind.EDGE -> edges.remove(record.id)
                    }
                    is ChangeRecord, is HeaderRecord -> Unit
                }
            }
        }
        return KeemunGraph(
            metadata = metadata,
            nodes = nodes.values.toList(),
            edges = edges.values.toList(),
        )
    }

    fun current(): KeemunGraph =
        project { it.status == ChangeStatus.ACCEPTED }

    fun previewChange(changeId: String): KeemunGraph =
        project { it.status == ChangeStatus.ACCEPTED || it.changeId == changeId }

    fun asOf(seq: Int): KeemunGraph =
        project { it.status == ChangeStatus.ACCEPTED && it.seq <= seq }

    fun changeSet(changeId: String): ChangeSet? =
        changes().firstOrNull { it.changeId == changeId }

    /** Deterministic, collision-free next change id (no clock/RNG). */
    fun nextChangeId(): String {
        val existing = changes().mapTo(mutableSetOf()) { it.changeId }
        var n = existing.size + 1
        var candidate = changeId(n)
        while (candidate in existing) {
            n++
            candidate = changeId(n)
        }
        return candidate
    }

    private fun changeId(n: Int): String = "change-" + n.toString().padStart(4, '0')
}

/** Build the records for one change-set that recreates a whole [KeemunGraph]. */
fun changeRecordsFromGraph(
    graph: KeemunGraph,
    changeId: String,
    status: ChangeStatus,
    message: String? = null,
    author: String? = null,
    createdAt: String? = null,
    includeMeta: Boolean = true,
): List<LogRecord> {
    val records = mutableListOf<LogRecord>()
    records += ChangeRecord(changeId, status, message, author, createdAt)
    if (includeMeta) {
        records += MetaRecord(changeId, graph.metadata.title, graph.metadata.summary, graph.metadata.authors)
    }
    graph.nodes.forEach { records += it.toRecord(changeId) }
    graph.edges.forEach { records += it.toRecord(changeId) }
    return records
}
