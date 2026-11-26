# Progreso en la app — histórico de las partes C y D

Lo terminado de `docs/tareas/progreso-app.md`: la parte C (tareas 7–10, cerrada el 2026-11-26) y, al
final, el detalle de los pasos de la parte D ya revisados (desde la tarea 11). No hace
falta leerlo para continuar: cómo funciona lo hecho está en `docs/arquitectura-progreso.md`, y las
decisiones que siguen vigentes, en `progreso-app.md`. Sirve para saber por qué se hizo algo así.

## Tareas y pasos (todos hechos y revisados)

Orden aprobado el 2026-11-25: 9a → 9b → 10a → 10b → 7a → 7b → 7c → 7d → 8a → 8b.

### [x] 9. La puerta en la interfaz

- **[x] 9a · Combinar acceso y estado del módulo (`sharedLogic`).** Sonnet 5.5, esfuerzo medio.
  Función pura `Course.tapModule(moduleId, mastered, access): ModuleTap?` (`OpenPractice | Locked(missing)
  | NotEnrolled`); sin inscripción gana `NotEnrolled` (C-6); `null` si el módulo no existe. 11 tests.
- **[x] 9b · El camino y la práctica usan la puerta (`app`).** Opus 5.5, esfuerzo medio.
  `CourseViewModel` escucha `enrolled` y `mastered` con `combine` y decide solo con `tapModule`; aviso
  `CourseNotice.NotEnrolled`. `ExerciseViewModel.access` (leído una vez, decisión 7) y `MainNavGraph`
  vuelve atrás sin inscripción antes de componer la práctica. 21 tests. Probado en el emulador.

### [x] 10. Botón de inscribirse y página de relleno

- **[x] 10a · `EnrollmentViewModel` + `EnrollButton`.** Sonnet 5.5, esfuerzo medio.
  `EnrollmentState` (`Unknown | NotEnrolled | Enrolling | Enrolled`) en `ui/components`; dos toques, una
  inscripción. 12 tests JVM y 8 instrumentados.
- **[x] 10b · Página de relleno y navegación.** Sonnet 5.5, esfuerzo medio.
  `Route.CourseOverview(courseId)` y `ScreenCourseOverview` (`EnrollButton` + «Ir al camino» siempre
  activo). Buscar navega a ella. Arreglado en la revisión: fondo y márgenes del sistema (`Surface` +
  `safeDrawingPadding`, como `ScreenLogin`). 11 tests instrumentados. Probado de punta a punta.

### [x] 7. Room por usuario

- **[x] 7a · Dependencias y esquema.** Opus 5.5, esfuerzo medio. Room 2.8.5 + KSP 2.3.14 funcionan con
  Kotlin 2.4.21 y AGP 9.5 (Kotlin integrado), sin `room-ktx`. Entidades, `InstantConverter` (nanosegundos,
  exacto y conservando el orden; préstamo de un segundo cerca del mínimo), DAO mínimo y
  `ProgressDatabase.create(context, name)`. 11 tests JVM y 9 instrumentados.
- **[x] 7b · `RoomProgressStore`: `ProgressStore` + `EnrollmentStore`.** Opus 5.5, esfuerzo alto.
  `INSERT OR IGNORE`, `watch` con `distinctUntilChanged`, sin transacción (decisión 7). 48 tests en 7
  clases; los `Flow` se esperan con un `Channel` y una «barrera», sin `delay`.
- **[x] 7c · `ProgressSyncStore`: `pending` y `settle`.** Opus 5.5, esfuerzo alto. `@Transaction` en el
  DAO; sin UPSERT (SQLite 3.22 en Android 10). Todo o nada probado con una fecha remota de 2300 (sin
  `@Transaction` fallan 2 de 3 tests). B-11 (respuesta tardía) con su test. 7 tests JVM y 40
  instrumentados.
- **[x] 7d · `Application`, `ProgressModule` y cambio a Room.** Sonnet 5.5, esfuerzo medio.
  `GradusApplication`, `android:name` en el manifiesto, `ProgressModule` (*Facade*), `AppContainer.init`.
  Comprobado en el código de Room que `build()` no abre el fichero. Probado: sobrevive a `force-stop` y
  otro usuario no lo ve.

### [x] 8. Sincronizar con la app abierta

- **[x] 8a · `ProgressSyncLauncher`.** Opus 5.5, esfuerzo medio. `ProgressSyncRequester` (`fun interface`)
  y el launcher en un `appScope` (`SupervisorJob() + Dispatchers.Default`) inyectado; sin sesión no llama
  y `lastOutcome` no cambia; sin `try/catch`, `Mutex` ni reintentos. 12 tests JVM (cancelar a quien lo
  pidió no la cancela) y uno instrumentado.
