package io.heapy.keemun

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TraceAndRenderTest {
    private fun sampleLog(): GraphLog =
        GraphLog(
            listOf(HeaderRecord()) +
                changeRecordsFromGraph(SampleGraph.create(), "change-0001", ChangeStatus.ACCEPTED, "sample"),
        )

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
    fun `renders self contained interactive html with history and authoring`() {
        val html = HtmlRenderer().render(sampleLog(), editable = true)

        assertTrue(html.contains("<svg id=\"graph-svg\""))
        assertTrue(html.contains("How this node was reached"))
        assertTrue(html.contains("Build a compile-time framework"))
        assertTrue(html.contains("function projectGraph"))
        assertTrue(html.contains("function renderTimeline"))
        assertTrue(html.contains("Author a change"))
        assertTrue(html.contains("\"schema_version\":1"))
    }

    @Test
    fun `static render hides authoring controls`() {
        val html = HtmlRenderer().render(sampleLog(), editable = false)

        assertTrue(html.contains("const editorEnabled = false"))
        assertTrue(html.contains("function renderTimeline"))
    }

    @Test
    fun `renders agent readable markdown with nodes change history and edge rationale`() {
        val markdown = MarkdownRenderer().render(sampleLog())

        assertTrue(markdown.startsWith("# "))
        assertTrue(markdown.contains("## Change history"))
        assertTrue(markdown.contains("| `change-0001` | accepted |"))
        assertTrue(markdown.contains("### Decisions"))
        assertTrue(markdown.contains("**Build a compile-time framework** (`compile-time-framework`)"))
        assertTrue(markdown.contains("## Rationale (edges)"))
        // first-class rationale and polarity must survive into the markdown
        assertTrue(markdown.contains("—conflicts→"))
        assertTrue(markdown.contains("negative"))
        assertTrue(markdown.contains("moves wiring mistakes to execution time"))
    }
}
