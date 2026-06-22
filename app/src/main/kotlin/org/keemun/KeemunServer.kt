package org.keemun

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.nio.charset.StandardCharsets

class KeemunServer(
    private val repository: GraphRepository,
    private val renderer: HtmlRenderer = HtmlRenderer(),
    private val engine: RenderEngine = RenderEngine.SVG,
) {
    fun start(host: String, port: Int) {
        embeddedServer(CIO, host = host, port = port) {
            keemunApplication(repository, renderer, this@KeemunServer.engine)
        }.start(wait = true)
    }
}

fun Application.keemunApplication(
    repository: GraphRepository,
    renderer: HtmlRenderer = HtmlRenderer(),
    engine: RenderEngine = RenderEngine.SVG,
) {
    val renderCache = RenderCache(repository, renderer, engine)

    routing {
        get("/") {
            call.respondHtml(renderCache.html(editable = true))
        }
        get("/keemun.html") {
            call.respondHtml(renderCache.html(editable = false))
        }
        get("/api/graph") {
            call.respondText(
                repository.readJson(),
                ContentType.Application.Json.withCharset(StandardCharsets.UTF_8),
            )
        }
        put("/api/graph") {
            call.saveGraph(repository, renderCache)
        }
        post("/api/graph") {
            call.saveGraph(repository, renderCache)
        }
        get("/api/describe/{id}") {
            val id = call.parameters["id"].orEmpty()
            val from = call.request.queryParameters["from"]
            runCatching {
                TraceService.describe(repository.read(), id, from)
            }.fold(
                onSuccess = { description ->
                    call.respondText(description, ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8))
                },
                onFailure = { error ->
                    call.respondError(HttpStatusCode.BadRequest, error.message ?: "Could not describe node")
                },
            )
        }
    }
}

private class RenderCache(
    private val repository: GraphRepository,
    private val renderer: HtmlRenderer,
    private val engine: RenderEngine,
) {
    private var editableHtml: RenderSnapshot? = null
    private var staticHtml: RenderSnapshot? = null

    @Synchronized
    fun html(editable: Boolean): String {
        val graphSnapshot = repository.snapshot()
        val current = if (editable) editableHtml else staticHtml
        if (current?.cacheKey == graphSnapshot.cacheKey && current.engine == engine) {
            return current.html
        }

        val html = renderer.render(graphSnapshot.graph, editable, engine)
        val next = RenderSnapshot(graphSnapshot.cacheKey, engine, html)
        if (editable) {
            editableHtml = next
        } else {
            staticHtml = next
        }
        return html
    }

    @Synchronized
    fun invalidate() {
        editableHtml = null
        staticHtml = null
    }
}

private data class RenderSnapshot(
    val cacheKey: GraphCacheKey,
    val engine: RenderEngine,
    val html: String,
)

private suspend fun ApplicationCall.respondHtml(html: String) {
    respondText(html, ContentType.Text.Html.withCharset(StandardCharsets.UTF_8))
}

private suspend fun ApplicationCall.saveGraph(repository: GraphRepository, renderCache: RenderCache) {
    runCatching {
        repository.writeText(receiveText())
    }.fold(
        onSuccess = { graph ->
            renderCache.invalidate()
            respondText(repository.toJson(graph), ContentType.Application.Json.withCharset(StandardCharsets.UTF_8))
        },
        onFailure = { error ->
            val details = if (error is GraphValidationException) error.errors else emptyList()
            respondError(HttpStatusCode.BadRequest, error.message ?: "Invalid graph", details)
        },
    )
}

private suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    message: String,
    details: List<String> = emptyList(),
) {
    val payload = ErrorPayload(error = message, details = details)
    respondText(
        GraphJson.format.encodeToString(payload),
        ContentType.Application.Json.withCharset(StandardCharsets.UTF_8),
        status,
    )
}

@Serializable
private data class ErrorPayload(
    val error: String,
    val details: List<String> = emptyList(),
)
