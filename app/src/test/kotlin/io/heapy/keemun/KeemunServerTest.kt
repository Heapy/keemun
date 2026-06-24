package io.heapy.keemun

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class KeemunServerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `serves an editable history UI and reviews a proposed change`() = testApplication {
        val repository = GraphRepository(tempDir.resolve("keemun.jsonl").toString())
        repository.createFrom(SampleGraph.create())

        application { keemunApplication(repository) }

        val html = client.get("/")
        assertEquals(HttpStatusCode.OK, html.status)
        val page = html.bodyAsText()
        assertTrue(page.contains("Author a change"))
        assertTrue(page.contains("function renderTimeline"))
        assertTrue(page.contains("const editorEnabled = true"))

        val log = client.get("/api/log")
        assertEquals(HttpStatusCode.OK, log.status)
        assertTrue(log.bodyAsText().contains("\"changes\""))

        val proposal = ChangeProposal(
            message = "add node x",
            records = listOf(NodeRecord(id = "x", type = NodeType.DECISION, title = "X")),
        )
        val proposed = client.post("/api/changes") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(GraphJson.compact.encodeToString(proposal))
        }
        assertEquals(HttpStatusCode.OK, proposed.status)
        val view = GraphJson.compact.decodeFromString(GraphLogView.serializer(), proposed.bodyAsText())
        val created = view.changes.last()
        assertEquals(ChangeStatus.PROPOSED, created.status)
        assertFalse(repository.read().nodes.any { it.id == "x" }, "proposed change is not yet canonical")

        val accepted = client.post("/api/changes/${created.changeId}/accept")
        assertEquals(HttpStatusCode.OK, accepted.status)
        assertTrue(repository.read().nodes.any { it.id == "x" })
    }

    @Test
    fun `rejects an invalid proposal with details`() = testApplication {
        val repository = GraphRepository(tempDir.resolve("keemun.jsonl").toString())
        repository.createFrom(SampleGraph.create())

        application { keemunApplication(repository) }

        val proposal = ChangeProposal(
            message = "broken",
            records = listOf(
                EdgeRecord(id = "broken", source = "ksp", target = "missing", type = EdgeType.ENABLES, rationale = "x"),
            ),
        )
        val response = client.post("/api/changes") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(GraphJson.compact.encodeToString(proposal))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("missing"))
    }
}
