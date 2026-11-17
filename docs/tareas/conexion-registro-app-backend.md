# Conexión del registro: app ↔ backend

Desarrolla la **tarea 4** de `registro-cuentas-reales.md` («App: llamada real»). Usa el endpoint
`POST /api/users` de `backend/docs/tareas/registro-usuario.md` y los códigos de error de
`registro-cuentas-reales.md` («Códigos de error»).

## Objetivo

Que «Crear cuenta» (o el deslizador, cuando exista) cree una cuenta de verdad en el backend, y que
el **transporte** (hoy HTTP + JSON, quizá mañana gRPC) se pueda cambiar tocando el menor código
posible: un adaptador nuevo y una línea en el sitio donde se elige.

## Idea de diseño: puerto y adaptadores

La app no sabe que existe HTTP. Habla con una interfaz (el **puerto**) y en un único sitio se decide
qué implementación (el **adaptador**) se usa. Es lo mismo que ya hace el backend: `RegisterUser`
no sabe nada de Javalin; `UserController` es el adaptador HTTP.

```
 app (Android)                         sharedLogic (KMP, commonMain)
┌──────────────────────┐    usa     ┌────────────────────────────────────────────┐
│ RegisterViewModel    │──────────▶│ AccountGateway (interfaz, el puerto)        │
└──────────────────────┘            │   suspend fun register(NewAccount)          │
          ▲ recibe uno              │        : RegistrationOutcome                │
┌──────────────────────┐            └────────────────────────────────────────────┘
│ AppContainer         │  elige           ▲ implementa            ▲ implementará
│ (composition root)   │─────────▶ HttpAccountGateway       GrpcAccountGateway
└──────────────────────┘            (account/http/: DTO JSON,  (futuro: .proto,
                                     Ktor, códigos HTTP)        status gRPC)
```

Qué depende del transporte y qué no:

| Pieza | ¿Cambia al pasar a gRPC? |
|---|---|
| `NewAccount`, `Account`, `RegistrationOutcome`, `AccountGateway` | No |
| `ApiErrorCode` y su traducción a `FieldError` / paso | No (los mismos códigos irán en el error gRPC) |
| `RegisterForm`, `RegisterViewModel`, pantallas | No |
| `account/http/` (DTO `@Serializable`, `HttpAccountGateway`) | Se añade `account/grpc/` al lado |
| `AppContainer` | Una línea: qué adaptador se crea |
| Backend: `RegisterUser` y el dominio | No |
| Backend: `web/UserController` | Se añade `grpc/UserGrpcService` al lado, llamando al mismo `RegisterUser` |

Patrones que usamos (y que hay que nombrar al explicarlo):

- **Puerto y adaptador (arquitectura hexagonal)** y **Strategy**: `AccountGateway` con varias
  implementaciones intercambiables.
- **DTO + mapper**: lo que viaja por la red (`…Dto`) no es el modelo de la app; un mapper traduce.
  Así, si el JSON o el `.proto` cambian, el modelo no.
- **Resultado sellado en vez de excepciones** (estilo funcional): `RegistrationOutcome` es una
  `sealed interface`, igual que `RegistrationResult` en el backend. Los fallos de red también son
  un valor, no una excepción que se escapa.
- **Factory method**: `AccountGateways.http(baseUrl)` construye el adaptador configurado.
- **Composition root / inyección manual**: `AppContainer` es el único sitio que conoce la
  implementación concreta (como `App.java` en el backend).

## Alcance

- Contrato de cuentas transporte‑neutral en `sharedLogic/commonMain` (paquete
  `es.ulpgc.pamn.dominio.account`).
- Adaptador HTTP en `sharedLogic` (paquete `es.ulpgc.pamn.dominio.account.http`) con un cliente
  HTTP multiplataforma, para que sirva también en iOS.
- Estados de envío en el registro: enviando, error de red con reintento, error del servidor.
- Errores del servidor colocados en su paso (p. ej. `EMAIL_TAKEN` vuelve al paso del correo).
- Permiso de internet y tráfico HTTP sin cifrar **solo** hacia el emulador (`10.0.2.2`).

## Fuera de alcance

- gRPC (solo dejamos el camino preparado; ver «Cómo pasar a gRPC»).
- Inicio de sesión, tokens y recordar la sesión (el puerto podrá crecer con `login` más adelante).
- HTTPS y servidor desplegado: de momento, backend local en el ordenador.
- Deslizador «Desliza para registrarte» (T11 de `registro-login.md`): usamos el botón provisional.

