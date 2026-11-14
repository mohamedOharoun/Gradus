# Registro con cuentas reales (app ↔ backend)

Anillo 2 de la especificación. Continúa `registro-login.md` (solo interfaz) y
`backend/docs/tareas/registro-usuario.md` (endpoint `POST /api/users`).

## Objetivo

Que «Desliza para registrarte» cree una cuenta de verdad en el backend, y que la app y el servidor
apliquen las mismas reglas y se entiendan en los errores.

## Alcance

- Los términos aceptados llegan al servidor y queda constancia de cuándo se aceptaron.
- Cada error del servidor lleva un código que la app puede traducir a su propio texto.
- Mismas reglas en los dos lados (longitudes máximas y formato de correo).
- La app llama a `POST /api/users` y muestra el envío, los errores del servidor y los de red.

## Fuera de alcance

- Inicio de sesión real, tokens y recordar la sesión.
- Enviar la repetición de la contraseña: es una comprobación de la interfaz contra erratas.
- Verificación del correo y recuperación de la contraseña.

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
- [ ] **4. App: llamada real.** Permiso de internet, cliente HTTP, estados de envío y de error en
  `RegisterViewModel`. Se dividirá en pasos al empezarla (necesita aprobar una dependencia y tocar
  el manifiesto). Coordinar con T10 y T11 de `registro-login.md`.
  *Acepta si:* desde el emulador se crea una cuenta que aparece en la tabla `users`; un correo
  repetido se muestra en el paso del correo; sin red se avisa y se puede reintentar.

## Códigos de error

Cada error de las respuestas 400 y 409 tiene esta forma: `{"field": "…", "code": "…", "message": "…"}`.
La app decide el texto a partir de `field` y `code`; `message` es solo una ayuda para quien prueba la API.
Los códigos no se renombran ni se borran (lo vigila `ErrorCodeTest`); sí se pueden añadir.

| `code` | Campos (`field`) | Equivalente hoy en la app |
|---|---|---|
| `REQUIRED` | `name`, `email`, `password` | `FieldError.EMPTY` |
| `TOO_SHORT` | `name`, `password` | `FieldError.TOO_SHORT` / `PasswordRequirement.MIN_LENGTH` |
| `TOO_LONG` | `name`, `email` | `FieldError.TOO_LONG` |
| `INVALID_EMAIL` | `email` | `FieldError.INVALID_EMAIL` |
| `MISSING_LETTER`, `MISSING_DIGIT` | `password` | `PasswordRequirement.LETTER_AND_DIGIT` |
| `MISSING_SPECIAL_CHARACTER` | `password` | `PasswordRequirement.SPECIAL_CHARACTER` |
| `TERMS_NOT_ACCEPTED` | `acceptedTerms` | No existe todavía (tarea 4) |
| `EMAIL_TAKEN` (409) | `email` | No existe todavía (tarea 4) |
| `MALFORMED_BODY` | `body` | No debería verse: indica un fallo de la app, no del usuario |

## Progreso

- 2026-11-14: tarea 1 hecha. 83 tests del backend en verde. Probado con `curl` contra PostgreSQL 18:
  201 con términos; 400 sin ellos o con `false`; migración `002` probada sobre datos existentes
  (dos veces, es repetible) y sobre una base de datos nueva. Peticiones de Yaak actualizadas.
- 2026-11-14: tarea 2 hecha. 85 tests del backend en verde. Probado con `curl`: los 10 códigos
  aparecen en respuestas reales (400 y 409).
- 2026-11-14: la lógica de la app pasa a `sharedLogic` (Kotlin Multiplatform). Tarea 3 hecha:
  93 tests del backend y 38 de `ValidationTest` (antes 26) en verde, con los mismos casos de correo
  en `EmailTest` y `ValidationTest`.

## Siguiente paso

Tarea 4: la app llama de verdad a `POST /api/users`. Antes de empezar, dividirla en pasos.
