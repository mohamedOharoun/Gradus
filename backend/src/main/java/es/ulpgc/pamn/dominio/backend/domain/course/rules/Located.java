package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;

import java.util.Objects;

/** Un valor del curso junto con su ruta en el documento, para poder informar dónde está. */
record Located<T>(JsonPointer path, T value) {

    Located {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(value, "value");
    }
}
