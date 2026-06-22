package io.heapy.keemun

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object GraphJson {
    val format: Json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = false
    }

    fun decode(text: String): KeemunGraph = format.decodeFromString(text)

    fun encode(graph: KeemunGraph): String = format.encodeToString(graph.normalized())
}
