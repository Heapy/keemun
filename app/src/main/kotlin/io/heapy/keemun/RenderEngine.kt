package io.heapy.keemun

enum class RenderEngine(val wireName: String) {
    SVG("svg"),
    COSMOGRAPH("cosmograph");

    companion object {
        fun parse(value: String?): RenderEngine {
            val normalized = value?.trim()?.lowercase()
            return entries.firstOrNull { it.wireName == normalized }
                ?: if (normalized == null || normalized.isBlank()) {
                    SVG
                } else {
                    throw IllegalArgumentException("--engine must be one of: ${entries.joinToString { it.wireName }}")
                }
        }
    }
}
