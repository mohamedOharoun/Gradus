package es.ulpgc.pamn.dominio.course.publication

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// Metadatos ligeros extraídos del fichero JSON para mostrar la tarjeta del borrador incluso
// antes de que el servidor lo valide o aunque el JSON tenga errores de sintaxis.
data class CourseDraftPreview(
    val title: String,
    val moduleCount: Int?,
) {
    companion object {
        private const val UTF8_BOM = "\uFEFF"
        private const val JSON_SUFFIX = ".json"

        fun fromBytes(bytes: ByteArray, fallbackFileName: String): CourseDraftPreview =
            fromJson(bytes.decodeToString(), fallbackFileName)

        fun fromJson(jsonText: String, fallbackFileName: String): CourseDraftPreview {
            val fallbackTitle = cleanFallbackFileName(fallbackFileName)
            val cleanText = jsonText.removePrefix(UTF8_BOM)
            if (cleanText.isBlank()) {
                return CourseDraftPreview(title = fallbackTitle, moduleCount = null)
            }

            val root = runCatching { Json.parseToJsonElement(cleanText) }.getOrNull() as? JsonObject
                ?: return CourseDraftPreview(title = fallbackTitle, moduleCount = null)

            val title = (root["title"] as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: fallbackTitle

            val moduleCount = (root["modules"] as? JsonArray)?.size

            return CourseDraftPreview(title = title, moduleCount = moduleCount)
        }

        private fun cleanFallbackFileName(fileName: String): String {
            val trimmed = fileName.trim()
            val withoutExtension = if (trimmed.endsWith(JSON_SUFFIX, ignoreCase = true)) {
                trimmed.dropLast(JSON_SUFFIX.length)
            } else {
                trimmed
            }
            return withoutExtension.trim()
        }
    }
}
