# Tarea — Inicio de sesión (backend)

Rama: `develop-backend-gradus`. Proyecto: `backend/` (Maven, Java 25, Javalin 7, PostgreSQL en Docker).

## Objetivo

Que la app Android (y la futura de iOS) pueda iniciar sesión con correo y contraseña y seguir
identificada durante días sin volver a escribir la contraseña.

## Decisiones (aprobadas el 2026-11-17)

- **Token opaco guardado en la base de datos** (opción A): 32 bytes aleatorios en Base64 URL.
  La app lo envía en `Authorization: Bearer <token>`. En la tabla solo se guarda su SHA-256.
- **Caducidad:** 30 días deslizantes desde el último uso, con un tope absoluto de 90 días
  desde el inicio de sesión.
- Sin dependencias nuevas.
- **Preparado para un segundo factor (2FA):** el resultado del login es un tipo sellado, así que
  más adelante se le puede añadir un caso `SecondFactorRequired` sin cambiar los demás.

## Alcance

- `POST /api/sessions` con `{email, password}`.
  - `201` → `{token, expiresAt, user: {id, name, email, createdAt}}`.
  - `400` → `REQUIRED` si falta el correo o la contraseña.
  - `401` → `INVALID_CREDENTIALS`, igual si el correo no existe o si la contraseña no coincide.
- `GET /api/users/me` con el token → `200` con el usuario; `401` `UNAUTHENTICATED` si falta,
  no existe o ha caducado. Cada uso renueva la caducidad deslizante.
- `DELETE /api/sessions/current` con el token → `204` (cerrar sesión: se borra la fila).
- En el login no se aplican las reglas de fortaleza de la contraseña (solo «no vacía»): si las
  reglas cambian, los usuarios antiguos deben poder seguir entrando.
- Si el correo no existe se calcula igualmente un hash, para que el tiempo de respuesta no lo delate.

## Fuera de alcance

- Segundo factor (TOTP, passkeys), recuperar contraseña, verificar el correo.
- Límite de intentos (fuerza bruta). PBKDF2 con 600 000 iteraciones ya frena, pero queda pendiente.
- Limpieza periódica de sesiones caducadas (se borran al intentar usarlas).
- Listar o cerrar sesiones de otros dispositivos.
- Flyway: la tabla nueva va en `db/init/003_create_sessions.sql` y en volúmenes ya creados se
  aplica a mano, como el `002`.
- Integración con la app Android.

## Tareas

- [x] **1. Dominio de la sesión** (paquete `domain/session`). `SessionToken` (generación
  aleatoria, `toString` oculto, `hash()`), `SessionTokenHash`, `Session` (inmutable,
  `isActiveAt`) y `SessionPolicy` (30/90 días; factory con `start` y `renew`). Tests primero.
  *Acepta si:* los tests cubren la caducidad deslizante, el tope de 90 días y que el token nunca
  aparece en `toString`.
- [x] **2. Caso de uso `LogIn`.** `LogInCommand`, `LogInResult` (sellado), `LoginPassword`
  (solo «no vacía»), `UserRepository.findByEmail`, puerto `SessionRepository`, cambiar
  `PasswordHasher.matches` para recibir `LoginPassword`, código `INVALID_CREDENTIALS`.
  *Acepta si:* con credenciales correctas devuelve token y sesión; correo inexistente y
  contraseña errónea dan el mismo resultado, y en ambos se ha calculado un hash.
- [x] **3. Casos de uso `Authenticate` y `LogOut`.** Buscar la sesión por el hash del token,
  comprobar la caducidad, renovarla y devolver el usuario; cerrar sesión borra la sesión.
  *Acepta si:* una sesión caducada se rechaza y se borra; un uso renueva la caducidad sin pasar
  del tope.
- [x] **4. Infraestructura.** `003_create_sessions.sql` y `JdbcSessionRepository`
  (`JdbcUserRepository.findByEmail` y `findById` se adelantaron a los pasos 2 y 3 para que compilara).
  *Acepta si:* el SQL (también el de `findByEmail` y `findById`) se prueba a mano en PostgreSQL y
  las sesiones se borran al borrar el usuario.
