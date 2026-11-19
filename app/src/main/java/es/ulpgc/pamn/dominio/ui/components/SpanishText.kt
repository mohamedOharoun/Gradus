package es.ulpgc.pamn.dominio.ui.components

// «A», «A y B», «A, B y C». Para los mensajes que enumeran cosas.
fun List<String>.joinedWithAnd(): String =
    if (size <= 1) joinToString() else dropLast(1).joinToString(", ") + " y " + last()
