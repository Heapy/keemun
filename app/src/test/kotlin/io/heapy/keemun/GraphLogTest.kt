package io.heapy.keemun

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GraphLogTest {
    private fun node(change: String, id: String, title: String) =
        NodeRecord(changeId = change, id = id, type = NodeType.DECISION, title = title)

    private val base = listOf(
        HeaderRecord(),
        ChangeRecord("change-0001", ChangeStatus.ACCEPTED, "init"),
        node("change-0001", "a", "A"),
        node("change-0001", "b", "B"),
        EdgeRecord("change-0001", id = "a-b", source = "a", target = "b", type = EdgeType.ENABLES, rationale = "r"),
    )

    @Test
    fun `projects accepted change-sets into the current graph`() {
        val current = GraphLog(base).current()
        assertEquals(listOf("a", "b"), current.nodes.map { it.id }.sorted())
        assertEquals(listOf("a-b"), current.edges.map { it.id })
    }

    @Test
    fun `last write wins across change-sets`() {
        val log = GraphLog(
            base + listOf(
                ChangeRecord("change-0002", ChangeStatus.ACCEPTED, "rename"),
                node("change-0002", "a", "A renamed"),
            ),
        )
        assertEquals("A renamed", log.current().nodes.first { it.id == "a" }.title)
    }

    @Test
    fun `delete records tombstone entities`() {
        val log = GraphLog(
            base + listOf(
                ChangeRecord("change-0002", ChangeStatus.ACCEPTED, "drop edge and b"),
                DeleteRecord("change-0002", EntityKind.EDGE, "a-b"),
                DeleteRecord("change-0002", EntityKind.NODE, "b"),
            ),
        )
        val current = log.current()
        assertEquals(listOf("a"), current.nodes.map { it.id })
        assertTrue(current.edges.isEmpty())
    }

    @Test
    fun `proposed change-sets are excluded until accepted`() {
        val proposed = GraphLog(
            base + listOf(
                ChangeRecord("change-0002", ChangeStatus.PROPOSED, "add c"),
                node("change-0002", "c", "C"),
            ),
        )
        assertFalse(proposed.current().nodes.any { it.id == "c" })
        assertTrue(proposed.previewChange("change-0002").nodes.any { it.id == "c" })

        val accepted = GraphLog(proposed.records + ChangeRecord("change-0002", ChangeStatus.ACCEPTED))
        val change = accepted.changeSet("change-0002")!!
        assertEquals(ChangeStatus.ACCEPTED, change.status)
        assertEquals("add c", change.message, "metadata merges across change records")
        assertTrue(accepted.current().nodes.any { it.id == "c" })
    }

    @Test
    fun `asOf folds history up to a sequence`() {
        val log = GraphLog(
            base + listOf(
                ChangeRecord("change-0002", ChangeStatus.ACCEPTED, "add c"),
                node("change-0002", "c", "C"),
            ),
        )
        assertEquals(listOf("a", "b"), log.asOf(1).nodes.map { it.id }.sorted())
        assertEquals(listOf("a", "b", "c"), log.asOf(2).nodes.map { it.id }.sorted())
    }

    @Test
    fun `nextChangeId is deterministic and collision free`() {
        assertEquals("change-0002", GraphLog(base).nextChangeId())
    }

    @Test
    fun `changeRecordsFromGraph round-trips through projection`() {
        val sample = SampleGraph.create()
        val log = GraphLog(
            listOf(HeaderRecord()) +
                changeRecordsFromGraph(sample, "change-0001", ChangeStatus.ACCEPTED, "sample"),
        )
        val current = log.current().normalized()
        assertEquals(sample.normalized().nodes.map { it.id }, current.nodes.map { it.id })
        assertEquals(sample.normalized().edges.map { it.id }, current.edges.map { it.id })
        assertEquals(sample.metadata.title, current.metadata.title)
    }
}
