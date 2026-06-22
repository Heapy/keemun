package org.keemun

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class KeemunServerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `serves html and saves edited graph`() = testApplication {
        val repository = GraphRepository(tempDir.resolve("keemun.json"))
        repository.write(SampleGraph.create())

        application {
            keemunApplication(repository)
        }

        val html = client.get("/")
        assertEquals(HttpStatusCode.OK, html.status)
        assertTrue(html.bodyAsText().contains("json-editor"))

        val edited = repository.read().copy(
            metadata = repository.read().metadata.copy(title = "Edited Keemun"),
        )
        val saved = client.put("/api/graph") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(GraphJson.encode(edited))
        }

        assertEquals(HttpStatusCode.OK, saved.status)
        assertEquals("Edited Keemun", repository.read().metadata.title)
        assertTrue(saved.bodyAsText().contains("Edited Keemun"))

        val editedHtml = client.get("/")
        assertEquals(HttpStatusCode.OK, editedHtml.status)
        assertTrue(editedHtml.bodyAsText().contains("Edited Keemun"))
    }
}
