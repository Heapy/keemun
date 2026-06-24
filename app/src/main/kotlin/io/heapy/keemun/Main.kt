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
import com.github.ajalt.clikt.parameters.options.required
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
            ImportCommand(),
            RenderCommand(renderer),
            ServeCommand(renderer),
            DescribeCommand(),
            ValidateCommand(),
            LogCommand(),
            InstallCommand(),
            ProposeCommand(),
            AcceptCommand(),
            RejectCommand(),
        )
    }

    override fun help(context: Context): String =
        """
        Keemun decision graph CLI

        The graph is an append-only JSONL log: every line is a record carrying a
        change_id, and records sharing a change_id form one reviewable change-set
        (proposed / accepted / rejected). The current graph is the projection of
        all accepted change-sets; nodes are decisions, constraints, questions,
        options, or outcomes, and edges are first-class rationale records.
        """.trimIndent()
}

private class InitCommand : CliktCommand(name = "init") {
    private val file by pathOption("--file", defaultGraphPath)
    private val force by option("--force").flag()

    override fun help(context: Context): String = "Create a sample decision graph log."

    override fun run() = runCommand {
        requireWritable(file, force)
        GraphRepository(file).createFrom(SampleGraph.create(), message = "Initial sample graph")
        echo("Wrote $file")
    }
}

private class GenerateCommand : CliktCommand(name = "generate") {
    private val file by pathOption("--file", KeemunPath("keemun-synthetic.jsonl"))
    private val nodeCount by option("--nodes").int().default(1_000).check("--nodes must be positive") { it > 0 }
    private val edgeCount by option("--edges").int().default(-1).check("--edges must not be negative") { it >= -1 }
    private val clusterCount by option("--clusters").int().default(-1).check("--clusters must be positive") { it == -1 || it > 0 }
    private val seed by option("--seed").int().default(42)
    private val force by option("--force").flag()

    override fun help(context: Context): String = "Create a deterministic synthetic graph log."

    override fun run() = runCommand {
        val resolvedEdgeCount = if (edgeCount == -1) (nodeCount * 12) / 10 else edgeCount
        val resolvedClusterCount = if (clusterCount == -1) {
            (nodeCount / 64).coerceIn(4, 24)
        } else {
            clusterCount
        }

        requireWritable(file, force)
        val graph = SyntheticGraph.create(nodeCount, resolvedEdgeCount, seed, resolvedClusterCount)
        GraphRepository(file).createFrom(graph, message = "Synthetic graph (seed $seed)")
        echo("Wrote $file with ${graph.nodes.size} nodes, ${graph.edges.size} edges, and $resolvedClusterCount clusters using seed $seed")
    }
}

private class ImportCommand : CliktCommand(name = "import") {
    private val from by option("--from").convert { KeemunPath(it) }.required()
    private val file by pathOption("--file", defaultGraphPath)
    private val force by option("--force").flag()

    override fun help(context: Context): String = "Import a legacy single-file JSON graph into a new JSONL log."

    override fun run() = runCommand {
        if (!KeemunFiles.exists(from)) {
            throw IllegalArgumentException("$from does not exist")
        }
        requireWritable(file, force)
        val graph = GraphJson.decode(KeemunFiles.readString(from))
        GraphRepository(file).createFrom(graph, message = "Imported from $from")
        echo("Imported $from into $file")
    }
}

private class RenderCommand(private val renderer: HtmlRenderer) : CliktCommand(name = "render") {
    private val file by pathOption("--file", defaultGraphPath)
    private val output by option("--out").convert { KeemunPath(it) }
    private val format by option("--format").convert { RenderFormat.parse(it) }.default(RenderFormat.HTML)

    override fun help(context: Context): String =
        "Render the current graph: --format html (human-reviewable) or markdown (agent-readable)."

    override fun run() = runCommand {
        val repository = GraphRepository(file)
        val target = output ?: defaultRenderPath(file, format)
        val log = repository.readLog()
        val content = when (format) {
            RenderFormat.HTML -> renderer.render(log, editable = false)
            RenderFormat.MARKDOWN -> MarkdownRenderer().render(log)
        }
        target.parent?.let(KeemunFiles::createDirectories)
        KeemunFiles.writeString(target, content)
        echo("Wrote $target")
    }
}

private class ServeCommand(private val renderer: HtmlRenderer) : CliktCommand(name = "serve") {
    private val file by pathOption("--file", defaultGraphPath)
    private val host by option("--host").default("127.0.0.1")
    private val port by option("--port").int().default(8080)

    override fun help(context: Context): String = "Serve an editable graph over HTTP."

    override fun run() = runCommand {
        GraphRepository(file).read()
        echo("Serving $file at http://$host:$port")
        KeemunServer(GraphRepository(file), renderer).start(host, port)
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

    override fun help(context: Context): String = "Validate a graph JSONL log."

    override fun run() = runCommand {
        val log = GraphRepository(file).readLog()
        val graph = log.current()
        echo("Valid log: ${log.changes().size} change-sets, ${graph.nodes.size} nodes, ${graph.edges.size} edges")
    }
}

private class LogCommand : CliktCommand(name = "log") {
    private val file by pathOption("--file", defaultGraphPath)

