# Cuentas reales (app ↔ backend)

Anillo 2 de la especificación. Continúa `registro-login.md` (solo interfaz),
`backend/docs/tareas/registro-usuario.md` (`POST /api/users`) y
`backend/docs/tareas/inicio-sesion.md` (`POST /api/sessions`, `GET /api/users/me`,
`DELETE /api/sessions/current`). El plan por pasos de la parte de la app está en
`conexion-registro-app-backend.md`.

## Objetivo

Que la app use cuentas de verdad: «Crear cuenta» (después, «Desliza para registrarte») crea la
cuenta en el backend, «Entrar» inicia sesión con ella, y la app y el servidor aplican las mismas
reglas y se entienden en los errores.

## Alcance

- Los términos aceptados llegan al servidor y queda constancia de cuándo se aceptaron.
- Cada error del servidor lleva un código que la app puede traducir a su propio texto.
- Mismas reglas en los dos lados (longitudes máximas y formato de correo).
- La app llama a `POST /api/users` y muestra el envío, los errores del servidor y los de red.
- La app inicia sesión con `POST /api/sessions` y guarda el token para las siguientes peticiones.
- Al abrir la app con un token guardado, se comprueba con `GET /api/users/me` y se entra directo.
- Cerrar sesión con `DELETE /api/sessions/current`.

## Fuera de alcance

- Enviar la repetición de la contraseña: es una comprobación de la interfaz contra erratas.
- Verificación del correo, recuperación de la contraseña y segundo factor.
- Servidor desplegado y HTTPS: de momento, backend local y emulador (`http://localhost:7070` con `adb reverse`).
- gRPC (el camino queda preparado en `conexion-registro-app-backend.md`).

## Contrato de la API

Base en desarrollo: `http://localhost:7070`, tanto desde el ordenador (Yaak) como desde el emulador
o un móvil por USB, gracias a `adb reverse tcp:7070 tcp:7070` (lo hacen `gradus.sh`/`gradus.ps1`).
No se usa `10.0.2.2`: con `targetSdk` 37 la red local exige el permiso `ACCESS_LOCAL_NETWORK`.

| Método y ruta | Cuerpo | Respuestas |
|---|---|---|
| `POST /api/users` | `{name, email, password, acceptedTerms}` | `201` usuario · `400` errores · `409` `EMAIL_TAKEN` |
| `POST /api/sessions` | `{email, password}` | `201` `{token, expiresAt, user}` · `400` · `401` `INVALID_CREDENTIALS` |
| `GET /api/users/me` | — (cabecera `Authorization: Bearer <token>`) | `200` usuario · `401` `UNAUTHENTICATED` |
| `DELETE /api/sessions/current` | — (cabecera `Authorization: Bearer <token>`) | `204` · `401` `UNAUTHENTICATED` |

Usuario: `{id, name, email, createdAt}` (fechas en ISO‑8601). Nunca se devuelve la contraseña.

## Tareas

- [x] **1. Backend: términos.** `acceptedTerms` en la petición (obligatorio y `true`), objeto de
  valor `TermsAcceptance` y columna `terms_accepted_at`.
  *Acepta si:* sin términos o con `false` responde 400 en el campo `acceptedTerms`; con `true`
  crea la cuenta y guarda la fecha; el script `002` funciona en una base de datos nueva y en una
  que ya tenía usuarios.
- [x] **2. Backend: códigos de error.** Cada error lleva `code` (enum `ErrorCode`) además del
  mensaje. La lista está en «Códigos de error», más abajo.
  *Acepta si:* todos los errores de 400 y 409 tienen un código estable y documentado.
- [x] **3. Alinear reglas.** Máximo de nombre (100) y de correo (254) en la app, un único formato
  de correo en los dos lados, campo vacío = `REQUIRED` en el backend y longitudes en caracteres
  reales en la app (un emoji cuenta 1).
  *Acepta si:* una misma lista de casos da el mismo resultado en los tests de la app y del backend.
- [x] **4. App: registro real.** Permiso de internet, cliente HTTP, estados de envío y de error en
  `RegisterViewModel`. Plan por pasos (1 a 9) en `conexion-registro-app-backend.md`. Coordinar
  con T10 y T11 de `registro-login.md`.
  *Acepta si:* desde el emulador se crea una cuenta que aparece en la tabla `users`; un correo
  repetido se muestra en el paso del correo; sin red se avisa y se puede reintentar.
- [ ] **5. App: inicio de sesión real.** El puerto `AccountGateway` gana `logIn(email, password)`
  con su resultado sellado (`LoggedIn(session)`, `Rejected`, `InvalidCredentials`, `Unreachable`,
  `ServerFailure`); el adaptador HTTP traduce 201/400/401. `LoginViewModel` hace la llamada y
  `LoginForm` muestra «Correo o contraseña incorrectos» sin decir cuál de los dos.
  Reutiliza el cliente HTTP, el `AppContainer` y la red de Android de la tarea 4.
  *Acepta si:* con una cuenta creada en la tarea 4 se entra a Inicio; con contraseña errónea o
  correo inexistente se ve el mismo aviso; sin red se avisa y se puede reintentar.
- [ ] **6. App: recordar la sesión.** Puerto `SessionStore` (guardar, leer y borrar el token) en
  `sharedLogic`; adaptador Android que lo guarda cifrado. Al arrancar: si hay token,
  `GET /api/users/me` → Inicio; si responde 401, se borra y se va a Bienvenida. Cerrar sesión
  llama a `DELETE /api/sessions/current` y borra el token aunque no haya red.
  Necesita aprobar dependencia (ver «Decisiones pendientes»).
  *Acepta si:* cerrar y reabrir la app mantiene la sesión; tras cerrar sesión, `GET /api/users/me`
  desde Yaak con ese token da 401; el token no aparece en logs.

