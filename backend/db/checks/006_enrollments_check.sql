-- Comprobación de la migración 006 (inscripciones y módulos dominados).
-- Ver backend/docs/tareas/progreso-backend.md, tarea 2a.
--
-- Qué es: documentación ejecutable. Prueba las restricciones de la 006 y las dos sentencias de
-- fusión de JdbcEnrollmentRepository (el mismo texto, con $n donde Java pone ?).
-- Cada comprobación es un bloque DO con ASSERT: si algo no se cumple, el guion se para con un error
-- (psql con ON_ERROR_STOP=1). El \echo 'OK: …' de detrás solo sale si el bloque ha pasado.
--
-- Cómo se ejecuta (nunca en la base de desarrollo: el primer bloque lo impide):
--   1. Conectado a la base postgres: CREATE DATABASE gradus_progress_check;
--   2. Aplicar a esa base, en orden, 001–004 y 006 de backend/db/init/.
--   3. docker exec -i gradus-postgres sh -c \
--        'psql -X -q -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d gradus_progress_check -f -' \
--        < backend/db/checks/006_enrollments_check.sql
--   4. Conectado a la base postgres: DROP DATABASE gradus_progress_check;
-- Todo va dentro de BEGIN … ROLLBACK: no deja datos y se puede repetir.
-- La prueba de dos sesiones a la vez no cabe en un solo psql: su receta está al final.
--
-- Datos de prueba (UUID fáciles de leer):
--   autor a0a0a0a0-…, alumno A aaaaaaaa-…, alumno B bbbbbbbb-…,
--   cursos C1 c1c1c1c1-… y C2 c2c2c2c2-… (del autor), curso inexistente c9c9c9c9-….

DO $$
BEGIN
    ASSERT current_database() = 'gradus_progress_check',
        'Este guion solo se ejecuta en gradus_progress_check, no en ' || current_database();
END $$;
\echo 'OK: base gradus_progress_check'

BEGIN;

INSERT INTO users (id, name, email, password_hash, created_at, terms_accepted_at) VALUES
    ('a0a0a0a0-0000-0000-0000-000000000000', 'Autor',    'autor@check.test', 'x', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000000', 'Alumno A', 'a@check.test',     'x', now(), now()),
    ('bbbbbbbb-0000-0000-0000-000000000000', 'Alumno B', 'b@check.test',     'x', now(), now());
INSERT INTO courses (id, author_id, created_at, latest_version) VALUES
    ('c1c1c1c1-0000-0000-0000-000000000000', 'a0a0a0a0-0000-0000-0000-000000000000', now(), 1),
    ('c2c2c2c2-0000-0000-0000-000000000000', 'a0a0a0a0-0000-0000-0000-000000000000', now(), 1);

-- Las dos sentencias de fusión: unión, gana la fecha más antigua, nunca se borra nada.
PREPARE merge_enrollment (uuid, uuid, timestamptz) AS
INSERT INTO enrollments (user_id, course_id, enrolled_at)
VALUES ($1, $2, $3)
ON CONFLICT (user_id, course_id)
DO UPDATE SET enrolled_at = LEAST(enrollments.enrolled_at, EXCLUDED.enrolled_at);

PREPARE merge_mastered_module (uuid, uuid, text, timestamptz) AS
INSERT INTO mastered_modules (user_id, course_id, module_id, mastered_at)
VALUES ($1, $2, $3, $4)
ON CONFLICT (user_id, course_id, module_id)
DO UPDATE SET mastered_at = LEAST(mastered_modules.mastered_at, EXCLUDED.mastered_at);

-- Ayudas de lectura: la fecha guardada de una fila (NULL si no existe). Se deshacen con el ROLLBACK.
CREATE FUNCTION pg_temp.enrolled_at(u uuid, c uuid) RETURNS timestamptz LANGUAGE sql AS
    $$ SELECT enrolled_at FROM enrollments WHERE user_id = u AND course_id = c $$;
CREATE FUNCTION pg_temp.mastered_at(u uuid, c uuid, m text) RETURNS timestamptz LANGUAGE sql AS
    $$ SELECT mastered_at FROM mastered_modules WHERE user_id = u AND course_id = c AND module_id = m $$;

-- 1. Espera: borrar un usuario borra sus inscripciones (c = cascada); borrar una inscripción
--    borra sus módulos; la clave a courses no tiene cascada (a = sin acción).
DO $$
BEGIN
    ASSERT (SELECT confdeltype FROM pg_constraint
            WHERE conrelid = 'enrollments'::regclass AND conname = 'enrollments_user_id_fkey') = 'c',
        'enrollments → users debe borrar en cascada';
    ASSERT (SELECT confdeltype FROM pg_constraint
            WHERE conrelid = 'enrollments'::regclass AND conname = 'enrollments_course_id_fkey') = 'a',
        'enrollments → courses no debe tener cascada';
    ASSERT (SELECT confdeltype FROM pg_constraint
            WHERE conrelid = 'mastered_modules'::regclass AND conname = 'mastered_modules_enrollment_fk') = 'c',
        'mastered_modules → enrollments debe borrar en cascada';
    ASSERT NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conrelid = 'mastered_modules'::regclass AND contype = 'f'
                         AND confrelid = 'users'::regclass),
        'mastered_modules no debe tener clave ajena directa a users';
