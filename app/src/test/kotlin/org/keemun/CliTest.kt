package org.keemun

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path

class CliTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `init render and describe work together`() {
        val graphPath = tempDir.resolve("keemun.json")
        val htmlPath = tempDir.resolve("keemun.html")
        val cli = KeemunCli()

        val init = runCli(cli, "init", "--file", graphPath.toString())
        assertEquals(0, init.code)
        assertTrue(Files.exists(graphPath))

        val render = runCli(cli, "render", "--file", graphPath.toString(), "--out", htmlPath.toString())
        assertEquals(0, render.code)
        assertTrue(Files.readString(htmlPath).contains("Keemun decision graph"))

        val cosmographPath = tempDir.resolve("keemun-cosmograph.html")
        val cosmographRender = runCli(
            cli,
            "render",
            "--file",
            graphPath.toString(),
            "--out",
            cosmographPath.toString(),
            "--engine",
            "cosmograph",
        )
        assertEquals(0, cosmographRender.code)
        assertTrue(Files.readString(cosmographPath).contains("cosmograph-container"))

        val describe = runCli(cli, "describe", "ksp", "--file", graphPath.toString(), "--from", "kotlin-language")
        assertEquals(0, describe.code)
        assertTrue(describe.out.contains("Path from Kotlin is the implementation language"))
    }

    @Test
    fun `generate creates deterministic synthetic graph`() {
        val graphPath = tempDir.resolve("large.json")
        val cli = KeemunCli()

        val generated = runCli(
            cli,
            "generate",
            "--file",
            graphPath.toString(),
            "--nodes",
            "1000",
            "--edges",
            "3000",
            "--seed",
            "7",
        )

        assertEquals(0, generated.code, generated.err)
        val graph = GraphRepository(graphPath).read()
        assertEquals(1000, graph.nodes.size)
        assertEquals(3000, graph.edges.size)
        assertEquals("edge-000001", graph.edges.first().id)
    }

    private fun runCli(cli: KeemunCli, vararg args: String): CliResult {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val code = cli.run(args.toList().toTypedArray(), PrintStream(out), PrintStream(err))
        return CliResult(code, out.toString(), err.toString())
    }

    private data class CliResult(
        val code: Int,
        val out: String,
        val err: String,
    )
}
