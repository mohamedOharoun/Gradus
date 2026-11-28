package es.ulpgc.pamn.dominio.home

// «Hola, Ana» en Inicio (docs/tareas/inicio.md, paso 3): el primer nombre, separado por espacios (un nombre
// con guion es una sola palabra, como en initialsOf). Sin nombre, null: la pantalla dice solo «Hola».
fun greetingName(name: String): String? = name.trim().split(Regex("""\s+""")).first().ifEmpty { null }
