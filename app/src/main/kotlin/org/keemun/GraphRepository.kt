package org.keemun

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit

class GraphRepository(private val path: Path) {
    private var cachedSnapshot: GraphSnapshot? = null

    fun exists(): Boolean = Files.exists(path)

    fun read(): KeemunGraph = snapshot().graph

    fun readJson(): String = snapshot().json

    @Synchronized
    internal fun snapshot(): GraphSnapshot {
        if (!exists()) {
            throw IllegalArgumentException("Graph file does not exist: $path")
        }
        val cacheKey = cacheKey()
        cachedSnapshot?.let { snapshot ->
            if (snapshot.cacheKey == cacheKey) {
                return snapshot
            }
        }

        val graph = parse(Files.readString(path, StandardCharsets.UTF_8))
        val json = encode(graph)
        return GraphSnapshot(graph, json, cacheKey).also {
            cachedSnapshot = it
        }
    }

    fun parse(text: String): KeemunGraph =
        GraphJson.decode(text).requireValid()

    @Synchronized
    fun write(graph: KeemunGraph): KeemunGraph {
        val normalized = graph.requireValid()
        val parent = path.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        val json = encode(normalized)
        Files.writeString(
            path,
            json,
            StandardCharsets.UTF_8,
            CREATE,
            WRITE,
            TRUNCATE_EXISTING,
        )
        cachedSnapshot = GraphSnapshot(normalized, json, cacheKey())
        return normalized
    }

    fun writeText(text: String): KeemunGraph =
        write(parse(text))

    fun toJson(graph: KeemunGraph): String =
        encode(graph.requireValid())

    private fun encode(graph: KeemunGraph): String =
        GraphJson.encode(graph) + "\n"

    private fun cacheKey(): GraphCacheKey {
        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
        return GraphCacheKey(
            lastModifiedNanos = attributes.lastModifiedTime().to(TimeUnit.NANOSECONDS),
            size = attributes.size(),
        )
    }
}

internal data class GraphSnapshot(
    val graph: KeemunGraph,
    val json: String,
    val cacheKey: GraphCacheKey,
)

internal data class GraphCacheKey(
    val lastModifiedNanos: Long,
    val size: Long,
)
