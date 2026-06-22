package io.heapy.keemun

object SampleGraph {
    fun create(): KeemunGraph =
        KeemunGraph(
            metadata = GraphMetadata(
                title = "Keemun",
                summary = "A decision graph for architectural documentation.",
            ),
            nodes = listOf(
                GraphNode(
                    id = "compile-time-framework",
                    type = NodeType.DECISION,
                    title = "Build a compile-time framework",
                    summary = "Generate framework wiring before runtime so failures surface earlier and startup stays predictable.",
                    tags = listOf("architecture", "performance"),
                ),
                GraphNode(
                    id = "framework-code",
                    type = NodeType.DECISION,
                    title = "Use a framework boundary",
                    summary = "Keep application code scalable and testable by centralizing conventions in a framework layer.",
                    tags = listOf("architecture"),
                ),
                GraphNode(
                    id = "kotlin-language",
                    type = NodeType.CONSTRAINT,
                    title = "Kotlin is the implementation language",
                    summary = "The project is written in Kotlin, so integration points should fit Kotlin tooling and idioms.",
                    tags = listOf("external"),
                    external = true,
                ),
                GraphNode(
                    id = "ksp",
                    type = NodeType.DECISION,
                    title = "Use KSP for compile-time integration",
                    summary = "KSP provides a Kotlin-native symbol processing API for code generation.",
                    tags = listOf("tooling"),
                ),
                GraphNode(
                    id = "runtime-reflection",
                    type = NodeType.OPTION,
                    title = "Runtime reflection",
                    summary = "A rejected implementation option that discovers wiring while the application starts.",
                    status = NodeStatus.REJECTED,
                    tags = listOf("alternative"),
                ),
            ),
            edges = listOf(
                GraphEdge(
                    id = "compile-time-enables-ksp",
                    source = "compile-time-framework",
                    target = "ksp",
                    type = EdgeType.ENABLES,
                    rationale = "Once framework wiring is moved to compile time, a symbol processor is needed to inspect Kotlin declarations and generate code.",
                    weight = 0.86,
                    order = 4,
                    criteria = listOf("tooling-fit", "maintainability"),
                ),
                GraphEdge(
                    id = "framework-enables-compile-time",
                    source = "framework-code",
                    target = "compile-time-framework",
                    type = EdgeType.ENABLES,
                    rationale = "A framework boundary creates a small set of conventions that can be checked and generated ahead of runtime.",
                    weight = 0.82,
                    order = 2,
                    criteria = listOf("testability", "startup"),
                ),
                GraphEdge(
                    id = "kotlin-constrains-ksp",
                    source = "kotlin-language",
                    target = "ksp",
                    type = EdgeType.CONSTRAINS,
                    rationale = "Because Kotlin is fixed externally, the generation mechanism should understand Kotlin symbols without falling back to Java-only annotation processing.",
                    weight = 0.9,
                    order = 3,
                    criteria = listOf("language-fit"),
                ),
                GraphEdge(
                    id = "kotlin-enables-framework",
                    source = "kotlin-language",
                    target = "framework-code",
                    type = EdgeType.ENABLES,
                    rationale = "Kotlin gives enough type-system support and compiler tooling to express framework contracts without hiding application behavior.",
                    weight = 0.74,
                    order = 1,
                    criteria = listOf("language-fit", "testability"),
                ),
                GraphEdge(
                    id = "runtime-conflicts-compile-time",
                    source = "runtime-reflection",
                    target = "compile-time-framework",
                    type = EdgeType.CONFLICTS,
                    rationale = "Runtime discovery was considered, but it moves wiring mistakes to execution time and can slow cold start, so it conflicts with the chosen compile-time direction.",
                    status = EdgeStatus.REJECTED,
                    polarity = EdgePolarity.NEGATIVE,
                    weight = 0.93,
                    order = 5,
                    criteria = listOf("correctness", "startup"),
                ),
            ),
        ).requireValid()
}
