package io.heapy.keemun

import kotlin.random.Random

object SyntheticGraph {
    fun create(
        nodeCount: Int,
        edgeCount: Int,
        seed: Int,
        clusterCount: Int = defaultClusterCount(nodeCount),
    ): KeemunGraph {
        require(nodeCount > 0) { "node count must be positive" }
        require(edgeCount >= 0) { "edge count must not be negative" }
        require(clusterCount > 0) { "cluster count must be positive" }

        val random = Random(seed)
        val clusters = (1..nodeCount).groupBy { clusterForIndex(it, nodeCount, clusterCount) }
        val nodes = (1..nodeCount).map { index ->
            val cluster = clusterForIndex(index, nodeCount, clusterCount)
            val type = when {
                index % 25 == 0 -> NodeType.CONSTRAINT
                index % 11 == 0 -> NodeType.OPTION
                index % 7 == 0 -> NodeType.QUESTION
                index % 13 == 0 -> NodeType.OUTCOME
                else -> NodeType.DECISION
            }
            GraphNode(
                id = nodeId(index),
                type = type,
                title = "${type.wireName().replaceFirstChar { it.uppercase() }} ${index.toString().padStart(4, '0')}",
                summary = "Synthetic ${type.wireName()} node used for graph rendering and traversal performance checks.",
                status = if (type == NodeType.OPTION && index % 22 == 0) NodeStatus.REJECTED else NodeStatus.ACCEPTED,
                tags = listOf("synthetic", type.wireName(), "cluster-${cluster.toString().padStart(2, '0')}"),
                external = type == NodeType.CONSTRAINT,
            )
        }

        val usedPairs = mutableSetOf<Pair<Int, Int>>()
        val edges = buildList {
            var attempts = 0
            while (size < edgeCount && attempts < edgeCount * 20 + 100) {
                attempts += 1
                val index = size + 1
                val source = random.nextInt(1, nodeCount + 1)
                val sourceCluster = clusterForIndex(source, nodeCount, clusterCount)
                val sameCluster = clusterCount == 1 || random.nextDouble() < 0.78
                var target = if (sameCluster) {
                    clusters.getValue(sourceCluster).random(random)
                } else {
                    val targetCluster = clusters.keys.filter { it != sourceCluster }.random(random)
                    clusters.getValue(targetCluster).random(random)
                }
                if (nodeCount > 1) {
                    while (target == source) {
                        target = random.nextInt(1, nodeCount + 1)
                    }
                }
                if (!usedPairs.add(source to target)) {
                    continue
                }
                add(createEdge(index, source, target, random, seed))
            }
        }

        return KeemunGraph(
            metadata = GraphMetadata(
                title = "Keemun Synthetic $nodeCount",
                summary = "Synthetic graph with $nodeCount nodes, $edgeCount random connections, and $clusterCount clusters. Seed: $seed.",
            ),
            nodes = nodes,
            edges = edges,
        ).requireValid()
    }

    private fun createEdge(index: Int, source: Int, target: Int, random: Random, seed: Int): GraphEdge {
        val type = EdgeType.entries[random.nextInt(EdgeType.entries.size)]
        val polarity = if (type == EdgeType.FORBIDS || type == EdgeType.CONFLICTS) {
            EdgePolarity.NEGATIVE
        } else {
            EdgePolarity.POSITIVE
        }
        return GraphEdge(
            id = edgeId(index),
            source = nodeId(source),
            target = nodeId(target),
            type = type,
            rationale = "Synthetic rationale ${index.toString().padStart(5, '0')}: ${nodeId(source)} ${type.wireName()} ${nodeId(target)} for load testing.",
            status = if (polarity == EdgePolarity.NEGATIVE && index % 5 == 0) EdgeStatus.REJECTED else EdgeStatus.ACCEPTED,
            polarity = polarity,
            weight = random.nextInt(35, 101) / 100.0,
            order = index,
            criteria = listOf("performance", "random", "seed-$seed"),
        )
    }

    private fun nodeId(index: Int): String =
        "node-${index.toString().padStart(4, '0')}"

    private fun edgeId(index: Int): String =
        "edge-${index.toString().padStart(6, '0')}"

    private fun clusterForIndex(index: Int, nodeCount: Int, clusterCount: Int): Int =
        ((index - 1) * clusterCount / nodeCount) + 1

    private fun defaultClusterCount(nodeCount: Int): Int =
        (nodeCount / 64).coerceIn(4, 24)
}
