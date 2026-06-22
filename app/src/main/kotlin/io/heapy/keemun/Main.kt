package io.heapy.keemun

import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(KeemunCli().run(args, System.out, System.err))
}

class KeemunCli(
    private val renderer: HtmlRenderer = HtmlRenderer(),
) {
    fun run(args: Array<String>, out: PrintStream, err: PrintStream): Int {
        if (args.isEmpty() || args[0] == "--help" || args[0] == "-h") {
            out.println(usage())
            return 0
        }

        return runCatching {
            val command = args[0]
            val parsed = ParsedArgs(args.drop(1))
            when (command) {
                "init" -> init(parsed, out)
                "generate" -> generate(parsed, out)
                "render" -> render(parsed, out)
                "serve" -> serve(parsed, out)
                "describe" -> describe(parsed, out)
                "validate" -> validate(parsed, out)
                else -> {
                    err.println("Unknown command: $command")
                    err.println(usage())
                    2
                }
            }
        }.getOrElse { error ->
            err.println(error.message ?: error.toString())
            1
        }
    }

    private fun init(args: ParsedArgs, out: PrintStream): Int {
        val file = args.pathOption("--file", defaultGraphPath)
        val force = args.flag("--force")
        if (Files.exists(file) && !force) {
            throw IllegalArgumentException("$file already exists; pass --force to overwrite it")
        }
        GraphRepository(file).write(SampleGraph.create())
        out.println("Wrote $file")
        return 0
    }

    private fun generate(args: ParsedArgs, out: PrintStream): Int {
        val file = args.pathOption("--file", Path.of("keemun-synthetic.json"))
        val nodeCount = args.positiveInt("--nodes", 1_000)
        val edgeCount = args.nonNegativeInt("--edges", (nodeCount * 12) / 10)
        val clusterCount = args.positiveInt("--clusters", (nodeCount / 64).coerceIn(4, 24))
        val seed = args.intOption("--seed", 42)
        val force = args.flag("--force")
        if (Files.exists(file) && !force) {
            throw IllegalArgumentException("$file already exists; pass --force to overwrite it")
        }

        val graph = GraphRepository(file).write(SyntheticGraph.create(nodeCount, edgeCount, seed, clusterCount))
        out.println("Wrote $file with ${graph.nodes.size} nodes, ${graph.edges.size} edges, and $clusterCount clusters using seed $seed")
        return 0
    }

    private fun render(args: ParsedArgs, out: PrintStream): Int {
        val file = args.pathOption("--file", defaultGraphPath)
        val repository = GraphRepository(file)
        val output = args.pathOption("--out", defaultHtmlPath(file))
        val engine = RenderEngine.parse(args.option("--engine"))
        val graph = repository.read()
        val parent = output.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        Files.writeString(output, renderer.render(graph, editable = false, engine = engine))
        out.println("Wrote $output using ${engine.wireName}")
        return 0
    }

    private fun serve(args: ParsedArgs, out: PrintStream): Int {
        val file = args.pathOption("--file", defaultGraphPath)
        val host = args.option("--host") ?: "127.0.0.1"
        val portText = args.option("--port")
        val port = portText?.toIntOrNull()
            ?: if (portText == null) 8080 else throw IllegalArgumentException("--port must be an integer")
        val engine = RenderEngine.parse(args.option("--engine"))
        GraphRepository(file).read()
        out.println("Serving $file at http://$host:$port using ${engine.wireName}")
        KeemunServer(GraphRepository(file), renderer, engine).start(host, port)
        return 0
    }

    private fun describe(args: ParsedArgs, out: PrintStream): Int {
        val file = args.pathOption("--file", defaultGraphPath)
        val nodeId = args.positionals.firstOrNull()
            ?: throw IllegalArgumentException("describe requires a node id")
        val from = args.option("--from")
        out.println(TraceService.describe(GraphRepository(file).read(), nodeId, from))
        return 0
    }

    private fun validate(args: ParsedArgs, out: PrintStream): Int {
        val file = args.pathOption("--file", defaultGraphPath)
        val graph = GraphRepository(file).read()
        out.println("Valid graph: ${graph.nodes.size} nodes, ${graph.edges.size} edges")
        return 0
    }

    private fun usage(): String =
        """
        Keemun decision graph CLI

        Commands:
          init [--file keemun.json] [--force]
          generate [--file keemun-synthetic.json] [--nodes 1000] [--edges 1200] [--clusters 15] [--seed 42] [--force]
          render [--file keemun.json] [--out keemun.html] [--engine svg|cosmograph]
          serve [--file keemun.json] [--host 127.0.0.1] [--port 8080] [--engine svg|cosmograph]
          describe <node-id> [--file keemun.json] [--from <node-id>]
          validate [--file keemun.json]

        The JSON format is a property graph: nodes are decisions, constraints,
        questions, options, or outcomes; edges are first-class rationale records
        with Kruchten-style relationship types and QOC-style weights.
        """.trimIndent()

    private class ParsedArgs(args: List<String>) {
        val positionals: List<String>
        private val options: Map<String, String?>

        init {
            val parsedOptions = linkedMapOf<String, String?>()
            val parsedPositionals = mutableListOf<String>()
            var index = 0
            while (index < args.size) {
                val token = args[index]
                if (token.startsWith("--")) {
                    val value = args.getOrNull(index + 1)
                    if (value != null && !value.startsWith("--")) {
                        parsedOptions[token] = value
                        index += 2
                    } else {
                        parsedOptions[token] = null
                        index += 1
                    }
                } else {
                    parsedPositionals += token
                    index += 1
                }
            }
            options = parsedOptions
            positionals = parsedPositionals
        }

        fun option(name: String): String? = options[name]

        fun flag(name: String): Boolean = options.containsKey(name)

        fun pathOption(name: String, default: Path): Path =
            option(name)?.let(Path::of) ?: default

        fun intOption(name: String, default: Int): Int =
            option(name)?.toIntOrNull() ?: if (option(name) == null) {
                default
            } else {
                throw IllegalArgumentException("$name must be an integer")
            }

        fun positiveInt(name: String, default: Int): Int =
            intOption(name, default).also {
                if (it <= 0) {
                    throw IllegalArgumentException("$name must be positive")
                }
            }

        fun nonNegativeInt(name: String, default: Int): Int =
            intOption(name, default).also {
                if (it < 0) {
                    throw IllegalArgumentException("$name must not be negative")
                }
            }
    }

    companion object {
        private val defaultGraphPath: Path = Path.of("keemun.json")

        private fun defaultHtmlPath(input: Path): Path {
            val name = if (input.fileName.toString().endsWith(".json")) {
                input.fileName.nameWithoutExtension + ".html"
            } else {
                "keemun.html"
            }
            return input.parent?.resolve(name) ?: Path.of(name)
        }
    }
}