END $$;
\echo 'OK 1: claves ajenas y su borrado'

-- 2. Espera: una inscripción nueva crea una fila con la fecha enviada.
EXECUTE merge_enrollment('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000',
                         '2026-02-01 00:00:00+00');
DO $$
BEGIN
    ASSERT (SELECT count(*) FROM enrollments) = 1, 'Debe haber 1 inscripción';
    ASSERT pg_temp.enrolled_at('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000')
           = '2026-02-01 00:00:00+00', 'La inscripción debe tener la fecha enviada';
END $$;
\echo 'OK 2: inscripción nueva'

-- 3. Espera: repetir la misma inscripción no cambia nada (ni filas ni fecha).
EXECUTE merge_enrollment('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000',
                         '2026-02-01 00:00:00+00');
DO $$
BEGIN
    ASSERT (SELECT count(*) FROM enrollments) = 1, 'Repetir no debe crear otra fila';
    ASSERT pg_temp.enrolled_at('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000')
           = '2026-02-01 00:00:00+00', 'Repetir no debe cambiar la fecha';
END $$;
\echo 'OK 3: inscripción repetida sin cambios'

-- 4. Espera: una fecha más moderna no sobrescribe la guardada.
EXECUTE merge_enrollment('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000',
                         '2026-05-01 00:00:00+00');
DO $$
BEGIN
    ASSERT pg_temp.enrolled_at('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000')
           = '2026-02-01 00:00:00+00', 'Una fecha más moderna no debe sobrescribir';
END $$;
\echo 'OK 4: inscripción, la fecha moderna no sobrescribe'

-- 5. Espera: una fecha más antigua gana.
EXECUTE merge_enrollment('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000',
                         '2026-01-01 00:00:00+00');
DO $$
BEGIN
    ASSERT (SELECT count(*) FROM enrollments) = 1, 'Fusionar no debe crear otra fila';
    ASSERT pg_temp.enrolled_at('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000')
           = '2026-01-01 00:00:00+00', 'Debe ganar la fecha más antigua';
END $$;
\echo 'OK 5: inscripción, gana la fecha más antigua'

-- 6. Espera: un módulo nuevo crea una fila con la fecha enviada.
EXECUTE merge_mastered_module('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000',
                              'm1', '2026-03-01 00:00:00+00');
DO $$
BEGIN
    ASSERT (SELECT count(*) FROM mastered_modules) = 1, 'Debe haber 1 módulo';
    ASSERT pg_temp.mastered_at('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000', 'm1')
           = '2026-03-01 00:00:00+00', 'El módulo debe tener la fecha enviada';
END $$;
\echo 'OK 6: módulo nuevo'

-- 7. Espera: repetir el mismo módulo no cambia nada.
EXECUTE merge_mastered_module('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000',
                              'm1', '2026-03-01 00:00:00+00');
DO $$
BEGIN
    ASSERT (SELECT count(*) FROM mastered_modules) = 1, 'Repetir no debe crear otra fila';
    ASSERT pg_temp.mastered_at('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000', 'm1')
           = '2026-03-01 00:00:00+00', 'Repetir no debe cambiar la fecha';
END $$;
\echo 'OK 7: módulo repetido sin cambios'

