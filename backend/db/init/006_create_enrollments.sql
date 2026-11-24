-- Inscripciones de los alumnos y módulos que han dominado. Ver backend/docs/tareas/progreso-backend.md.
-- Las fechas las pone el móvil. Al fusionar gana siempre la más antigua (LEAST) y nunca se borra nada.
-- Comprobación: backend/db/checks/006_enrollments_check.sql.
-- Se puede ejecutar varias veces sin efecto (IF NOT EXISTS).
CREATE TABLE IF NOT EXISTS enrollments (
    -- Al borrar un usuario se borran sus inscripciones y, en cascada, sus módulos.
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Sin cascada: los cursos no se borran.
    course_id   UUID        NOT NULL REFERENCES courses (id),
    enrolled_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, course_id)
);

CREATE TABLE IF NOT EXISTS mastered_modules (
    user_id     UUID        NOT NULL,
    course_id   UUID        NOT NULL,
    -- Id del módulo en el JSON del curso. TEXT: el dominio no limita su longitud.
    module_id   TEXT        NOT NULL,
    mastered_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, course_id, module_id),
    -- Cuelga solo de su inscripción (sin clave ajena directa a users):
    -- borrar la inscripción, o al usuario, borra sus módulos.
    CONSTRAINT mastered_modules_enrollment_fk FOREIGN KEY (user_id, course_id)
        REFERENCES enrollments (user_id, course_id) ON DELETE CASCADE,
    -- Red de seguridad, ni vacío ni solo espacios: el dominio ya lo valida.
    CONSTRAINT mastered_modules_module_id_not_empty CHECK (btrim(module_id) <> '')
);

-- Sin índices extra: las claves primarias empiezan por user_id (consultas por usuario y la cascada
-- desde users) y por (user_id, course_id) (la cascada desde enrollments). enrollments.course_id no
-- lleva índice porque los cursos no se borran.