## Decisiones pendientes

1. ~~**Tras registrarse, ¿se inicia sesión sola?**~~ **Decidido (2026-11-17): no.** Tras el 201 la
   app va a Inicio de sesión y la persona entra con su correo y contraseña.
2. **Dónde guardar el token** (tarea 6). Opciones a comprobar al llegar: DataStore + cifrado con
   Android Keystore, u otra librería multiplataforma. No se elige ni se añade sin aprobarlo.

## Códigos de error

Las respuestas de error tienen la forma `{"errors": [{"field": "…", "code": "…", "message": "…"}]}`.
La app decide el paso y el texto a partir de `field` y `code` (tabla en `sharedLogic/.../account/RegisterProblem.kt`); `message` es solo una ayuda para quien prueba la API.
Los códigos no se renombran ni se borran (lo vigila `ErrorCodeTest`); sí se pueden añadir.

| `code` | Campos (`field`) | Equivalente hoy en la app |
|---|---|---|
| `REQUIRED` | `name`, `email`, `password` | `FieldError.EMPTY` |
| `TOO_SHORT` | `name`, `password` | `FieldError.TOO_SHORT` / `PasswordRequirement.MIN_LENGTH` |
| `TOO_LONG` | `name`, `email` | `FieldError.TOO_LONG` |
| `INVALID_EMAIL` | `email` | `FieldError.INVALID_EMAIL` |
| `MISSING_LETTER`, `MISSING_DIGIT` | `password` | `PasswordRequirement.LETTER_AND_DIGIT` |
| `MISSING_SPECIAL_CHARACTER` | `password` | `PasswordRequirement.SPECIAL_CHARACTER` |
| `TERMS_NOT_ACCEPTED` | `acceptedTerms` | `FieldError.TERMS_NOT_ACCEPTED` (paso de la contraseña) |
| `EMAIL_TAKEN` (409) | `email` | `FieldError.EMAIL_TAKEN` (paso del correo) |
| `INVALID_CREDENTIALS` (401) | `credentials` | No existe todavía (tarea 5) |
| `UNAUTHENTICATED` (401) | `authorization` | No se muestra: se borra el token y se vuelve a Bienvenida (tarea 6) |
| `MALFORMED_BODY` | `body` | No debería verse: indica un fallo de la app, no del usuario |

## Entorno de desarrollo

Tres piezas en marcha a la vez, cada una en su sitio:

1. **PostgreSQL en Docker** (`backend/docker-compose.yml`). Desde `backend/`: `docker compose up -d`.
   Lee las credenciales de `backend/.env` (copiar de `.env.example`). Publica el puerto 5432.
   Los scripts de `db/init/` solo se ejecutan con el volumen vacío; en un volumen ya creado,
   aplicar a mano los que falten (p. ej. `003_create_sessions.sql`).
2. **Backend en IntelliJ** (proyecto `backend/`, abierto por su `pom.xml`). Ejecutar `App` con las
   variables `GRADUS_DB_NAME`, `GRADUS_DB_USER`, `GRADUS_DB_PASSWORD` (las mismas del `.env`) en
   la configuración de ejecución, o `mvn compile exec:java` con ellas definidas. Escucha en el 7070.
3. **App en Android Studio** (raíz del repositorio) y emulador. Llama a `http://localhost:7070`;
   hace falta `adb reverse tcp:7070 tcp:7070` cada vez que se abre el emulador (lo hace `gradus.sh`).

Yaak (`backend/yaak/workspace`) prueba la API sin la app, contra `http://localhost:7070`
(variable `baseUrl`). Si una petición funciona en Yaak y falla en la app, el problema está en la app.

## Progreso

- 2026-11-14: tarea 1 hecha. 83 tests del backend en verde. Probado con `curl` contra PostgreSQL 18:
  201 con términos; 400 sin ellos o con `false`; migración `002` probada sobre datos existentes
  (dos veces, es repetible) y sobre una base de datos nueva. Peticiones de Yaak actualizadas.
- 2026-11-14: tarea 2 hecha. 85 tests del backend en verde. Probado con `curl`: los 10 códigos
  aparecen en respuestas reales (400 y 409).
- 2026-11-14: la lógica de la app pasa a `sharedLogic` (Kotlin Multiplatform). Tarea 3 hecha:
  93 tests del backend y 38 de `ValidationTest` (antes 26) en verde, con los mismos casos de correo
  en `EmailTest` y `ValidationTest`.
- 2026-11-17: el backend ya tiene inicio de sesión (`backend/docs/tareas/inicio-sesion.md`). El
  documento amplía el alcance a iniciar sesión y recordarla en la app (tareas 5 y 6), añade el
  contrato de la API, los códigos `INVALID_CREDENTIALS` y `UNAUTHENTICATED` y el entorno de
  desarrollo. Ninguna de las tareas de la app (4, 5, 6) está empezada.

- 2026-11-17: tarea 4 hecha (detalle en `conexion-registro-app-backend.md`). Probada en el
  emulador contra el backend real: cuenta nueva, correo repetido y sin conexión con reintento.
  Tras registrarse se va a Inicio de sesión. La app llega al backend por `localhost` + `adb reverse`.

## Siguiente paso

Tarea 5: inicio de sesión real. Dividirla en pasos como la tarea 4 antes de empezar.