-- 8. Espera: una fecha más moderna no sobrescribe la del módulo.
EXECUTE merge_mastered_module('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000',
                              'm1', '2026-06-01 00:00:00+00');
DO $$
BEGIN
    ASSERT pg_temp.mastered_at('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000', 'm1')
           = '2026-03-01 00:00:00+00', 'Una fecha más moderna no debe sobrescribir';
END $$;
\echo 'OK 8: módulo, la fecha moderna no sobrescribe'

-- 9. Espera: una fecha más antigua gana en el módulo.
EXECUTE merge_mastered_module('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000',
                              'm1', '2026-02-15 00:00:00+00');
DO $$
BEGIN
    ASSERT (SELECT count(*) FROM mastered_modules) = 1, 'Fusionar no debe crear otra fila';
    ASSERT pg_temp.mastered_at('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000', 'm1')
           = '2026-02-15 00:00:00+00', 'Debe ganar la fecha más antigua';
END $$;
\echo 'OK 9: módulo, gana la fecha más antigua'

-- En los bloques de rechazo se captura solo el error esperado y se lee el nombre de la restricción.
-- Si la sentencia no falla, el nombre queda NULL y el ASSERT falla; si falla por otra causa, el
-- error no se captura y el guion se para.

-- 10. Espera: un módulo de un curso en el que A no está inscrito (C2, sin nadie inscrito) se rechaza.
DO $$
DECLARE
    violated TEXT;
BEGIN
    BEGIN
        INSERT INTO mastered_modules (user_id, course_id, module_id, mastered_at)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000000', 'c2c2c2c2-0000-0000-0000-000000000000',
                'm1', '2026-03-01 00:00:00+00');
    EXCEPTION WHEN foreign_key_violation THEN
        GET STACKED DIAGNOSTICS violated = CONSTRAINT_NAME;
    END;
    ASSERT violated = 'mastered_modules_enrollment_fk',
        'Se esperaba mastered_modules_enrollment_fk; se obtuvo ' || coalesce(violated, 'ningún error');
END $$;
\echo 'OK 10: módulo sin inscripción rechazado'

-- 11. Espera: con B inscrito en C2, un módulo de A en C2 se sigue rechazando: la clave mira el par
--     (usuario, curso), no solo el curso.
EXECUTE merge_enrollment('bbbbbbbb-0000-0000-0000-000000000000', 'c2c2c2c2-0000-0000-0000-000000000000',
                         '2026-01-10 00:00:00+00');
EXECUTE merge_mastered_module('bbbbbbbb-0000-0000-0000-000000000000', 'c2c2c2c2-0000-0000-0000-000000000000',
                              'm1', '2026-01-20 00:00:00+00');
DO $$
DECLARE
    violated TEXT;
BEGIN
    BEGIN
        INSERT INTO mastered_modules (user_id, course_id, module_id, mastered_at)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000000', 'c2c2c2c2-0000-0000-0000-000000000000',
                'm1', '2026-03-01 00:00:00+00');
    EXCEPTION WHEN foreign_key_violation THEN
        GET STACKED DIAGNOSTICS violated = CONSTRAINT_NAME;
    END;
    ASSERT violated = 'mastered_modules_enrollment_fk',
        'Se esperaba mastered_modules_enrollment_fk; se obtuvo ' || coalesce(violated, 'ningún error');
END $$;
\echo 'OK 11: módulo con inscripción de otro usuario rechazado'

-- 12. Espera: una inscripción a un curso que no existe se rechaza.
DO $$
DECLARE
    violated TEXT;
BEGIN
    BEGIN
        INSERT INTO enrollments (user_id, course_id, enrolled_at)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000000', 'c9c9c9c9-0000-0000-0000-000000000000',
                '2026-01-01 00:00:00+00');
    EXCEPTION WHEN foreign_key_violation THEN
        GET STACKED DIAGNOSTICS violated = CONSTRAINT_NAME;
    END;
    ASSERT violated = 'enrollments_course_id_fkey',
        'Se esperaba enrollments_course_id_fkey; se obtuvo ' || coalesce(violated, 'ningún error');
END $$;
\echo 'OK 12: inscripción a un curso inexistente rechazada'

-- 13. Espera: un module_id vacío se rechaza.
DO $$
DECLARE
    violated TEXT;
