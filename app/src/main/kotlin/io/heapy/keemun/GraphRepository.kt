package io.heapy.keemun

/**
 * Backed by an append-only JSONL log (`keemun.jsonl`). Reads project the log into
 * the current [KeemunGraph]; writes append new records and never rewrite history.
 */
class GraphRepository(private val path: KeemunPath) {
    constructor(path: String) : this(KeemunPath(path))

    fun exists(): Boolean = KeemunFiles.exists(path)

    /** The current accepted graph (used by render, describe, validate, trace). */
    fun read(): KeemunGraph = readLog().current()

    /** The current accepted graph as pretty JSON (the /api/graph payload). */
    fun readJson(): String = GraphJson.encode(read())

    fun readLog(): GraphLog = snapshot().log

    fun readLogViewJson(): String = GraphJson.encodeLogView(readLog().view())

    internal fun snapshot(): GraphSnapshot {
        if (!exists()) {
            throw IllegalArgumentException("Graph file does not exist: $path")
        }
        val cacheKey = KeemunFiles.metadata(path)
        val log = GraphJson.decodeLog(KeemunFiles.readString(path))
        validate(log)
        return GraphSnapshot(log, cacheKey)
    }

    /** Create a brand-new log: a header line plus one accepted change-set for [graph]. */
    fun createFrom(graph: KeemunGraph, message: String? = null): GraphLog {
        val valid = graph.requireValid()
        val records = buildList {
            add(HeaderRecord())
            addAll(changeRecordsFromGraph(valid, FIRST_CHANGE_ID, ChangeStatus.ACCEPTED, message))
        }
        path.parent?.let(KeemunFiles::createDirectories)
        KeemunFiles.writeString(path, GraphJson.encodeLog(records) + "\n")
        return GraphLog(records)
    }

    /** Append a proposed (or accepted) change-set. Validates the resulting projection. */
    fun propose(proposal: ChangeProposal): ChangeSet {
        val log = readLog()
        val id = proposal.changeId?.takeIf(String::isNotBlank) ?: log.nextChangeId()
        require(log.changeSet(id) == null) { "change '$id' already exists" }

        val records = buildList {
            add(ChangeRecord(id, proposal.status, proposal.message, proposal.author, proposal.createdAt))
            proposal.records.forEach { add(it.withChangeId(id)) }
        }
        val next = GraphLog(log.records + records)
        validate(next)
        projectionFor(next, id, proposal.status).requireValid()

        append(records)
        return next.changeSet(id) ?: error("change '$id' missing after append")
    }

    fun accept(changeId: String, author: String? = null): ChangeSet =
        transition(changeId, ChangeStatus.ACCEPTED, author)

    fun reject(changeId: String, author: String? = null): ChangeSet =
        transition(changeId, ChangeStatus.REJECTED, author)

    private fun transition(changeId: String, status: ChangeStatus, author: String?): ChangeSet {
        val log = readLog()
        val current = log.changeSet(changeId)
            ?: throw IllegalArgumentException("Unknown change '$changeId'")
        if (current.status == status) {
            return current
        }
        val record = ChangeRecord(changeId = changeId, status = status, author = author)
        val next = GraphLog(log.records + record)
        if (status == ChangeStatus.ACCEPTED) {
            next.current().requireValid()
        }
        append(listOf(record))
        return next.changeSet(changeId) ?: error("change '$changeId' missing after transition")
    }

    private fun projectionFor(log: GraphLog, changeId: String, status: ChangeStatus): KeemunGraph =
        if (status == ChangeStatus.ACCEPTED) log.current() else log.previewChange(changeId)

    private fun append(records: List<LogRecord>) {
        if (records.isEmpty()) return
        if (!exists()) {
            throw IllegalArgumentException("Graph file does not exist: $path")
        }
        KeemunFiles.appendString(path, GraphJson.encodeLog(records) + "\n")
    }

    private fun validate(log: GraphLog) {
        val errors = mutableListOf<String>()
        if (log.schemaVersion != 1) {
            errors += "schema_version must be 1, got ${log.schemaVersion}"
        }
        log.records.forEach { record ->
            if (record is HeaderRecord) return@forEach
            val changeId = record.changeId()
            if (changeId.isNullOrBlank()) {
                errors += "every record must carry a non-empty change_id"
            } else if (!stableIdPattern.matches(changeId)) {
                errors += "change_id '$changeId' must match ${stableIdPattern.pattern}"
            }
            when (record) {
                is NodeRecord -> if (!stableIdPattern.matches(record.id)) {
                    errors += "node '${record.id}' must use a stable id matching ${stableIdPattern.pattern}"
                }
                is EdgeRecord -> {
                    if (!stableIdPattern.matches(record.id)) {
                        errors += "edge '${record.id}' must use a stable id matching ${stableIdPattern.pattern}"
                    }
                    if (record.weight < 0.0 || record.weight > 1.0 || record.weight.isNaN()) {
                        errors += "edge '${record.id}' weight must be between 0.0 and 1.0"
                    }
                }
                is DeleteRecord -> if (!stableIdPattern.matches(record.id)) {
                    errors += "delete target '${record.id}' must use a stable id matching ${stableIdPattern.pattern}"
                }
                else -> Unit
            }
        }
        if (errors.isNotEmpty()) {
            throw GraphValidationException(errors.distinct().sorted())
        }
    }

    private companion object {
        const val FIRST_CHANGE_ID = "change-0001"
    }
}

internal data class GraphSnapshot(
    val log: GraphLog,
    val cacheKey: GraphCacheKey,
)

internal data class GraphCacheKey(
    val lastModifiedNanos: Long,
    val size: Long,
)
