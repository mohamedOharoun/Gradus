# Inicio de sesión real en la app

Desarrolla la **tarea 5** de `registro-cuentas-reales.md` («App: inicio de sesión real»). Usa el
endpoint `POST /api/sessions` de `backend/docs/tareas/inicio-sesion.md` y sigue el mismo diseño que
el registro (`conexion-registro-app-backend.md`): puerto `AccountGateway`, adaptador HTTP y resultado
sellado. Al principio era del compañero; ahora la hago yo (2026-11-18).

## Objetivo

Que «Entrar» compruebe el correo y la contraseña en el servidor: con datos correctos se llega a
Inicio (con la barra) y la app guarda el token de la sesión; con datos incorrectos se avisa sin decir
cuál de los dos falla; sin red se avisa y se puede reintentar.

## Contrato (ya existe en el backend)

| Petición | Respuestas |
|---|---|
| `POST /api/sessions` con `{email, password}` | `201` `{token, expiresAt, user: {id, name, email, createdAt}}` · `400` errores de campo (`REQUIRED`, `INVALID_EMAIL`) · `401` `{field: "credentials", code: "INVALID_CREDENTIALS"}` |

El token se envía después en `Authorization: Bearer <token>` (lo usará la tarea 6). Nunca debe
aparecer en logs: los `toString()` lo ocultan, como ya hace el backend.

## Qué se reutiliza y qué es nuevo

| Ya existe (registro) | Nuevo (inicio de sesión) |
|---|---|
| `AccountGateway`, `AccountGateways.http`, `AppContainer` | `AccountGateway.logIn(credentials)` |
| `HttpAccountGateway`, `gradusHttpClient`, mapeo de fallos de red | `LogInRequestDto`, `SessionResponseDto` y el caso 401 |
| `Submission` (`Idle`, `Sending`, `Succeeded`, `Failed`) y `FailureReason` | `LoginForm` con `submission` y error del servidor |
| `ErrorText`, `FailureReason.toMessage()`, `FakeAccountGateway` | `FieldError.INVALID_CREDENTIALS` y su texto |
| `localhost` + `adb reverse`, red de Android | — |

## Alcance

- Contrato `Credentials`, `Session` y `LogInOutcome` en `sharedLogic` (test primero).
- `LoginForm` con estado de envío y error del servidor (test primero).
- Adaptador HTTP de `POST /api/sessions` (test primero con `MockEngine`).
- Guardar la sesión **en memoria** tras entrar, detrás de un puerto `SessionStore`.
- `LoginViewModel` y `ScreenLogin`: «Entrando…», aviso de credenciales incorrectas, sin red con
  «Reintentar» y navegación a Inicio al entrar.

## Fuera de alcance

- Recordar la sesión al cerrar la app y cerrar sesión: tarea 6 (necesita aprobar una dependencia
  para guardar el token cifrado). Gracias al puerto `SessionStore`, será cambiar el adaptador.
- «¿Has olvidado tu contraseña?» (la especificación lo menciona, pero el backend no lo tiene).
- Iniciar sesión sola tras registrarse (decidido el 2026-11-17: no).

## Decisiones (aprobadas el 2026-11-18: las tres propuestas)

1. **Dónde queda la sesión de momento.** Propuesta: puerto `SessionStore` (`save`, `current`,
   `clear`) en `sharedLogic` y un `InMemorySessionStore` en `AppContainer`. Se pierde al cerrar la
   app, y en la tarea 6 se cambia por uno persistente sin tocar el ViewModel.
2. **Dónde se ve «Correo o contraseña incorrectos».** Propuesta: un `ErrorText` encima del botón
   «Entrar», no debajo de un campo (señalar uno solo diría cuál falla). Se borra al editar
   cualquiera de los dos campos.
3. **Rellenar el correo al llegar desde el registro** (opcional, paso 7). Propuesta: sí, con una
   ruta `Route.Login(email: String? = null)`. Toca `Routes.kt` y la navegación del registro.

## Tareas

- [x] **1. Contrato (Kotlin puro, test primero).** `Credentials(email, password)` con
  `toString()` sin contraseña; `Session(token, expiresAt, account)` con `toString()` sin token;
  `LogInOutcome` (`LoggedIn(session)`, `Rejected(errors)`, `InvalidCredentials`, `Unreachable`,
  `ServerFailure`); `AccountGateway.logIn(credentials)`; `LoginForm.toCredentials()` (recorta el
  correo, no la contraseña; `null` si el formulario no es válido). `FakeAccountGateway` aprende a
  responder también a `logIn`.
  *Acepta si:* tests de `toCredentials()` y de los `toString()`; sin dependencias nuevas.
- [x] **2. Estado del envío en `LoginForm` (Kotlin puro, test primero).** `submission` y
  `serverProblem`; `sending()`, `applying(outcome)`. `InvalidCredentials` → error general
  `FieldError.INVALID_CREDENTIALS`; `Rejected` → errores en su campo (`email`/`password`);
  `Unreachable`/`ServerFailure` → `Failed`. Editar un campo borra el error del servidor.
  *Acepta si:* un test por desenlace; `LoginFormTest` sigue en verde.
