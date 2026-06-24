package io.heapy.keemun

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class GraphRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    private fun repository() = GraphRepository(tempDir.resolve("keemun.jsonl").toString())

    @Test
    fun `creates an append-only jsonl log and projects the current graph`() {
        val repository = repository()
        repository.createFrom(SampleGraph.create())

        val text = Files.readString(tempDir.resolve("keemun.jsonl"))
        val lines = text.trim().lines()
        assertTrue(lines.first().startsWith("{\"kind\":\"header\""), "first line is the header")
        assertTrue(lines.first().contains("\"repo\":\"https://github.com/Heapy/keemun\""), "header links the repo")
        assertEquals(1, lines.count { it.contains("\"kind\":\"header\"") })
        assertTrue(text.endsWith("\n"))

        val current = repository.read()
        assertEquals(SampleGraph.create().normalized().nodes.map { it.id }, current.nodes.map { it.id })
    }

    @Test
    fun `proposing then accepting appends records and updates the projection`() {
        val repository = repository()
        repository.createFrom(SampleGraph.create())
        val before = repository.read().nodes.size
        val linesBefore = Files.readString(tempDir.resolve("keemun.jsonl")).trim().lines().size

        val proposal = ChangeProposal(
            message = "add caching node",
            records = listOf(
                NodeRecord(id = "caching", type = NodeType.DECISION, title = "Add caching"),
            ),
        )
        val change = repository.propose(proposal)
        assertEquals(ChangeStatus.PROPOSED, change.status)
        assertEquals(before, repository.read().nodes.size, "proposed change is not yet in the current graph")

        repository.accept(change.changeId)
        assertEquals(before + 1, repository.read().nodes.size)
        assertTrue(repository.read().nodes.any { it.id == "caching" })

        val linesAfter = Files.readString(tempDir.resolve("keemun.jsonl")).trim().lines().size
        assertTrue(linesAfter > linesBefore, "records are appended, never rewritten")
        assertEquals(ChangeStatus.ACCEPTED, repository.readLog().changeSet(change.changeId)!!.status)
    }

    @Test
    fun `rejects a proposal that breaks the projection`() {
        val repository = repository()
        repository.createFrom(SampleGraph.create())
        val proposal = ChangeProposal(
            message = "dangling edge",
            records = listOf(
                EdgeRecord(
                    id = "broken",
                    source = "ksp",
                    target = "missing",
                    type = EdgeType.ENABLES,
                    rationale = "Points nowhere.",
                ),
            ),
        )
        val error = assertThrows(GraphValidationException::class.java) { repository.propose(proposal) }
        assertTrue(error.errors.any { it.contains("missing target") })
        assertFalse(repository.readLog().changes().any { it.message == "dangling edge" }, "nothing was appended")
    }

    @Test
    fun `rejects creating a log with dangling edges`() {
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
        val error = assertThrows(GraphValidationException::class.java) { repository().createFrom(graph) }
        assertTrue(error.errors.any { it.contains("missing target") })
    }
}
