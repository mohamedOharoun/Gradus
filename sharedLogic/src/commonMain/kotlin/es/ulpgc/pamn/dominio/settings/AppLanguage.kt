package es.ulpgc.pamn.dominio.settings

// El idioma de la app que se elige en Ajustes (docs/tareas/ajustes.md, fase 3, decisiones O y S). `tag`:
// la etiqueta de idioma que entiende Android («es», «en»); el del sistema no tiene ninguna. Solo cambia
// lo general de la app: los cursos se quedan en el idioma en que los escribió su autor (decisión R).
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    SPANISH("es"),
    ENGLISH("en"),
    ;

    companion object {
        // Lo que guarda Android («en-US,es-ES», o vacío): cuenta el primero, por su idioma y no por su país.
        // Uno que la app no tiene (elegido en los ajustes del sistema) es como el del sistema.
        fun fromTags(tags: String): AppLanguage {
            val first = tags.split(',').first().trim().substringBefore('-').lowercase()
            return entries.firstOrNull { it.tag != null && it.tag == first } ?: SYSTEM
        }
    }
}