- [x] **3. Adaptador HTTP (test primero con `MockEngine`).** `LogInRequestDto`,
  `SessionResponseDto` (reutiliza `UserResponseDto`), mapper y `HttpAccountGateway.logIn`:
  201 → `LoggedIn`, 400 → `Rejected`, 401 → `InvalidCredentials`, sin red → `Unreachable`,
  5xx o cuerpo raro → `ServerFailure`.
  *Acepta si:* un test por caso; el JSON enviado coincide con `LogInRequest.java`; el token no
  aparece en ningún `toString()`.
- [x] **4. Sesión en memoria.** Puerto `SessionStore` e `InMemorySessionStore` en `sharedLogic`
  (con tests); `AppContainer.sessionStore`.
  *Acepta si:* guardar, leer y borrar funcionan en los tests.
- [x] **5. ViewModel.** `LoginViewModel(accountGateway, sessionStore)` con `factory`, como el del
  registro: `onSubmit()` lanza la llamada en `viewModelScope`; con `LoggedIn` guarda la sesión y
  pasa a `Succeeded`. `GradusNavHost` navega a Inicio al ver `Succeeded`.
  *Acepta si:* compila; sin lógica nueva en el ViewModel (solo orquesta).
- [x] **6. Pantalla.** `ScreenLogin` recibe un `LoginActions` (objeto parámetro, como
  `RegisterActions`) en vez de seis lambdas sueltas; botón «Entrar» / «Entrando…» / «Reintentar»;
  `ErrorText` del error del servidor y del fallo de red. Previews en claro y oscuro de cada estado.
  *Acepta si:* previews de: vacío, enviando, credenciales incorrectas, sin red.
- [ ] ~~**7. (Opcional) Correo rellenado tras registrarse.**~~ Descartado el 2026-11-18.
- [x] **8. Prueba completa y documentos.** Ver «Cómo comprobarlo»; marcar la tarea 5 de
  `registro-cuentas-reales.md`.

## Cómo comprobarlo

1. Abrir el emulador y, desde `backend/`, `.\gradus.ps1` (base de datos, `adb reverse` y servidor).
2. Arrancar la app con **Run ▶** (configuración `app`).
3. Correo y contraseña de una cuenta creada desde la app → Inicio con la barra; atrás → sale de la app.
4. Contraseña incorrecta → «Correo o contraseña incorrectos»; correo que no existe → **el mismo**
   aviso.
5. Parar el servidor (o `adb reverse --remove tcp:7070`) → aviso de sin conexión; restaurarlo y
   «Reintentar» → entra.
6. `adb logcat | findstr <token>` no encuentra el token.

## Trabajar con agentes

> Lee `AGENTS.md` y `docs/tareas/inicio-sesion-app.md`. Haz **solo el paso N**. Sigue test primero.
> Solo puedes tocar los ficheros que indica el paso. Al terminar, ejecuta
> `./gradlew :sharedLogic:testAndroidHostTest` (y `:app:assembleDebug` si tocas `app`), marca el
> paso, añade una línea en «Progreso» y explícanos el cambio con el formato de `AGENTS.md` §11.

Orden: 1 → 2 y 3 (independientes entre sí) → 4 → 5 → 6 → 7 → 8.

## Progreso

- 2026-11-18: documento creado. La tarea pasa del compañero a mí; en la app no había nada empezado
  (el backend sí está). Aprobadas las tres decisiones.
- 2026-11-18: paso 1 hecho (147 tests en verde, 11 nuevos: 6 en `CredentialsTest`, 2 en
  `SessionTest` y 3 en `AccountGatewaysTest`; `:app:assembleDebug` sin avisos). En `account/`:
  `Credentials.kt` (con `LoginForm.toCredentials()` como extensión, para que `model/` no dependa de
  `account/`), `Session.kt`, `LogInOutcome.kt` y `AccountGateway.logIn`. `FakeAccountGateway` gana
  `logInAnswer`, un constructor `FakeAccountGateway(LogInOutcome)` y `receivedCredentials`.
  **Provisional:** `HttpAccountGateway.logIn` es un `TODO(...)` hasta el paso 3 (nadie lo llama
  todavía; el ViewModel lo usará en el paso 5).

- 2026-11-18: paso 3 hecho (156 tests en verde, 9 nuevos en `HttpAccountGatewayLogInTest`; los 11
  del registro siguen en verde; `:app:assembleDebug` sin avisos). `LogInRequestDto` y
  `SessionResponseDto` (reutiliza `UserResponseDto`; `toString()` sin contraseña ni token), mappers
  `Credentials.toDto()` y `SessionResponseDto.toSession()`, y `HttpAccountGateway.logIn` (ya no hay
  `TODO`). El `try/catch` común pasa a una función de orden superior `callServer(...)` que usan
  `register` y `logIn`. En los tests, `MockServer.kt` reúne `gatewayAnswering`, `json` y
  `USER_JSON`, que comparten los dos ficheros de tests del adaptador. Comprobado con `curl` contra
  el backend real: 201 con `{token, expiresAt, user}` (la forma de `SessionResponseDto`), y 401 con
  `INVALID_CREDENTIALS` tanto con contraseña incorrecta como con un correo que no existe.

