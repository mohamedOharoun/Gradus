package es.ulpgc.pamn.dominio.course

// Los módulos en el orden en que se ven en el camino: de arriba abajo y, en cada fila, de
// izquierda a derecha. Es la lista «Qué vas a aprender» de la ficha del curso: así coincide con
// el dibujo del camino, sin depender del orden en que el autor escribió el JSON.
// Reutiliza pathLayout() (la misma función pura que dibuja el camino) en vez de repetir sus reglas.
fun Course.learningOrder(): List<Module> =
    pathLayout().nodes
        .sortedWith(compareBy<PathNode> { it.row }.thenBy { it.x })
        .map { it.module }
