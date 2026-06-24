package io.heapy.keemun

import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class CliTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `init render and describe work together`() {
        val graphPath = tempDir.resolve("keemun.jsonl")
        val htmlPath = tempDir.resolve("keemun.html")
        val cli = KeemunCli()

        val init = runCli(cli, "init", "--file", graphPath.toString())
        assertEquals(0, init.code)
        assertTrue(Files.exists(graphPath))
        assertTrue(Files.readString(graphPath).startsWith("{\"kind\":\"header\""))

        val render = runCli(cli, "render", "--file", graphPath.toString(), "--out", htmlPath.toString())
        assertEquals(0, render.code)
        val html = Files.readString(htmlPath)
        assertTrue(html.contains("Keemun decision graph"))
        assertTrue(html.contains("function renderTimeline"))

        val describe = runCli(cli, "describe", "ksp", "--file", graphPath.toString(), "--from", "kotlin-language")
        assertEquals(0, describe.code)
        assertTrue(describe.out.contains("Path from Kotlin is the implementation language"))
    }

    @Test
    fun `propose accept and log drive the review workflow`() {
        val graphPath = tempDir.resolve("keemun.jsonl")
        val cli = KeemunCli()
        runCli(cli, "init", "--file", graphPath.toString())

        val payload = """{"message":"add node","records":[{"kind":"node","id":"newnode","type":"decision","title":"New Node"}]}"""
        val propose = runCli(cli, "propose", "--file", graphPath.toString(), "--json", payload)
        assertEquals(0, propose.code, propose.err)
        assertTrue(propose.out.contains("change-0002"))

        val proposedLog = runCli(cli, "log", "--file", graphPath.toString())
        assertTrue(proposedLog.out.contains("change-0002"))
        assertTrue(proposedLog.out.contains("proposed"))
        assertFalse(GraphRepository(graphPath.toString()).read().nodes.any { it.id == "newnode" })

        val accept = runCli(cli, "accept", "change-0002", "--file", graphPath.toString())
        assertEquals(0, accept.code, accept.err)
        assertTrue(GraphRepository(graphPath.toString()).read().nodes.any { it.id == "newnode" })

        val validate = runCli(cli, "validate", "--file", graphPath.toString())
        assertEquals(0, validate.code)
        assertTrue(validate.out.contains("change-sets"))
    }

    @Test
    fun `generate creates a deterministic synthetic log`() {
        val graphPath = tempDir.resolve("large.jsonl")
        val cli = KeemunCli()

        val generated = runCli(
            cli, "generate", "--file", graphPath.toString(),
            "--nodes", "1000", "--edges", "3000", "--seed", "7",
        )

        assertEquals(0, generated.code, generated.err)
        val graph = GraphRepository(graphPath.toString()).read()
        assertEquals(1000, graph.nodes.size)
        assertEquals(3000, graph.edges.size)
        assertEquals("edge-000001", graph.edges.first().id)
    }

    @Test
    fun `import migrates a legacy single-file graph`() {
        val legacy = tempDir.resolve("legacy.json")
        Files.writeString(legacy, GraphJson.encode(SampleGraph.create()))
        val graphPath = tempDir.resolve("keemun.jsonl")
        val cli = KeemunCli()

        val imported = runCli(cli, "import", "--from", legacy.toString(), "--file", graphPath.toString())
        assertEquals(0, imported.code, imported.err)
        assertTrue(GraphRepository(graphPath.toString()).read().nodes.any { it.id == "ksp" })
    }

    private fun runCli(cli: KeemunCli, vararg args: String): CliResult {
        val result = cli.command().test(args.toList())
        return CliResult(result.statusCode, result.stdout, result.stderr)
    }

    private data class CliResult(
        val code: Int,
        val out: String,
        val err: String,
    )
}