- **[x] 8b · Los tres momentos.** Sonnet 5.5, esfuerzo medio. `LoginViewModel` (tras guardar la sesión),
  `EnrollmentViewModel` (tras `enroll`) y `ExerciseViewModel` (tras `markMastered`, al aprobar) piden
  sincronizar. 24 tests nuevos; 263 de 263 en `app/src/test`. Probado de punta a punta por los usuarios
  con permiso para escribir en la base de desarrollo: tras `adb shell pm clear` y volver a entrar, la
  inscripción y lo dominado vuelven del servidor.

## Progreso de la parte C (resumen)

- 2026-11-25: documento creado; decisiones C-1 a C-6 tomadas; plan aprobado.
- 2026-11-25: tareas 9 y 10 hechas (la app deja de cerrarse al dominar y permite inscribirse).
- 2026-11-25/26: tarea 7 hecha (el progreso se guarda en Room por usuario y sobrevive a cerrar la app).
- 2026-11-26: tarea 8 hecha (se sincroniza tras iniciar sesión, inscribirse y dominar). **Parte C
  terminada.**

Lo que conviene recordar de las revisiones:
- Los tests instrumentados pueden acabar en BUILD SUCCESSFUL sin ejecutar nada (emulador caído): mirar
  siempre el XML. El emulador `Pixel_9a` entró una vez en bucle (HAL de UWB); se arregló con un arranque
  en frío (`-no-snapshot-load`).
- Las previews no enseñan las barras del sistema ni el fondo real (10b): las pantallas nuevas se miran
  en el emulador.
- Buscar avisa solo con el `id` del curso, no con su título (C-6).

## Parte D — tarea 11 (sesión guardada cifrada): detalle de los pasos

Modelos usados: 11a Sonnet 5.5 · medio; 11b Opus 5.5 · medio; 11c (en tres sub-pasos) Sonnet 5.5 · alto.
Commits: `add: … (progress task 11a)`, `… 11b`, `… 11c-1`, `… 11c-2` y `… 11c-3`.

- **11a** (`sharedLogic`, 25 tests). `account/storage/` (`StoredSessionDto`, `encodeSession`,
  `decodeSession`: lo ilegible da `null`, el token no sale en ningún `toString`; el texto de `encodeSession`
  sí lleva el token en claro, para cifrarlo), `account/SessionStoreExtensions.kt` (`clearIfCurrent`) y
  `course/progress/SessionProgressSync.kt` + `SessionSyncResult.kt`. Decisiones de la revisión:
  `clearIfCurrent` compara **solo el token** (un 401 invalida el token sea cual sea su `expiresAt`); DTO del
  disco aparte del de la red; sin campo `version`. Límite: leer y borrar son dos pasos (el puerto no tiene
  uno atómico); el caso realista (guardar mientras la petición viaja) está probado.
- **11b** (app + DataStore 1.3.0; 9 JVM + 23 instrumentados). `SessionCipher`, `KeystoreSessionCipher`,
  `EncryptedSessionSerializer` y `DataStoreSessionStore`. Cambios pedidos en la revisión de la propuesta:
  `decrypt` no crea la clave; buscar o crear va dentro de un cerrojo (generar con un alias existente
  sustituye la clave); `scope` con valor por defecto; los tests nunca usan `DEFAULT_ALIAS`. Decisiones:
  `DataStore<Session?>` sin envoltorio; «sin sesión» = fichero vacío; una `IOException` del disco sale tal
  cual (no borra una sesión buena). El test de hilos documenta, no demuestra (no se ha visto fallar sin el
  cerrojo).
- **11c-1** (JVM 4 + 11; 33 instrumentados). `AppContainer`: `sessionStore` = `DataStoreSessionStore` (uno
  solo, necesita `init`) y `sessionUpdates`; `requireContext()`; comentario de `SessionCipher` corregido;
  `AppContainerInitTest.session_store_works_without_init` cambiado con permiso. Hallazgo probado en
  `DataStoreSessionStoreFailureTest`: con un fallo del sistema que no pasa, `save` también lanza.
- **11c-2** (18 JVM). `StartState` y `StartViewModel` en `ui/navigation/` (no es una pantalla); reintento
  cada 2 s con `retryWhen` porque un `Flow` que falla muere. Comprobado con una mutación.
- **11c-3** (7 JVM + 10 instrumentados). `StartNavigation.kt`, `GradusNavHost` y `MainActivity`. Cambios de
  la revisión de la propuesta: `leaveMain()` deja `[Welcome, Login]` en vez de tocar `enterHome` y la flecha
  atrás de Login; sin componente nuevo para `Loading`; test de recrear tras iniciar sesión. Decisión del
  implementador: al iniciar sesión, `enterHome()` espera también a `start == SignedIn` (si no, un fotograma
  de desfase devolvería a Login); probada.
