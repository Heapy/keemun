package io.heapy.keemun

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class GraphRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `writes stable git friendly json`() {
        val graph = KeemunGraph(
            nodes = listOf(
                GraphNode(id = "z-node", type = NodeType.DECISION, title = "Z"),
                GraphNode(id = "a-node", type = NodeType.CONSTRAINT, title = "A"),
            ),
            edges = listOf(
                GraphEdge(
                    id = "z-edge",
                    source = "z-node",
                    target = "a-node",
                    type = EdgeType.CONFLICTS,
                    rationale = "Rejected path.",
                    order = 2,
                ),
                GraphEdge(
                    id = "a-edge",
                    source = "a-node",
                    target = "z-node",
                    type = EdgeType.ENABLES,
                    rationale = "Accepted path.",
                    order = 1,
                ),
            ),
        )

        val path = tempDir.resolve("keemun.json")
        val written = GraphRepository(path).write(graph)
        val text = Files.readString(path)

        assertEquals(listOf("a-node", "z-node"), written.nodes.map { it.id })
        assertEquals(listOf("a-edge", "z-edge"), written.edges.map { it.id })
        assertTrue(text.indexOf("\"id\": \"a-node\"") < text.indexOf("\"id\": \"z-node\""))
        assertTrue(text.endsWith("\n"))
        assertEquals(text, GraphRepository(path).readJson())
    }

    @Test
    fun `rejects dangling edges`() {
        val graph = KeemunGraph(
            nodes = listOf(GraphNode(id = "source", type = NodeType.DECISION, title = "Source")),
            edges = listOf(
                GraphEdge(
                    id = "dangling",
                    source = "source",
                    target = "missing",
                    type = EdgeType.ENABLES,
                    rationale = "Missing target.",
                ),
            ),
        )

        val error = assertThrows(GraphValidationException::class.java) {
            GraphRepository(tempDir.resolve("keemun.json")).write(graph)
        }

        assertTrue(error.errors.any { it.contains("missing target") })
    }
}
