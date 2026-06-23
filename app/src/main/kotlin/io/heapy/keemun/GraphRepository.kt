package io.heapy.keemun

class GraphRepository(private val path: KeemunPath) {
    constructor(path: String) : this(KeemunPath(path))

    fun exists(): Boolean = KeemunFiles.exists(path)

    fun read(): KeemunGraph = snapshot().graph

    fun readJson(): String = snapshot().json

    internal fun snapshot(): GraphSnapshot {
        if (!exists()) {
            throw IllegalArgumentException("Graph file does not exist: $path")
        }
        val cacheKey = cacheKey()
        val graph = parse(KeemunFiles.readString(path))
        val json = encode(graph)
        return GraphSnapshot(graph, json, cacheKey)
    }

    fun parse(text: String): KeemunGraph =
        GraphJson.decode(text).requireValid()

    fun write(graph: KeemunGraph): KeemunGraph {
        val normalized = graph.requireValid()
        val parent = path.parent
        if (parent != null) {
            KeemunFiles.createDirectories(parent)
        }
        val json = encode(normalized)
        KeemunFiles.writeString(path, json)
        return normalized
    }

    fun writeText(text: String): KeemunGraph =
        write(parse(text))

    fun toJson(graph: KeemunGraph): String =
        encode(graph.requireValid())

    private fun encode(graph: KeemunGraph): String =
        GraphJson.encode(graph) + "\n"

    private fun cacheKey(): GraphCacheKey =
        KeemunFiles.metadata(path)
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
