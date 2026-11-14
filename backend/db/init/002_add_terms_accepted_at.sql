-- Momento en que el usuario aceptó los términos (obligatorio para registrarse).
-- Se puede ejecutar más de una vez sin efecto: sirve tanto para una base de datos nueva
-- como para aplicarlo a mano sobre una que ya tenía la tabla users.
ALTER TABLE users ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMPTZ;

-- Usuarios de prueba creados antes de que existiera la columna: se les pone su fecha de alta.
UPDATE users SET terms_accepted_at = created_at WHERE terms_accepted_at IS NULL;

ALTER TABLE users ALTER COLUMN terms_accepted_at SET NOT NULL;
