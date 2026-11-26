-- Entregas de los ejercicios con foto. Ver docs/tareas/ejercicio-foto.md (paso 3d, decisiones E, H e I).
-- El id, la fecha y el estado los decide la aplicación (Java), no la base de datos.
-- Se puede ejecutar varias veces sin efecto (IF NOT EXISTS).
CREATE TABLE IF NOT EXISTS submissions (
    id             UUID        PRIMARY KEY,
    -- Decisión H: al borrar la cuenta se borran sus entregas (y sus fotos).
    student_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id      UUID        NOT NULL,
    -- Decisión M: la versión que estudiaba el alumno. Debe existir (los cursos no se borran).
    course_version INTEGER     NOT NULL,
    exercise_id    VARCHAR(64) NOT NULL,
    submitted_at   TIMESTAMPTZ NOT NULL,
    -- Fase 1: solo PENDING. La fase 2 (revisión) añadirá los suyos.
    status         VARCHAR(16) NOT NULL,
    -- Decisión E: la foto en la propia tabla (JPEG ya comprobado por la aplicación, ≤ 2 000 000 bytes).
    photo          BYTEA       NOT NULL,
    -- Su tamaño, para listar sin leer la foto entera.
    photo_bytes    INTEGER     NOT NULL,
    FOREIGN KEY (course_id, course_version) REFERENCES course_versions (course_id, version),
    CONSTRAINT submissions_version_positive CHECK (course_version >= 1),
    CONSTRAINT submissions_exercise_not_blank CHECK (length(trim(exercise_id)) > 0),
    CONSTRAINT submissions_status_known CHECK (status IN ('PENDING')),
    CONSTRAINT submissions_photo_size CHECK (photo_bytes BETWEEN 1 AND 2000000),
    CONSTRAINT submissions_photo_bytes_match CHECK (photo_bytes = octet_length(photo))
);

-- «Mis entregas en este curso» (GET /api/submissions/mine?courseId=…) y el borrado en cascada de un alumno.
CREATE INDEX IF NOT EXISTS submissions_student_course_idx ON submissions (student_id, course_id);