- 2026-11-18: paso 2 hecho (177 tests en verde, 21 nuevos: 12 en `LoginFormSubmissionTest` y 9 en
  `LogInApplyingOutcomeTest`; los 12 de `LoginFormTest` siguen en verde; app sin avisos).
  `model/LoginProblem.kt` (`LoginField` + `LoginProblem(field?, error)`, `field = null` = error de
  todo el formulario), `FieldError.INVALID_CREDENTIALS` y su texto «Correo o contraseña
  incorrectos». `LoginForm` gana `submission`, `serverProblem`, `generalError`, `canSend`,
  `sending()` (si no se puede enviar, solo muestra los errores), `succeeded()`, `failed()` y
  `rejected()`; editar cualquier campo borra el error del servidor. `account/LogInApplyingOutcome.kt`
  con `LoginForm.applying(LogInOutcome)` y su tabla de traducción (`email`/`REQUIRED`,
  `email`/`INVALID_EMAIL`, `password`/`REQUIRED`). Tras credenciales incorrectas se puede volver a
  enviar. `LoginViewModel` todavía no lo usa (paso 5).

- 2026-11-18: paso 4 hecho (182 tests en verde, 5 nuevos en `InMemorySessionStoreTest`; app sin
  avisos). Puerto `account/SessionStore.kt` (`save`, `current`, `clear`, todas `suspend` para que la
  versión cifrada en disco de la tarea 6 quepa sin cambiar el puerto) e `InMemorySessionStore` (con
  un `MutableStateFlow`). `AppContainer.sessionStore` (`by lazy`). Nadie lo usa todavía (paso 5).

- 2026-11-18: paso 5 hecho (`:app:assembleDebug` sin avisos). `LoginViewModel(accountGateway,
  sessionStore)` con `factory`; `onSubmit()` ya no devuelve `Boolean`: con datos no válidos o un
  envío en curso solo marca el formulario, y si no, `sending()` → `logIn` en `viewModelScope` →
  guarda la sesión (si `LoggedIn`) → `applying(outcome)`. `GradusNavHost` recibe `sessionStore`
  (se lo pasa `MainActivity`) y navega a Inicio con `LaunchedEffect` al ver `Succeeded`.
  Probado en el emulador contra el backend real: contraseña incorrecta y correo inexistente → se
  queda en el inicio de sesión; datos correctos → Inicio con la barra y una fila nueva en
  `sessions`; atrás desde Inicio → sale de la app; sin conexión → se queda, sin cerrarse; el token
  no aparece en logcat. Los avisos («Correo o contraseña incorrectos», sin conexión) y «Entrando…»
  aún no se ven: son el paso 6.

- 2026-11-18: paso 6 hecho (`:app:assembleDebug` sin avisos). `ui/screens/login/LoginActions.kt`
  (objeto parámetro con las 5 lambdas); `ScreenLogin(form, actions, modifier)`; aviso encima del
  botón con `ErrorText` (primero `generalError`, si no, el motivo del fallo de red/servidor); botón
  «Entrar» / «Entrando…» / «Reintentar», desactivado solo mientras se envía. Para reutilizar, el
  texto del botón según el envío pasa a `ui/components/SubmissionTexts.kt`
  (`Submission.buttonText(idle, sending)`), que ahora usan el registro y el inicio de sesión.
  Previews nuevas: enviando, credenciales incorrectas y sin conexión (más las de vacío y errores).
  Probado en el emulador: contraseña incorrecta → «Correo o contraseña incorrectos» encima de
  «Entrar»; al editar la contraseña desaparece; sin conexión → aviso y «Reintentar»; al restaurar la
  conexión, «Reintentar» entra a Inicio; el registro sigue mostrando «Crear cuenta». «Entrando…» no
  se llega a ver en el emulador (el servidor local responde enseguida): está en la preview.

- 2026-11-18: paso 7 descartado (decisión nuestra). Paso 8 hecho:
  182 tests de `sharedLogic`, 3 instrumentados y los del backend (`mvn test`) en verde;
  `:app:assembleDebug` y `:app:assembleRelease` sin avisos (un primer intento de release falló en
  `processReleaseResources` y no se repitió en tres intentos más, uno de ellos limpio y sin caché).
  Recorrido en el emulador con una cuenta nueva: registro → Inicio de sesión → entrar → Inicio con
  la barra; atrás → sale de la app; contraseña incorrecta y correo inexistente → el mismo «Correo o
  contraseña incorrectos»; sin conexión → aviso y «Reintentar» entra al volver la red. La cuenta y
  sus sesiones están en `users` y `sessions`. Ni el token ni la contraseña aparecen en los logs de
  la app (solo en los de `adbd`, por las órdenes `adb input text` del propio script de prueba).
  **Tarea completada.**

## Siguiente paso

Ninguno: la tarea 6 (recordar la sesión) se ha descartado. La sesión se queda en memoria.
