package es.ulpgc.pamn.dominio.course.publication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CourseDraftPreviewTest {

    @Test
    fun `extracts trimmed title and module count from valid json`() {
        val json = """
            {
              "formatVersion": 1,
              "title": "  Introducción a los grafos  ",
              "modules": [{"id": "m1"}, {"id": "m2"}, {"id": "m3"}]
            }
        """.trimIndent()

        val preview = CourseDraftPreview.fromJson(json, fallbackFileName = "curso.json")

        assertEquals(CourseDraftPreview(title = "Introducción a los grafos", moduleCount = 3), preview)
    }

    @Test
    fun `extracts zero module count when modules array is empty`() {
        val json = """{"title": "Curso vacío", "modules": []}"""

        val preview = CourseDraftPreview.fromJson(json, fallbackFileName = "vacio.json")

        assertEquals(CourseDraftPreview(title = "Curso vacío", moduleCount = 0), preview)
    }

    @Test
    fun `handles utf8 bom in both string and byte array`() {
        val jsonWithBom = "\uFEFF" + """{"title": "Con BOM", "modules": [{"id": "m1"}]}"""
        val bytesWithBom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            """{"title": "Con BOM", "modules": [{"id": "m1"}]}""".encodeToByteArray()

        assertEquals(
            CourseDraftPreview(title = "Con BOM", moduleCount = 1),
            CourseDraftPreview.fromJson(jsonWithBom, fallbackFileName = "bom.json"),
        )
        assertEquals(
            CourseDraftPreview(title = "Con BOM", moduleCount = 1),
            CourseDraftPreview.fromBytes(bytesWithBom, fallbackFileName = "bom.json"),
        )
    }

    @Test
    fun `falls back to cleaned file name when title is missing blank or not a string`() {
        val cases = listOf(
            """{"modules": [{"id": "m1"}]}""",
            """{"title": null, "modules": [{"id": "m1"}]}""",
            """{"title": "   ", "modules": [{"id": "m1"}]}""",
            """{"title": 42, "modules": [{"id": "m1"}]}""",
            """{"title": true, "modules": [{"id": "m1"}]}""",
            """{"title": ["a"], "modules": [{"id": "m1"}]}""",
            """{"title": {"es": "Hola"}, "modules": [{"id": "m1"}]}""",
        )

        cases.forEach { json ->
            val preview = CourseDraftPreview.fromJson(json, fallbackFileName = "mi-curso.json")
            assertEquals("mi-curso", preview.title)
            assertEquals(1, preview.moduleCount)
        }
    }

    @Test
    fun `cleans fallback file name stripping json suffix case insensitively and trimming`() {
        val broken = "{"

        assertEquals("grafos", CourseDraftPreview.fromJson(broken, "grafos.json").title)
        assertEquals("grafos", CourseDraftPreview.fromJson(broken, "  grafos.JSON  ").title)
        assertEquals("grafos", CourseDraftPreview.fromJson(broken, "grafos .Json").title)
        assertEquals("curso.v2", CourseDraftPreview.fromJson(broken, "curso.v2.json").title)
        assertEquals("apuntes.txt", CourseDraftPreview.fromJson(broken, "  apuntes.txt  ").title)
        assertEquals("curso.json.bak", CourseDraftPreview.fromJson(broken, "curso.json.bak").title)
    }

    @Test
    fun `handles edge case fallback file names like dot json or only spaces without throwing`() {
        val broken = "not json"

        assertEquals("", CourseDraftPreview.fromJson(broken, ".json").title)
        assertEquals("", CourseDraftPreview.fromJson(broken, "  .JSON  ").title)
        assertEquals("", CourseDraftPreview.fromJson(broken, "   ").title)
        assertEquals("", CourseDraftPreview.fromJson(broken, "").title)
    }

    @Test
    fun `returns null module count when modules is missing null or not an array`() {
        val cases = listOf(
            """{"title": "Grafos"}""",
            """{"title": "Grafos", "modules": null}""",
            """{"title": "Grafos", "modules": 5}""",
            """{"title": "Grafos", "modules": "dos"}""",
            """{"title": "Grafos", "modules": {"m1": {}}}""",
        )

        cases.forEach { json ->
            val preview = CourseDraftPreview.fromJson(json, fallbackFileName = "fallback.json")
            assertEquals("Grafos", preview.title)
            assertNull(preview.moduleCount)
        }
    }

    @Test
    fun `never throws on empty truncated broken json or non object roots`() {
        val brokenInputs = listOf(
            "",
            "   \n\t  ",
            "{",
            """{"title": "Sin cerrar""",
            "esto no es json",
            """["array", "en", "raiz"]""",
            "123",
            "true",
            "null",
        )

        brokenInputs.forEach { input ->
            val preview = CourseDraftPreview.fromJson(input, fallbackFileName = "respaldo.json")
            assertEquals(CourseDraftPreview(title = "respaldo", moduleCount = null), preview)
        }
    }

    @Test
    fun `never throws on invalid utf8 bytes`() {
        val invalidUtf8 = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x80.toByte())

        val preview = CourseDraftPreview.fromBytes(invalidUtf8, fallbackFileName = "binario.json")

        assertEquals(CourseDraftPreview(title = "binario", moduleCount = null), preview)
    }
}
