package io.heapy.keemun

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TraceAndRenderTest {
    @Test
    fun `describes rationale path between nodes`() {
        val description = TraceService.describe(
            SampleGraph.create(),
            targetId = "ksp",
            fromId = "kotlin-language",
        )

        assertTrue(description.contains("Kotlin is the implementation language"))
        assertTrue(description.contains("Use KSP for compile-time integration"))
        assertTrue(description.contains("constrains"))
        assertTrue(description.contains("Rationale:"))
    }

    @Test
    fun `describes incoming negative alternative rationale`() {
        val description = TraceService.describe(SampleGraph.create(), targetId = "compile-time-framework")

        assertTrue(description.contains("Runtime reflection conflicts Build a compile-time framework"))
        assertTrue(description.contains("negative"))
        assertTrue(description.contains("moves wiring mistakes to execution time"))
    }

    @Test
    fun `renders self contained interactive html`() {
        val html = HtmlRenderer().render(SampleGraph.create())

        assertTrue(html.contains("<svg id=\"graph-svg\""))
        assertTrue(html.contains("How this node was reached"))
        assertTrue(html.contains("\"schema_version\": 1"))
        assertTrue(html.contains("Build a compile-time framework"))
        assertTrue(html.contains("function selectNode"))
    }

    @Test
    fun `renders cosmograph html shell`() {
        val html = HtmlRenderer().render(SampleGraph.create(), engine = RenderEngine.COSMOGRAPH)

        assertTrue(html.contains("https://esm.sh/@cosmograph/cosmograph"))
        assertTrue(html.contains("https://cdn.jsdelivr.net/npm/@cosmograph/cosmograph/+esm"))
        assertTrue(html.contains("cosmograph-container"))
        assertTrue(html.contains("id=\"node-form\""))
        assertTrue(html.contains("function saveNodeFromForm"))
        assertTrue(html.contains("id=\"new-node\""))
        assertTrue(html.contains("linkSourceIndexBy: 'sourceIndex'"))
        assertFalse(html.contains("pointClusterBy"))
        assertFalse(html.contains("simulationCluster"))
        assertFalse(html.contains("showClusterLabels"))
        assertTrue(html.contains("\"schema_version\": 1"))
    }
}
