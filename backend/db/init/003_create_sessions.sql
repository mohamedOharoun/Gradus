-- Sesiones iniciadas: una por dispositivo. Ver backend/docs/tareas/inicio-sesion.md.
-- Se puede ejecutar varias veces sin efecto (IF NOT EXISTS), también en volúmenes ya creados.
CREATE TABLE IF NOT EXISTS sessions (
    id           UUID        PRIMARY KEY,
    -- Al borrar un usuario se borran sus sesiones.
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- SHA-256 del token en hexadecimal: el token en sí nunca se guarda. UNIQUE crea su índice.
    token_hash   VARCHAR(64) NOT NULL UNIQUE,
    created_at   TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT sessions_token_hash_is_sha256 CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT sessions_used_after_created CHECK (last_used_at >= created_at),
    CONSTRAINT sessions_expire_after_use CHECK (expires_at > last_used_at)
);

-- Para borrar en cascada sin recorrer toda la tabla y, más adelante, cerrar todas las sesiones de un usuario.
CREATE INDEX IF NOT EXISTS sessions_user_id_idx ON sessions (user_id);