- [x] **5a. Web: iniciar sesión.** `LogInRequest`, `SessionResponse`, `SessionController.logIn`,
  ruta `POST /api/sessions` en `App` y carpeta de Yaak «Sesiones · Inicio de sesión».
  *Acepta si:* enviando las peticiones de Yaak se obtienen 201, 400 (campos ausentes, correo mal
  formado) y 401 (contraseña incorrecta y correo inexistente, con la misma respuesta).
- [x] **5b. Web: sesión en uso.** Lectura de `Authorization: Bearer`, código `UNAUTHENTICATED`,
  `GET /api/users/me` y `DELETE /api/sessions/current`; peticiones de Yaak que reutilizan el token
  del login.
  *Acepta si:* desde Yaak se obtienen 200, 204 y 401 (sin token, token inventado y tras cerrar sesión).

Las peticiones de Yaak se crean y se envían con la CLI de Yaak (`yaak request create`, `yaak send`)
sobre el workspace sincronizado; `curl` queda solo como alternativa.

## Progreso

- 2026-11-17: plan aprobado (opción A, 30/90 días).
- 2026-11-17: paso 1 hecho (123 tests en verde, 30 nuevos; sin avisos de compilación).
- 2026-11-17: paso 2 hecho (147 tests en verde, 24 nuevos). Añadidos `Result.combine` de dos
  argumentos y `PasswordHasher.decoyHash()` para el hash señuelo.
- 2026-11-17: paso 3 hecho (157 tests en verde, 10 nuevos). `AuthenticationResult` sellado;
  `TestUsers` reúne los usuarios de ejemplo de los tests.
- 2026-11-17: paso 4 hecho (157 tests en verde). SQL probado a mano en PostgreSQL 18 desechable:
  insertar, buscar, renovar, borrar (dos veces), las 5 restricciones, la cascada al borrar el
  usuario y repetir el `003`. `Timestamps` reúne la conversión de fechas de los dos repositorios.
  `003` aplicado a la base de datos de desarrollo de Mohamed; falta aplicarlo en la de Joel.
- 2026-11-17: paso 5a hecho (162 tests en verde, 5 nuevos). Carpeta de Yaak «Sesiones · Inicio de
  sesión» (`fl_tPJCbrSZJ3`) con 5 peticiones, enviadas con `yaak send`: 201 (con
  `Cache-Control: no-store`), 401 idénticos para contraseña incorrecta y correo inexistente
  (≈ 0,37 s ambos, gracias al hash señuelo), 400 por campos ausentes y por correo mal formado.
  En la tabla solo se guarda el hash del token.
- 2026-11-17: paso 5b hecho (175 tests en verde, 13 nuevos). `BearerToken` lee la cabecera,
  `RequireSession` (Decorator) protege las rutas y responde 401 con `WWW-Authenticate: Bearer`.
  `LogOut` recibe ahora la `Session` autenticada. Carpeta de Yaak «Sesiones · Sesión en uso»
  (`fl_asqV36HdJ9`) con 5 peticiones; el token se reutiliza con
  `${[ response.body.path(request='rq_ewvg5v49Jz', path='$.token') ]}`. Resultados: 200, 204 y
  401 (sin token, token inventado y tras cerrar sesión); la tabla queda sin la sesión cerrada.
- **Tarea completada.**

## Pendiente para más adelante

- Límite de intentos de inicio de sesión (fuerza bruta).
- Migraciones automáticas (Flyway u otra opción): ahora `003` se aplica a mano en volúmenes ya creados.
- Cada petición autenticada hace un `UPDATE`; si molestara, renovar solo si el último uso tiene más
  de, por ejemplo, una hora.
- Al leer un usuario se vuelven a validar `UserName` y `Email`: si se endurecen sus reglas, las
  cuentas antiguas no se podrían cargar.
- `yaak send <carpeta>` (CLI 0.7.1) no respeta el orden de la carpeta: para probar «Cerrar sesión»
  y «Mi usuario tras cerrar sesión», enviarlas una a una.
- Test automatizado de los repositorios y de los endpoints (cuando se decida el CI/CD).

## Siguiente paso

Integrar el inicio de sesión en la app (guardar el token de forma segura y enviarlo en cada
petición), coordinado con `docs/tareas/registro-cuentas-reales.md`.