## Decisiones (aprobadas el 2026-11-17)

1. **Dependencias** (hay que aprobarlas; van en `gradle/libs.versions.toml`). Propuesta:
   Ktor Client (`core`, `content-negotiation`, `serialization-kotlinx-json`, motor `okhttp` en
   Android y `darwin` en iOS), `kotlinx-coroutines-core`, `kotlinx-serialization-json`; en tests,
   `ktor-client-mock` y `kotlinx-coroutines-test`. **Aprobado.** Versiones comprobadas en Maven
   Central: Ktor 3.6.1, coroutines 1.11.0 y serialization 1.11.0 (las que usa Ktor 3.6.1). Alternativa solo Android: Retrofit/OkHttp en `app`; descartada porque
   no sirve para iOS.
2. **Dónde va la URL base**: constante en `AppContainer` (sin tocar Gradle) o `BuildConfig`
   (hay que activar `buildConfig` en `app/build.gradle.kts`). **Aprobado: constante**, por ahora.
3. **Paquete nuevo** `account/` junto a `model/` en `sharedLogic`. **Aprobado** (2026-11-17).

## Tareas

Cada paso es pequeño, se puede encargar a un agente por separado (ver «Trabajar con agentes») y
termina con los tests en verde.

- [x] **1. Contrato de cuentas (Kotlin puro, test primero).** `NewAccount(name, email, password,
  acceptedTerms)`, `Account(id, name, email, createdAt)`, `RegistrationOutcome`
  (`Registered`, `Rejected(errors)`, `EmailTaken`, `Unreachable`, `ServerFailure`) y la interfaz
  `AccountGateway`. Más `RegisterForm.toNewAccount()` (recorta nombre y correo; la contraseña no).
  *Ficheros:* `sharedLogic/.../account/*.kt`, `RegisterForm.kt`, tests.
  *Acepta si:* `toNewAccount()` tiene tests y no hay ninguna dependencia nueva.
- [x] **2. Traducción de códigos (Kotlin puro, test primero).** `ApiErrorCode` (espejo de
  `ErrorCode.java`, con `UNKNOWN` para códigos futuros) y `ServerFieldError(field, code)`. Función
  que dice a qué `RegisterStep` y a qué `FieldError` corresponde cada error. Se añade
  `FieldError.EMAIL_TAKEN` (y su texto en `FieldErrorMessages.kt`).
  *Acepta si:* hay un test por cada fila de la tabla «Códigos de error»; un código desconocido no
  rompe nada.
- [x] **3. Estado del envío en el formulario (Kotlin puro, test primero).** `RegisterForm` gana
  `submission` (`Idle`, `Sending`, `Failed(reason)`) y `serverError`; métodos `sending()` y
  `applying(outcome)`. Con `Rejected`/`EmailTaken` vuelve al primer paso con error y lo muestra;
  editar ese campo borra el error del servidor. `canRegister` es `false` mientras se envía.
  *Acepta si:* tests de cada desenlace; `RegisterFormTest` sigue en verde.
- [x] **4. Dependencias.** Tras decidir el punto 1 de «Decisiones pendientes»: `libs.versions.toml`
  y `sharedLogic/build.gradle.kts` (plugin de serialización y dependencias por `sourceSet`).
  *Acepta si:* `./gradlew :sharedLogic:testAndroidHostTest` y `./gradlew :app:assembleDebug`
  compilan sin avisos nuevos.
- [x] **5. Adaptador HTTP (test primero con `MockEngine`).** DTO `@Serializable`
  (`RegisterUserRequestDto`, `UserResponseDto`, `ErrorResponseDto`), mapper y
  `HttpAccountGateway`: 201 → `Registered`, 400 → `Rejected`, 409 → `EmailTaken`,
  sin conexión/tiempo agotado → `Unreachable`, 5xx o cuerpo raro → `ServerFailure`.
  *Acepta si:* un test por cada caso; el JSON enviado coincide con `RegisterUserRequest.java`;
  nada fuera de `account/http/` importa Ktor.
