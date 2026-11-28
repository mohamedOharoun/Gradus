package es.ulpgc.pamn.dominio.course.search

// Cuándo se publicó el curso: los tres botones de «Fecha de publicación» de la hoja de filtros
// (Figma «4 · Buscar con filtros»). `ANY` («Cualquiera») no filtra y es el valor por defecto: así no
// hace falta `null` para decir «sin fecha» (decisión A de la tarea 11).
//
// Las palabras de la API (`month` y `year`) no están aquí: son cosa del adaptador HTTP.
enum class PublishedWithin { ANY, LAST_MONTH, LAST_YEAR }