    override fun help(context: Context): String = "List change-sets in the log."

    override fun run() = runCommand {
        GraphRepository(file).readLog().changes().forEach { change ->
            val nodes = change.records.count { it is NodeRecord }
            val edges = change.records.count { it is EdgeRecord }
            val deletes = change.records.count { it is DeleteRecord }
            val counts = buildList {
                if (nodes > 0) add("$nodes node${plural(nodes)}")
                if (edges > 0) add("$edges edge${plural(edges)}")
                if (deletes > 0) add("$deletes delete${plural(deletes)}")
            }.joinToString(", ").ifEmpty { "no entities" }
            echo("${change.seq}\t${change.changeId}\t${change.status.wireName().padEnd(8)}\t${change.message ?: ""}\t($counts)")
        }
    }

    private fun plural(count: Int): String = if (count == 1) "" else "s"
}

private class InstallCommand : NoOpCliktCommand(name = "install") {
    init {
        subcommands(InstallSkillCommand())
    }

    override fun help(context: Context): String = "Install bundled keemun helper assets."
}

private class InstallSkillCommand : CliktCommand(name = "skill") {
    private val agent by option("--agent")
        .convert { SkillInstallAgent.parse(it) }
        .default(SkillInstallAgent.ALL)
    private val scope by option("--scope")
        .convert { SkillInstallScope.parse(it) }
        .default(SkillInstallScope.PROJECT)
    private val projectDir by option("--project-dir")
        .convert { KeemunPath(it) }
        .default(KeemunPath("."))
    private val home by option("--home").convert { KeemunPath(it) }
    private val force by option("--force").flag()

    override fun help(context: Context): String =
        "Install the bundled keemun skill for Codex and/or Claude Code."

    override fun run() = runCommand {
        SkillInstaller.install(agent, scope, projectDir, home, force).forEach { result ->
            val action = when (result.status) {
                SkillInstallStatus.INSTALLED -> "Installed"
                SkillInstallStatus.UPDATED -> "Updated"
                SkillInstallStatus.UNCHANGED -> "Already up to date"
            }
            echo("$action ${result.agent.wireName} skill at ${result.path}")
        }
    }
}

private class ProposeCommand : CliktCommand(name = "propose") {
    private val file by pathOption("--file", defaultGraphPath)
    private val input by option("--input").convert { KeemunPath(it) }
    private val json by option("--json")
    private val id by option("--id")
    private val message by option("--message")
    private val author by option("--author")

    override fun help(context: Context): String =
        "Append a proposed change-set from a JSON payload (--input <file> or --json <text>)."

    override fun run() = runCommand {
        val payload = when {
            json != null -> json!!
            input != null -> KeemunFiles.readString(input!!)
            else -> throw IllegalArgumentException("Provide the change payload via --input <file> or --json <text>")
        }
        val parsed = GraphJson.decodeProposal(payload)
        val proposal = parsed.copy(
            changeId = id ?: parsed.changeId,
            message = message ?: parsed.message,
            author = author ?: parsed.author,
            status = ChangeStatus.PROPOSED,
        )
        val change = GraphRepository(file).propose(proposal)
        echo("Proposed ${change.changeId} (${change.records.size} records)")
    }
}

private class AcceptCommand : CliktCommand(name = "accept") {
    private val changeId by argument("change-id")
    private val file by pathOption("--file", defaultGraphPath)
    private val author by option("--author")

    override fun help(context: Context): String = "Accept a proposed change-set."

    override fun run() = runCommand {
        val change = GraphRepository(file).accept(changeId, author)
        echo("Accepted ${change.changeId}")
    }
}

private class RejectCommand : CliktCommand(name = "reject") {
    private val changeId by argument("change-id")
    private val file by pathOption("--file", defaultGraphPath)
    private val author by option("--author")

    override fun help(context: Context): String = "Reject a proposed change-set."

    override fun run() = runCommand {
        val change = GraphRepository(file).reject(changeId, author)
        echo("Rejected ${change.changeId}")
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

private fun requireWritable(file: KeemunPath, force: Boolean) {
    if (KeemunFiles.exists(file) && !force) {
        throw IllegalArgumentException("$file already exists; pass --force to overwrite it")
    }
}

private fun ParameterHolder.pathOption(name: String, default: KeemunPath) =
    option(name).convert { KeemunPath(it) }.default(default)

private val defaultGraphPath: KeemunPath = KeemunPath("keemun.jsonl")

private fun defaultRenderPath(input: KeemunPath, format: RenderFormat): KeemunPath {
    val fileName = input.fileName
    val base = when {
        fileName.endsWith(".jsonl") -> fileName.substringBeforeLast(".jsonl")
        fileName.endsWith(".json") -> fileName.substringBeforeLast(".json")
        else -> "keemun"
    }
    val name = "$base.${format.extension}"
    return input.parent?.resolve(name) ?: KeemunPath(name)
}