- [x] **6. Composición.** `AccountGateways.http(baseUrl)` (factory) y
  `app/.../data/AppContainer.kt` con la URL `http://10.0.2.2:7070`. `RegisterViewModel` recibe el
  `AccountGateway` por constructor (factory de ViewModel en `GradusNavHost`).
  *Acepta si:* las previews no necesitan red (usan un `FakeAccountGateway`).
- [x] **7. Android: red.** Permiso `INTERNET` en `AndroidManifest.xml` y
  `network_security_config.xml` que permite HTTP sin cifrar solo a `10.0.2.2`.
  *Acepta si:* el resto del tráfico sin cifrar sigue bloqueado.
- [ ] **8. ViewModel y pantalla.** `onRegister()` lanza la llamada en `viewModelScope`; al
  registrarse navega a Inicio; botón «Creando cuenta…» desactivado mientras se envía; aviso
  «Sin conexión» con «Reintentar». Previews en claro y oscuro de cada estado.
  *Acepta si:* ver «Cómo comprobarlo».
- [ ] **9. Prueba completa y documentos.** Probar en el emulador; marcar la tarea 4 de
  `registro-cuentas-reales.md`; anotar en `especificacion.md` que el cliente usa Ktor.

## Cómo comprobarlo (al final)

1. `cd backend && docker compose up -d && mvn compile exec:java`.
2. Arrancar la app en el emulador y registrarse con datos válidos → llega a Inicio y la cuenta
   aparece en `SELECT name, email FROM users;`.
3. Repetir con el mismo correo → vuelve al paso del correo con «Ya existe una cuenta…».
4. Parar el backend y registrarse → aviso de sin conexión; arrancarlo y pulsar «Reintentar» → funciona.

## Cómo pasar a gRPC (cuando toque)

1. Backend: `.proto` con `RegisterUser(RegisterUserRequest) returns (RegisterUserReply)`; los
   errores llevan los mismos `field` y `code`. Nuevo adaptador `grpc/UserGrpcService` que llama a
   `RegisterUser`, arrancado desde `App.java` junto a (o en lugar de) Javalin.
2. App: nuevo `account/grpc/GrpcAccountGateway` que implementa `AccountGateway` y traduce el
   status gRPC a `RegistrationOutcome`. Ojo: `grpc-kotlin` solo funciona en JVM/Android (iría en
   `androidMain`); para iOS habría que mirar una librería multiplataforma (por ejemplo Wire).
   Comprobarlo entonces.
3. `AppContainer`: cambiar `AccountGateways.http(…)` por `AccountGateways.grpc(…)`.
4. Ni el ViewModel, ni las pantallas, ni `RegisterForm`, ni sus tests cambian.

## Trabajar con agentes

Cada paso se puede encargar a un agente (o a una sesión nueva de Claude Code) de forma aislada.
Plantilla de petición:

> Lee `AGENTS.md` y `docs/tareas/conexion-registro-app-backend.md`. Haz **solo el paso N**.
> Sigue test primero. Solo puedes tocar los ficheros que indica el paso. Al terminar, ejecuta
> `./gradlew :sharedLogic:testAndroidHostTest`, marca el paso como hecho, añade una línea en
> «Progreso» y explícanos el cambio con el formato de `AGENTS.md` §11.

Reglas para repartir:

- Los pasos 1, 2 y 3 son Kotlin puro y no dependen de nada externo: se pueden hacer en orden por
  un agente cada uno (el 2 y el 3 usan tipos del 1).
- El paso 4 lo aprobamos nosotros antes (dependencias y Gradle).
- El 5 depende del 4; el 6 del 1 y el 5; el 7 es independiente; el 8 depende de todos.
- Nada de commits: los revisamos y los hacemos nosotros.

## Progreso

- 2026-11-14: documento creado y plan propuesto. Pendiente de aprobar el plan y las decisiones.
- 2026-11-17: plan aprobado. Paso 1 hecho (87 tests en verde, 6 nuevos en `NewAccountTest`; sin
  dependencias ni avisos nuevos). `toNewAccount()` es una función de extensión en
  `account/NewAccount.kt` (no dentro de `RegisterForm.kt`), así `model/` no depende de `account/`;
  devuelve `null` si `canRegister` es falso. `Rejected` lleva `ServerFieldError(field, code)` con
  el código como texto; el paso 2 lo traduce a `ApiErrorCode`. `createdAt` es `String` (ISO‑8601).
