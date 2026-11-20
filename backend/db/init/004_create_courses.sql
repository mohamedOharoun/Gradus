-- Cursos publicados y sus versiones. Ver backend/docs/tareas/publicacion-cursos.md.
-- El id, las fechas y el número de versión los genera la aplicación (Java), no la base de datos.
-- Se puede ejecutar varias veces sin efecto (IF NOT EXISTS).
CREATE TABLE IF NOT EXISTS courses (
    id             UUID        PRIMARY KEY,
    -- Sin ON DELETE CASCADE: borrar a un autor no debe borrar cursos que otros estudian.
    author_id      UUID        NOT NULL REFERENCES users (id),
    created_at     TIMESTAMPTZ NOT NULL,
    latest_version INTEGER     NOT NULL,
    CONSTRAINT courses_latest_version_positive CHECK (latest_version >= 1)
);

-- Para comprobar la clave ajena al borrar un usuario y, más adelante, listar los cursos de un autor.
CREATE INDEX IF NOT EXISTS courses_author_id_idx ON courses (author_id);

-- Cada versión es inmutable. Los metadatos se derivan del JSON al publicar; el JSON es la fuente de verdad.
CREATE TABLE IF NOT EXISTS course_versions (
    course_id      UUID         NOT NULL REFERENCES courses (id),
    version        INTEGER      NOT NULL,
    published_at   TIMESTAMPTZ  NOT NULL,
    format_version INTEGER      NOT NULL,
    title          VARCHAR(120) NOT NULL,
    description    VARCHAR(500) NOT NULL,
    language       VARCHAR(5)   NOT NULL,
    -- Etiquetas tal como las escribió el autor, y normalizadas para buscar (ver §3.4 de la especificación).
    tags           TEXT[]       NOT NULL,
    search_tags    TEXT[]       NOT NULL,
    -- Título y descripción normalizados, para buscar con LIKE.
    search_text    TEXT         NOT NULL,
    module_count   INTEGER      NOT NULL,
    -- SHA-256 en hexadecimal de los bytes recibidos.
    sha256         VARCHAR(64)  NOT NULL,
    size_bytes     INTEGER      NOT NULL,
    -- El JSON recibido, sin modificar. TEXT y no JSONB: JSONB reordena claves y quita espacios.
    content        TEXT         NOT NULL,
    PRIMARY KEY (course_id, version),
    CONSTRAINT course_versions_version_positive CHECK (version >= 1),
    CONSTRAINT course_versions_format_version_positive CHECK (format_version >= 1),
    CONSTRAINT course_versions_language_format CHECK (language ~ '^[a-z]{2}(-[A-Z]{2})?$'),
    CONSTRAINT course_versions_tag_count CHECK (cardinality(tags) BETWEEN 1 AND 10
                                                AND cardinality(search_tags) = cardinality(tags)),
    CONSTRAINT course_versions_module_count CHECK (module_count BETWEEN 1 AND 100),
    CONSTRAINT course_versions_sha256_is_hex CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT course_versions_size CHECK (size_bytes BETWEEN 1 AND 2097152)
);

-- Búsqueda por etiquetas con search_tags @> ARRAY[...] («contiene todas»).
CREATE INDEX IF NOT EXISTS course_versions_search_tags_idx ON course_versions USING GIN (search_tags);
