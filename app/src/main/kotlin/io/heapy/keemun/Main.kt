package io.heapy.keemun

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

fun main(args: Array<String>) {
    KeemunCli().command().main(args)
}

class KeemunCli(
    private val renderer: HtmlRenderer = HtmlRenderer(),
) {
    fun command(): CliktCommand =
        KeemunCommand(renderer)
}

private class KeemunCommand(renderer: HtmlRenderer) : NoOpCliktCommand(name = "keemun") {
    init {
        subcommands(
            InitCommand(),
            GenerateCommand(),
            RenderCommand(renderer),
            ServeCommand(renderer),
            DescribeCommand(),
            ValidateCommand(),
        )
    }

    override fun help(context: Context): String =
        """
        Keemun decision graph CLI

        The JSON format is a property graph: nodes are decisions, constraints,
        questions, options, or outcomes; edges are first-class rationale records
        with Kruchten-style relationship types and QOC-style weights.
        """.trimIndent()
}

private class InitCommand : CliktCommand(name = "init") {
    private val file by pathOption("--file", defaultGraphPath)
    private val force by option("--force").flag()

    override fun help(context: Context): String = "Create a sample decision graph."

    override fun run() = runCommand {
        if (KeemunFiles.exists(file) && !force) {
            throw IllegalArgumentException("$file already exists; pass --force to overwrite it")
        }
        GraphRepository(file).write(SampleGraph.create())
        echo("Wrote $file")
    }
}

private class GenerateCommand : CliktCommand(name = "generate") {
    private val file by pathOption("--file", KeemunPath("keemun-synthetic.json"))
    private val nodeCount by option("--nodes").int().default(1_000).check("--nodes must be positive") { it > 0 }
    private val edgeCount by option("--edges").int().default(-1).check("--edges must not be negative") { it >= -1 }
    private val clusterCount by option("--clusters").int().default(-1).check("--clusters must be positive") { it == -1 || it > 0 }
    private val seed by option("--seed").int().default(42)
    private val force by option("--force").flag()

    override fun help(context: Context): String = "Create a deterministic synthetic graph."

    override fun run() = runCommand {
        val resolvedEdgeCount = if (edgeCount == -1) (nodeCount * 12) / 10 else edgeCount
        val resolvedClusterCount = if (clusterCount == -1) {
            (nodeCount / 64).coerceIn(4, 24)
        } else {
            clusterCount
        }

        if (KeemunFiles.exists(file) && !force) {
            throw IllegalArgumentException("$file already exists; pass --force to overwrite it")
        }

        val graph = GraphRepository(file).write(
            SyntheticGraph.create(nodeCount, resolvedEdgeCount, seed, resolvedClusterCount),
        )
        echo("Wrote $file with ${graph.nodes.size} nodes, ${graph.edges.size} edges, and $resolvedClusterCount clusters using seed $seed")
    }
}

private class RenderCommand(private val renderer: HtmlRenderer) : CliktCommand(name = "render") {
    private val file by pathOption("--file", defaultGraphPath)
    private val output by option("--out").convert { KeemunPath(it) }
    private val engineName by option("--engine").default(RenderEngine.SVG.wireName)

    override fun help(context: Context): String = "Render a graph to a standalone HTML file."

    override fun run() = runCommand {
        val repository = GraphRepository(file)
        val target = output ?: defaultHtmlPath(file)
        val engine = RenderEngine.parse(engineName)
        val graph = repository.read()
        target.parent?.let(KeemunFiles::createDirectories)
        KeemunFiles.writeString(target, renderer.render(graph, editable = false, engine = engine))
        echo("Wrote $target using ${engine.wireName}")
    }
}

private class ServeCommand(private val renderer: HtmlRenderer) : CliktCommand(name = "serve") {
    private val file by pathOption("--file", defaultGraphPath)
    private val host by option("--host").default("127.0.0.1")
    private val port by option("--port").int().default(8080)
    private val engineName by option("--engine").default(RenderEngine.SVG.wireName)

    override fun help(context: Context): String = "Serve an editable graph over HTTP."

    override fun run() = runCommand {
        val engine = RenderEngine.parse(engineName)
        GraphRepository(file).read()
        echo("Serving $file at http://$host:$port using ${engine.wireName}")
        KeemunServer(GraphRepository(file), renderer, engine).start(host, port)
    }
}

private class DescribeCommand : CliktCommand(name = "describe") {
    private val nodeId by argument("node-id")
    private val file by pathOption("--file", defaultGraphPath)
    private val from by option("--from")

    override fun help(context: Context): String = "Describe a node and its rationale trace."

    override fun run() = runCommand {
        echo(TraceService.describe(GraphRepository(file).read(), nodeId, from))
    }
}

private class ValidateCommand : CliktCommand(name = "validate") {
    private val file by pathOption("--file", defaultGraphPath)

    override fun help(context: Context): String = "Validate a graph JSON file."

    override fun run() = runCommand {
        val graph = GraphRepository(file).read()
        echo("Valid graph: ${graph.nodes.size} nodes, ${graph.edges.size} edges")
    }
}

private fun CliktCommand.runCommand(block: () -> Unit) {
    try {
        block()
    } catch (error: ProgramResult) {
        throw error
    } catch (error: Exception) {
        echo(error.message ?: error.toString(), err = true)
        throw ProgramResult(1)
    }
}

private fun ParameterHolder.pathOption(name: String, default: KeemunPath) =
    option(name).convert { KeemunPath(it) }.default(default)

private val defaultGraphPath: KeemunPath = KeemunPath("keemun.json")

private fun defaultHtmlPath(input: KeemunPath): KeemunPath {
    val fileName = input.fileName
    val name = if (fileName.endsWith(".json")) {
        fileName.substringBeforeLast(".json") + ".html"
    } else {
        "keemun.html"
    }
    return input.parent?.resolve(name) ?: KeemunPath(name)
}
