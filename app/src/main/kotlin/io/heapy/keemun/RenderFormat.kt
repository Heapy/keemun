package io.heapy.keemun

/** Output format for the `render` command: HTML for humans, Markdown for agents. */
enum class RenderFormat(val wireName: String, val extension: String) {
    HTML("html", "html"),
    MARKDOWN("markdown", "md");

    companion object {
        fun parse(value: String?): RenderFormat {
            return when (val normalized = value?.trim()?.lowercase()) {
                null, "", "html" -> HTML
                "markdown", "md" -> MARKDOWN
                else -> throw IllegalArgumentException(
                    "--format must be one of: ${entries.joinToString { it.wireName }} (got '$normalized')",
                )
            }
        }
    }
}
