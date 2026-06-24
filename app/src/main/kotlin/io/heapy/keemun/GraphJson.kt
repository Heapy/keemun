package io.heapy.keemun

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object GraphJson {
    /** Pretty JSON for projected graphs, the log view, and API/error payloads. */
    val format: Json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = false
        classDiscriminator = "kind"
    }

    /** Compact JSON; one object per JSONL line, and for embedding the log in HTML. */
    val compact: Json = Json {
        prettyPrint = false
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = false
        classDiscriminator = "kind"
    }

    fun decode(text: String): KeemunGraph = format.decodeFromString(text)

    fun encode(graph: KeemunGraph): String = format.encodeToString(graph.normalized())

    fun encodeRecord(record: LogRecord): String =
        compact.encodeToString(LogRecord.serializer(), record)

    fun decodeRecord(line: String): LogRecord =
        compact.decodeFromString(LogRecord.serializer(), line)

    fun decodeLog(text: String): GraphLog =
        GraphLog(
            text.lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map(::decodeRecord)
                .toList(),
        )

    /** Serialize records as newline-delimited JSON (no trailing newline). */
    fun encodeLog(records: List<LogRecord>): String =
        records.joinToString(separator = "\n", transform = ::encodeRecord)

    /** Compact single-object encoding of the grouped log view (embedded in HTML, /api/log). */
    fun encodeLogView(view: GraphLogView): String =
        compact.encodeToString(view)

    fun decodeProposal(text: String): ChangeProposal =
        format.decodeFromString(text)
}