- 2026-11-17: paso 2 hecho (104 tests en verde, 17 nuevos en `RegisterProblemTest`; la app compila).
  `ApiErrorCode.fromName` (UNKNOWN si no lo conoce), `ServerFieldError.apiCode`, `RegisterProblem(step,
  error)` con una tabla (campo, código) → problema en `account/RegisterProblem.kt` y
  `firstRegisterProblem()` (el del paso más temprano). Además de `FieldError.EMAIL_TAKEN` se añade
  `FieldError.TERMS_NOT_ACCEPTED`, con sus textos en `FieldErrorMessages.kt`.
- 2026-11-17: paso 3 hecho (121 tests en verde, 17 nuevos en `RegisterFormSubmissionTest` y
  `ApplyingOutcomeTest`; `RegisterFormTest` sin cambios y en verde). `Submission` sellado con
  `Idle`, `Sending`, `Succeeded` (añadido: impide crear la cuenta dos veces mientras se navega) y
  `Failed(FailureReason)`. `RegisterForm` gana `serverProblem`, `serverError`, `sending()`,
  `succeeded()`, `failed()` y `rejected()`; un error del servidor bloquea su paso hasta editar ese
  dato. `applying(outcome)` es una extensión en `account/ApplyingOutcome.kt` y la clase
  `RegisterProblem` pasa a `model/`, para que `model/` no dependa de `account/`. Un 400 sin error de
  campo conocido se trata como `SERVER_FAILURE`.
- 2026-11-17: paso 4 hecho. `libs.versions.toml` (versiones `ktor`, `kotlinx-coroutines`,
  `kotlinx-serialization` y 9 librerías) y `sharedLogic/build.gradle.kts` (plugin de serialización;
  `commonMain`, `androidMain` con `okhttp`, `iosMain` con `darwin`, `commonTest` con `mock` y
  `coroutines-test`). `testAndroidHostTest` (121 tests) y `:app:assembleDebug` en verde, sin avisos
  nuevos. En `app`, `kotlinx-coroutines-core` sube de 1.9.0 a 1.11.0 por arrastre.
- 2026-11-17: paso 5 hecho (132 tests en verde, 11 nuevos en `HttpAccountGatewayTest`). En
  `account/http/`: `AccountDtos.kt` (3 DTO + `FieldErrorDto`), `AccountDtoMappers.kt`,
  `GradusHttpClient.kt` (factory del cliente: `expectSuccess = false`, `ignoreUnknownKeys`, tiempo
  de conexión 5 s y de petición 10 s) y `HttpAccountGateway.kt`. Todo es `internal`: fuera del
  módulo solo se verá la factory del paso 6. Solo esos dos ficheros importan Ktor. Falta probarlo
  contra el backend real (paso 9).
- 2026-11-17: paso 6 hecho (136 tests en verde, 4 nuevos en `AccountGatewaysTest`; `:app:assembleDebug`
  sin avisos). `AccountGateways.http(baseUrl)` (factory pública) y `FakeAccountGateway` en
  `sharedLogic/.../account/` (puede responder un desenlace fijo y recuerda lo recibido).
  `app/.../data/AppContainer.kt` es un `object` con la URL y el gateway `by lazy`.
  `RegisterViewModel(accountGateway)` con `factory(...)` (`viewModelFactory`); `GradusNavHost`
  recibe el gateway por parámetro y `MainActivity` le pasa `AppContainer.accountGateway`. Las
  previews no cambian: las pantallas no tienen estado y nunca crean el ViewModel. `app` no importa Ktor.
- 2026-11-17: paso 7 hecho (aprobado tocar el manifiesto). Permiso `INTERNET` y
  `android:networkSecurityConfig` en `AndroidManifest.xml`. Dos versiones de
  `network_security_config.xml`: `src/main/res/xml/` (release: nada sin cifrar) y
  `src/debug/res/xml/` (debug: HTTP sin cifrar solo hacia `10.0.2.2`). Comprobado en los recursos
  empaquetados de cada variante. `:app:assembleDebug` y `:app:processReleaseResources` en verde.

## Siguiente paso

Paso 8: `RegisterViewModel.onRegister()` llama al gateway en `viewModelScope`; botón «Creando
cuenta…», aviso «Sin conexión» con «Reintentar» y navegación a Inicio al registrarse. Antes,
decidir el punto 1 de «Decisiones pendientes» de `registro-cuentas-reales.md` (¿iniciar sesión
solo tras registrarse?).
