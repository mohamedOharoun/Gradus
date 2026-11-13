# Tarea — Registro de usuario (backend)

Rama: `develop-backend-gradus`. Proyecto: `backend/` (Maven, Java 25, Javalin 7, PostgreSQL en Docker).

## Objetivo

Exponer un endpoint que dé de alta un usuario con nombre, correo y contraseña, guardándolo en
PostgreSQL con la contraseña hasheada (PBKDF2 con sal aleatoria por usuario).

## Alcance

- `POST /api/users` con `{name, email, password}`.
  - `201` → `{id, name, email, createdAt}` (nunca se devuelve la contraseña ni su hash).
  - `400` → `{errors: [{field, message}]}` si los datos no son válidos.
  - `409` → el correo ya está registrado.
- Validación (especificación §5): nombre ≥ 2 caracteres (máx. 100); correo con formato válido
  (se guarda en minúsculas, máx. 254); contraseña ≥ 8 caracteres con letra, número y carácter especial.
- Campos guardados: `id` (UUID), `name`, `email`, `password_hash`, `created_at`.
- Diseño inmutable (records, clases `final`) y funcional (`Result` en lugar de excepciones).
- Capas: `domain` → `application` → `infrastructure` / `web`; `App` como composition root.

## Fuera de alcance

- Inicio de sesión, tokens o sesiones.
- Verificación del correo, recuperación de contraseña, borrado de cuenta.
- Migraciones con Flyway (de momento basta el script de `db/init/`).
- Integración con la app Android.

## Tareas

- [x] **1. Infraestructura del proyecto.** `pom.xml` con dependencias, `docker-compose.yml`,
  `db/init/001_create_users.sql`, `.env.example`, `.env` en `.gitignore`.
  *Acepta si:* `mvn -q compile` termina sin errores y `docker compose up -d` deja el contenedor
  `healthy` con la tabla `users` creada.
- [x] **2. Dominio.** `UserName`, `Email`, `RawPassword`, `PasswordHash`, `User` y, en
  `domain/validation`, `Result`, `ValidationError`, `Rule` y `Validation`, con tests primero.
  *Acepta si:* los tests cubren casos válidos e inválidos de cada regla y pasan.
- [x] **3. Caso de uso.** `RegisterUser`, `RegisterUserCommand`, `RegistrationResult`, puertos
  `UserRepository` y `PasswordHasher`, y `Result.combine`; tests con repositorio en memoria.
  *Acepta si:* registra un usuario válido, acumula todos los errores de validación y rechaza
  un correo repetido.
- [x] **4. Infraestructura.** `Pbkdf2PasswordHasher` + `Pbkdf2Hash` (con test) y
  `JdbcUserRepository` (`INSERT ... ON CONFLICT (email) DO NOTHING`). Sin test automático del
  repositorio hasta decidir CI/CD: se comprueba a mano en el paso 5.
  *Acepta si:* dos hashes de la misma contraseña son distintos (sal) y ambos se verifican.
- [x] **5. Web.** `UserController`, DTOs, `ExceptionHandlers` (JSON inválido → 400), `DatabaseConfig`
  y `HttpConfig` (variables de entorno, con test) y `App`. Se arranca con `mvn compile exec:java`.
  *Acepta si:* con `curl` se obtienen 201, 400 y 409 en los casos correspondientes y el
  usuario aparece en la tabla.

## Progreso

- 2026-11-13: plan aprobado. Paso 1 hecho.
- 2026-11-13: paso 2 hecho (48 tests en verde, sin avisos de compilación).
- 2026-11-13: paso 3 hecho (57 tests en verde, sin avisos de compilación).
- 2026-11-13: paso 4 hecho (71 tests en verde; SQL del repositorio probado a mano en PostgreSQL 18).
- 2026-11-13: paso 5 hecho (77 tests en verde). Probado con `curl`: 201, 409, 400 por validación,
  400 por campos ausentes, 400 por JSON roto o con campos desconocidos; usuario guardado con hash PBKDF2.
- **Tarea completada.**

## Pendiente para más adelante

- Test automatizado del repositorio y del endpoint (cuando se decida el CI/CD).
- Pool de conexiones (HikariCP): ahora se abre una conexión por petición.
- Errores 500 en formato JSON (ahora Javalin responde texto plano «Server Error»).
- Actualizar `especificacion.md` §8: el backend es Javalin, no Ktor.

## Siguiente paso

Por decidir: inicio de sesión (`POST /api/sessions`) o integrar el registro en la app Android.
