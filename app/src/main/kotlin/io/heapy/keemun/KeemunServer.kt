package io.heapy.keemun

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
import io.ktor.server.routing.routing
import io.ktor.utils.io.charsets.Charsets
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class KeemunServer(
    private val repository: GraphRepository,
    private val renderer: HtmlRenderer = HtmlRenderer(),
) {
    fun start(host: String, port: Int) {
        embeddedServer(CIO, host = host, port = port) {
            keemunApplication(repository, renderer)
        }.start(wait = true)
    }
}

fun Application.keemunApplication(
    repository: GraphRepository,
    renderer: HtmlRenderer = HtmlRenderer(),
) {
    val renderCache = RenderCache(repository, renderer)

    routing {
        get("/") {
            call.respondHtml(renderCache.html(editable = true))
        }
        get("/keemun.html") {
            call.respondHtml(renderCache.html(editable = false))
        }
        get("/api/graph") {
            call.respondJson(repository.readJson())
        }
        get("/api/log") {
            call.respondJson(repository.readLogViewJson())
        }
        post("/api/changes") {
            call.mutate(repository, renderCache) {
                repository.propose(GraphJson.decodeProposal(receiveText()))
            }
        }
        post("/api/changes/{id}/accept") {
            val id = call.parameters["id"].orEmpty()
            call.mutate(repository, renderCache) { repository.accept(id) }
        }
        post("/api/changes/{id}/reject") {
            val id = call.parameters["id"].orEmpty()
            call.mutate(repository, renderCache) { repository.reject(id) }
        }
        get("/api/describe/{id}") {
            val id = call.parameters["id"].orEmpty()
            val from = call.request.queryParameters["from"]
            runCatching {
                TraceService.describe(repository.read(), id, from)
            }.fold(
                onSuccess = { description ->
                    call.respondText(description, ContentType.Text.Plain.withCharset(Charsets.UTF_8))
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
) {
    private var editableHtml: RenderSnapshot? = null
    private var staticHtml: RenderSnapshot? = null

    fun html(editable: Boolean): String {
        val snapshot = repository.snapshot()
        val current = if (editable) editableHtml else staticHtml
        if (current?.cacheKey == snapshot.cacheKey) {
            return current.html
        }

        val html = renderer.render(snapshot.log, editable)
        val next = RenderSnapshot(snapshot.cacheKey, html)
        if (editable) editableHtml = next else staticHtml = next
        return html
    }

    fun invalidate() {
        editableHtml = null
        staticHtml = null
    }
}

private data class RenderSnapshot(
    val cacheKey: GraphCacheKey,
    val html: String,
)

private suspend fun ApplicationCall.respondHtml(html: String) {
    respondText(html, ContentType.Text.Html.withCharset(Charsets.UTF_8))
}

private suspend fun ApplicationCall.respondJson(json: String) {
    respondText(json, ContentType.Application.Json.withCharset(Charsets.UTF_8))
}

/** Run a repository mutation, invalidate the render cache, and return the updated log view. */
private suspend fun ApplicationCall.mutate(
    repository: GraphRepository,
    renderCache: RenderCache,
    block: suspend ApplicationCall.() -> Unit,
) {
    runCatching { block() }.fold(
        onSuccess = {
            renderCache.invalidate()
            respondJson(repository.readLogViewJson())
        },
        onFailure = { error ->
            val details = if (error is GraphValidationException) error.errors else emptyList()
            respondError(HttpStatusCode.BadRequest, error.message ?: "Invalid change", details)
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
        ContentType.Application.Json.withCharset(Charsets.UTF_8),
        status,
    )
}

@Serializable
private data class ErrorPayload(
    val error: String,
    val details: List<String> = emptyList(),
)
