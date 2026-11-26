package es.ulpgc.pamn.dominio.account

// Las iniciales del avatar (Ajustes): la primera letra de la primera palabra y la de la última,
// en mayúsculas. «Mohamed Oharoun» → «MO»; «Ana» → «A»; sin nombre → «?».
// Un nombre con guion («María-José») es una sola palabra.
fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return "?"
    val ends = if (words.size == 1) words else listOf(words.first(), words.last())
    return ends.joinToString("") { it.first().uppercase() }
}

val Account.initials: String get() = initialsOf(name)