BEGIN
    BEGIN
        INSERT INTO mastered_modules (user_id, course_id, module_id, mastered_at)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000',
                '', '2026-03-01 00:00:00+00');
    EXCEPTION WHEN check_violation THEN
        GET STACKED DIAGNOSTICS violated = CONSTRAINT_NAME;
    END;
    ASSERT violated = 'mastered_modules_module_id_not_empty',
        'Se esperaba mastered_modules_module_id_not_empty; se obtuvo ' || coalesce(violated, 'ningún error');
END $$;
\echo 'OK 13: module_id vacío rechazado'

-- 13b. Espera: un module_id con solo espacios también se rechaza.
DO $$
DECLARE
    violated TEXT;
BEGIN
    BEGIN
        INSERT INTO mastered_modules (user_id, course_id, module_id, mastered_at)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000',
                '   ', '2026-03-01 00:00:00+00');
    EXCEPTION WHEN check_violation THEN
        GET STACKED DIAGNOSTICS violated = CONSTRAINT_NAME;
    END;
    ASSERT violated = 'mastered_modules_module_id_not_empty',
        'Se esperaba mastered_modules_module_id_not_empty; se obtuvo ' || coalesce(violated, 'ningún error');
END $$;
\echo 'OK 13b: module_id con solo espacios rechazado'

-- 14. Espera: borrar a A borra sus inscripciones y sus módulos; lo de B sigue igual.
EXECUTE merge_mastered_module('aaaaaaaa-0000-0000-0000-000000000000', 'c1c1c1c1-0000-0000-0000-000000000000',
                              'm2', '2026-03-05 00:00:00+00');
DELETE FROM users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000000';
DO $$
BEGIN
    ASSERT (SELECT count(*) FROM enrollments WHERE user_id = 'aaaaaaaa-0000-0000-0000-000000000000') = 0,
        'Las inscripciones de A deben borrarse';
    ASSERT (SELECT count(*) FROM mastered_modules WHERE user_id = 'aaaaaaaa-0000-0000-0000-000000000000') = 0,
        'Los módulos de A deben borrarse';
    ASSERT (SELECT count(*) FROM enrollments WHERE user_id = 'bbbbbbbb-0000-0000-0000-000000000000') = 1,
        'La inscripción de B debe seguir';
    ASSERT (SELECT count(*) FROM mastered_modules WHERE user_id = 'bbbbbbbb-0000-0000-0000-000000000000') = 1,
        'El módulo de B debe seguir';
END $$;
\echo 'OK 14: borrar un usuario borra sus inscripciones y sus módulos'

ROLLBACK;
\echo 'Todas las comprobaciones han pasado.'

-- Dos sesiones a la vez (se hace desde bash, con varios psql: no cabe en este guion).
-- Comprueba que, si dos móviles sincronizan la misma fila a la vez, la segunda sesión espera a que
-- la primera confirme y que al final queda la fecha más antigua, en la inscripción y en el módulo.
--   0. Preparación, confirmada (fuera de BEGIN … ROLLBACK): un usuario D (dddddddd-…) que es autor
--      de un curso E (eeeeeeee-…). La fila D–E todavía no existe en ninguna tabla.
--   1. s1 (PGAPPNAME=s1), en segundo plano, como mergeAll:
--        BEGIN;
--        fusión de inscripción (D, E, 2026-03-01);
--        fusión de módulo (D, E, 'm1', 2026-03-01);
--        SELECT pg_sleep(8);   -- retiene los bloqueos de fila sin confirmar
--        COMMIT;
--   2. Esperar (máximo 5 s) a que pg_stat_activity muestre a s1 con wait_event = 'PgSleep':
--      ya ha escrito y aún no ha confirmado. Si no llega, la prueba falla.
--   3. s2 (PGAPPNAME=s2), en segundo plano: BEGIN; las mismas dos fusiones con 2026-01-01; COMMIT;
--   4. Esperar (máximo 5 s) a que s2 tenga wait_event_type = 'Lock' y comprobarlo con un DO/ASSERT
--      que filtra por application_name = 's2'.
--   5. wait: s1 y s2 terminan sin error (s2, después del COMMIT de s1).
--   6. DO/ASSERT: la inscripción D–E y el módulo m1 tienen los dos la fecha 2026-01-01.
