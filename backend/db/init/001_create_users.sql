-- Usuarios registrados en Gradus.
-- El id y la fecha de alta los genera la aplicación (Java), no la base de datos.
-- IF NOT EXISTS: se puede ejecutar varias veces sin efecto (lo necesita gradus.sh en bases ya creadas).
CREATE TABLE IF NOT EXISTS users (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(100) NOT NULL CHECK (char_length(name) >= 2),
    -- Se guarda normalizado en minúsculas; UNIQUE impide dos cuentas con el mismo correo.
    email         VARCHAR(254) NOT NULL UNIQUE CHECK (email = lower(email)),
    -- Formato autodescriptivo: algoritmo$iteraciones$sal$hash (sal aleatoria por usuario).
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);
