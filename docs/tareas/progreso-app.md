# Progreso en la app (partes C y D: tareas 7–13)

Parte C y D del seguimiento del progreso (índice común: `docs/tareas/seguimiento-progreso.md`).
Referencia viva de cómo funciona lo que ya hay: `docs/arquitectura-progreso.md`. Rama:
`dev/course-progress-tracking`.

Estado: **plan de la parte C aprobado; tarea 9 hecha; 10a hecho (2026-11-25).** Siguiente: 10b.

## Relevo

Se **reescribe** al final de cada sesión; no se acumula.

- **Dónde estamos (2026-11-25):** tarea 9 hecha y en commits (9a, 9b; 9b probado en el emulador por los
  usuarios). La app ya no se cierra, pero nadie puede inscribirse (ni, por tanto, estudiar) hasta 10b.
- **Paso que toca:** 10b (Sonnet 5.5, esfuerzo medio), mensaje preparado. 10a hecho y revisado
  (`EnrollmentViewModel`, `EnrollButton`, `EnrollmentState`; todavía sin usar en ninguna pantalla).
- **Abierto:** el plan de la parte D se hace al cerrar C. El botón de inscribirse en el camino queda
  para después (C-6).
- **Riesgos:**
  - KSP 2.3.14 con Kotlin 2.4.21 y el Kotlin integrado de AGP 9.5 no está comprobado: el paso 7a lo
    comprueba antes de escribir código y, si falla, se para.
  - La rama `dev/joel` toca `AppContainer`, `CourseViewModel`, `ScreenCourse`, `MainNavGraph`,
    `libs.versions.toml` y `build.gradle.kts`: cada paso los toca lo mínimo.
  - Hasta D11 la sesión está en memoria: si Android mata el proceso y restaura una pantalla de `Main`,
    no hay sesión (el camino sale vacío e inscribirse lanzaría). Conocido y aceptado hasta D11.
- **Qué mirar en la próxima revisión (10b):** que «Ir al camino» viva solo en la página (no en
  `EnrollButton`) y esté siempre activo; que el `EnrollmentViewModel` se cree con la entrada de la
  página; que Buscar navegue a la página y no al camino; cambios mínimos en `Routes` y `MainNavGraph`;
  emulador de punta a punta (inscribirse, ir al camino, dominar, sin cierre).
- **Comprobar los tests:** leer el XML con `read_file` o un `grep` que imprima `<`/`>` se cuelga en
  esta máquina; usar `grep -o 'tests="[0-9]*" … timestamp="[^"]*"'` (sin las etiquetas). Si Android
  Studio tiene un Gradle ocupado, lanzar `./gradlew` desacoplado (`setsid nohup … > /tmp/log &`) y
  leer el log: la herramienta de shell corta las órdenes largas.
- **Nombres de test con comas:** la regla del índice es por iOS; vale para `sharedLogic`. En `app`
  (solo JVM) ya había nombres con comas y se aceptan.

## Objetivo

Que el estudiante se inscriba en un curso desde la app, estudie solo si está inscrito, que lo inscrito y
lo dominado se guarde en el móvil (Room, por usuario) y se sincronice con el servidor; y, en la parte D,
que funcione sin conexión y se sincronice solo.

## Alcance

- **C:** 7 Room por usuario · 8 sincronizar con la app abierta · 9 la puerta en la interfaz · 10 botón
  de inscribirse (y su página de relleno).
- **D:** 11 sesión guardada cifrada · 12 copia local del curso al inscribirse · 13 WorkManager.

## Fuera de alcance

Lo del índice: darse de baja; registro de respuestas y SM-2; Inicio real; reanudar a mitad de un
módulo; actualizar la copia local con versiones nuevas; comprobar el SHA-256. Además: cambiar el
contrato (congelado) o el comportamiento de los puertos de la parte B (si hiciera falta, se propone y
se reabre B con un paso nuevo); la página definitiva del curso (la de C10 es de relleno); inscribirse
desde el camino (C-6: se hará después reutilizando `EnrollButton` y `EnrollmentViewModel`).

## Lo que ya hay y sirve de modelo

- **Puertos y almacén en memoria** (`sharedLogic/.../course/progress/`): `ProgressStore`,
  `EnrollmentStore`, `ProgressSyncStore`, `InMemoryProgressStore`, `ProgressSync`, `SyncOutcome`,
  `studyAccess`. Room debe comportarse **igual** que `InMemoryProgressStore` (invariantes en
  `docs/arquitectura-progreso.md`); sus tests en `commonTest` son la lista de comportamientos a copiar.
- **Composition root:** `app/.../data/AppContainer.kt` (`object` con `lazy`).
- **ViewModels con factory:** `CourseViewModel`, `ExerciseViewModel` (`viewModelFactory { initializer { … } }`).
- **Tests de ViewModel** en `app/src/test` con `kotlinx-coroutines-test`: `SearchViewModelTest` y su
  doble `DelayedCourseGateway`.
- **Tests instrumentados** en `app/src/androidTest` (JUnit4): `StudyAccessibilityTest`, `CourseCardTest`.
- **Avisos del camino:** `CourseNotice` (en `CourseViewModel`) y sus textos en `CourseNoticeMessages.kt`.
- **Acciones de una pantalla:** objeto de parámetros `CourseActions`; un toque por pantalla activa con
  `dropUnlessResumed` / `onlyWhenResumed` (`MainNavGraph`).
- **Componentes:** `PrimaryButton`, `PlaceholderScreen`. **Previews:** `@LightDarkPreviews`.
- **Dobles de sharedLogic:** `FakeProgressGateway`, `FakeCourseGateway`, `InMemorySessionStore`.

## Decisiones (2026-11-25)

- **C-1 Dependencias.** Aprobadas **Room 2.8.5** (`androidx.room:room-runtime`, `room-compiler` con
  KSP) y **KSP 2.3.14** (plugin `com.google.devtools.ksp`), más `kotlinx-coroutines-test` en
  `androidTestImplementation` (ya está en el catálogo). En `libs.versions.toml`: `room` y `ksp` en
  `[versions]`, `androidx-room-runtime` y `androidx-room-compiler` en `[libraries]`, `ksp` en `[plugins]`;
  `alias(libs.plugins.ksp) apply false` en el `build.gradle.kts` raíz; el plugin y las dependencias en
  `app/build.gradle.kts`. Sin driver extra (SQLite del sistema). Room 3.0.3 existe; se descarta por
  ahora (menos documentación, y su ventaja multiplataforma no se usa: Room va en `app`, decisión 8).
  Para D, sin aprobar todavía: WorkManager 2.12.0 (D13); DataStore Preferences 1.3.0 + Android Keystore
  (D11; confirmar entonces si `security-crypto` está obsoleta).
- **C-2 Orden:** **9 → 10 → 7 → 8.** Arregla primero el cierre de la app (con el almacén en memoria,
  los puertos no cambian); Room y la sincronización se prueban después con una interfaz que ya inscribe.
- **C-3 `Context`:** `GradusApplication : Application` llama a `AppContainer.init(this)`; en el
  manifiesto, `android:name=".GradusApplication"` (aprobado). `Application` y no `MainActivity` porque
  el `Worker` de D13 arranca sin Activity. Un fichero nuevo, `data/progress/ProgressModule.kt`
  (*Facade*), crea la base, `RoomProgressStore` (un objeto, tres puertos) y, en C8, `ProgressSync` con
  ese mismo objeto. En `AppContainer`, unas pocas líneas.
- **C-4 Room por usuario:**
  - `enrollments(user_id, course_id, enrolled_at, pending)`, PK `(user_id, course_id)`.
  - `mastered_modules(user_id, course_id, module_id, mastered_at, pending)`, PK
    `(user_id, course_id, module_id)`, FK `(user_id, course_id)` → `enrollments`. `markMastered` comprueba
    antes y lanza la `IllegalStateException` del puerto; la FK es la segunda capa.
  - Fechas: `Long` con nanosegundos desde la época (*TypeConverter* de `kotlin.time.Instant`), ida y
    vuelta exacta (válido hasta 2262); «la más antigua» es `MIN`.
  - En desarrollo: versión 1, `exportSchema = false`, `fallbackToDestructiveMigration(dropAllTables = true)`.
    En la tarea 14 se congela: exportar el esquema y quitar el destructivo.
  - `courseId`: no se normaliza. Los ids del catálogo son UUID creados por el servidor (en minúsculas);
    se comprueba en la revisión de 7b de dónde sale el id y se anota en la arquitectura.
  - Tests de Room en `app/src/androidTest` (JUnit4, en el emulador): Room necesita un `Context`.
- **C-5 Sincronizar:**
  - Momentos: tras iniciar sesión (aunque no haya nada pendiente), tras inscribirse y tras dominar.
    «Al volver la conexión» pasa a D13 (WorkManager con restricción de red).
  - `ProgressSyncLauncher` (`app/data/progress/`) con un `CoroutineScope` de aplicación: la
    sincronización no se cancela al salir de la pantalla que la pidió. Sin sesión no llama. Expone
    `lastOutcome: StateFlow<SyncOutcome?>`. Los ViewModels reciben una `fun interface` para pedirla.
  - Desenlaces en C: `Synced`, `Unreachable`, `ServerFailure` y `SessionExpired`, **nada visible** (lo
    guardado está a salvo; se sube en la siguiente). D11 lleva a iniciar sesión con `SessionExpired`.
  - Volver a escuchar al cambiar de sesión: no se cambia `SessionStore`. Regla: **lo que escucha el
    progreso vive dentro del grafo `Main`**, que se crea tras iniciar sesión (cada ViewModel nuevo vuelve
    a escuchar); un futuro «cerrar sesión» debe sacar de `Main`.
- **C-6 Botón y página (decidido por los usuarios, con los ajustes del orquestador aprobados):**
  - Al tocar un curso en Buscar se abre una **página de relleno** (`Route.CourseOverview`,
    `ScreenCourseOverview`) con el botón de inscribirse. Más adelante se sustituye por la página
    definitiva; el botón (`EnrollButton`) y su lógica (`EnrollmentViewModel`) no cambian y se pueden
    usar en otros sitios.
  - La página tiene **dos botones**:
    - `EnrollButton` (reutilizable): «Inscribirme»; inscrito, «Inscrito» desactivado. No navega.
    - **«Ir al camino»**, propio de la página de relleno (se borra con ella): **siempre activo** (decisión 4,
      el camino se ve sin inscripción), con estilo secundario sin inscripción y principal tras
      inscribirse («se activa» visualmente). Tras inscribirse no se navega solo.
  - La ruta lleva solo `courseId` (corregido el 2026-11-25: Buscar avisa con el id, no con el título, y
    pasarlo obligaría a cambiar `SearchActions`, `SearchViewModel.actions`, `SearchResultsContent` y sus
    tests). La página de relleno no descarga el curso ni enseña su título: eso llegará con la definitiva.
  - En el camino, sin inscripción, tocar cualquier módulo (frontera, dominado o bloqueado) enseña
    «Inscríbete para empezar a estudiar» (antes que «bloqueado»). `ScreenCourse` no cambia salvo el texto
    del aviso.
  - Un botón de inscribirse **en el camino** (u otra solución) queda para después: no afecta a C. El
    botón no navega ni sabe dónde está, y la puerta (9a/9b) no depende de dónde se inscribe; solo
    habría que revisar entonces el texto del aviso.

## Tareas y pasos

La casilla la marca el orquestador tras revisar. Los tests nombrados son el **mínimo** (AGENTS §7).
Plan **aprobado el 2026-11-25**. Orden: 9a → 9b → 10a → 10b → 7a → 7b → 7c → 7d → 8a → 8b.

### [x] 9. La puerta en la interfaz

- **[x] 9a · Combinar acceso y estado del módulo (`sharedLogic`).** Sonnet 5.5, esfuerzo medio.
  - Función pura nueva (fichero nuevo en `course/progress/`; no cambia nada de B): con el curso, el
    módulo, lo dominado y el `StudyAccess`, devuelve qué pasa al tocar: abrir, bloqueado (con lo que
    falta) o sin inscripción; `null` si el módulo no existe. Jerarquía sellada, sin textos.
  - Tests mínimos (`commonTest`, fichero nuevo): sin inscripción, frontera, dominado y bloqueado dan «sin
    inscripción»; inscrito, frontera y dominado abren; inscrito y bloqueado da lo que falta; módulo
    inexistente da `null`.
  - Revisión: ligera (leer el fichero y los tests; ejecutarlos).
- **[x] 9b · El camino y la práctica usan la puerta (`app`).** Opus 5.5, esfuerzo medio (corrutinas).
  - `CourseViewModel` escucha `enrolled` y `mastered` y decide el toque con 9a; aviso nuevo
    `CourseNotice.NotEnrolled` y su texto en `CourseNoticeMessages.kt`. La entrada de `Route.Exercise`
    vuelve atrás sin inscripción, antes de enseñar la práctica. `AppContainer` expone el mismo
    `InMemoryProgressStore` como `ProgressStore` y como `EnrollmentStore`.
  - Tests mínimos (`app/src/test`, ficheros nuevos, con `InMemoryProgressStore`): sin inscripción no se
    abre ni frontera ni dominado (aviso, sin práctica); inscrito se abren; al inscribirse con el camino
    abierto, el siguiente toque abre; la práctica sin inscripción se cierra y no llega a `markMastered`.
  - Revisión: completa (código, tests, `:app:compileDebugKotlin`, emulador).
  - Emulador: Buscar → curso → tocar un módulo frontera: aviso, sin práctica y sin cierre.

### [ ] 10. Botón de inscribirse y página de relleno

- **[x] 10a · `EnrollmentViewModel` + `EnrollButton`.** Sonnet 5.5, esfuerzo medio.
  - Ficheros propios: el ViewModel (`courseId`, `EnrollmentStore`) expone si está inscrito y si se está
    inscribiendo; el botón (`ui/components/`) recibe el estado y `onEnroll` (*state hoisting*), 48 dp,
    previews claro/oscuro. Estados del botón: «Inscribirme», inscribiéndose (no se puede pulsar) e
    «Inscrito» (desactivado). No navega.
  - Tests mínimos: el ViewModel refleja `enrolled`; inscribirse lo cambia; dos toques seguidos, una sola
    inscripción (`app/src/test`); el botón llama a `onEnroll` y no se puede pulsar mientras se inscribe
    y que inscrito está desactivado (`app/src/androidTest`).
  - Revisión: normal (código, tests, previews).
- **[ ] 10b · Página de relleno y navegación.** Sonnet 5.5, esfuerzo medio.
  - `Route.CourseOverview(courseId)`, `ScreenCourseOverview` (texto genérico, `EnrollButton` e «Ir al
    camino», ver C-6); Buscar navega a ella; desde ella, al camino. «Ir al camino» vive en la página,
    no en `EnrollButton`: al sustituir la página se borra sin tocar el botón.
  - Tests mínimos (`app/src/androidTest`): sin inscripción se ven «Inscribirme» e «Ir al camino», los
    dos activos; inscrito, «Inscrito» desactivado e «Ir al camino» activo; cada botón avisa con su
    lambda.
  - Revisión: completa (emulador).
  - Emulador: Buscar → curso → Inscribirme → Ir al camino → dominar un módulo: el camino lo enseña y la
    app no se cierra. «Atrás» vuelve a la página y luego a Buscar.

### [ ] 7. Room por usuario

- **[ ] 7a · Dependencias y esquema.** Opus 5.5, esfuerzo medio.
  - Primero: añadir Room + KSP (C-1), sincronizar y compilar `:app` vacío; si KSP falla, parar y avisar.
  - Entidades, DAO, base de datos y *TypeConverter* de `Instant` en `app/.../data/progress/`.
  - Tests mínimos (`androidTest`): la FK impide un dominado sin inscripción; la fecha vuelve exacta.
  - Revisión: completa (diff de Gradle, sin avisos nuevos de compilación ni de Room).
- **[ ] 7b · `RoomProgressStore`: `ProgressStore` + `EnrollmentStore`.** Opus 5.5, esfuerzo alto.
  - Tests mínimos (`androidTest`, ficheros temáticos): por usuario; inscribirse dos veces conserva la
    primera fecha; dominar sin inscripción lanza y no deja filas; dominar dos veces conserva la fecha;
    sin sesión, leer da vacío/`false` y el `Flow` termina, y escribir lanza; `pending` solo al insertar;
    los `Flow` no avisan de cambios en otros cursos.
  - Revisión: completa, comparando con los invariantes de la arquitectura.
- **[ ] 7c · `ProgressSyncStore`: `pending` y `settle`.** Opus 5.5, esfuerzo alto.
  - `settle` en una sola transacción; nunca `DELETE`.
  - Tests mínimos: `pending` (módulo pendiente dentro de una inscripción ya subida, solo los módulos
    pendientes, vacío sin datos); `settle` (saca por clave sin mirar fechas, ignora lo que no existe, lo
    omitido sale y se conserva, unión con fecha mínima, lo pendiente que llega del servidor sigue
    pendiente, lo nuevo no queda pendiente, nunca borra, idempotente, lo marcado durante el viaje sigue
    pendiente); los `Flow` emiten tras `settle`; `ProgressSync` con Room y `FakeProgressGateway`.
  - Revisión: completa (SQL línea a línea).
- **[ ] 7d · `Application`, `ProgressModule` y cambio a Room.** Sonnet 5.5, esfuerzo medio.
  - `GradusApplication`, la línea del manifiesto, `ProgressModule` y `AppContainer` (C-3).
  - Revisión: completa (emulador).
  - Emulador: dominar un módulo, cerrar la app del todo, abrirla, iniciar sesión: sigue dominado. Con
    otro usuario no se ve.

### [ ] 8. Sincronizar con la app abierta

- **[ ] 8a · `ProgressSyncLauncher`.** Opus 5.5, esfuerzo medio (corrutinas y ámbitos).
  - `ProgressModule` crea `ProgressSync(store, ProgressGateways.http(baseUrl))` con el mismo objeto Room.
  - Tests mínimos (`app/src/test`, con `InMemoryProgressStore` y `FakeProgressGateway`): sin sesión no
    llama; con sesión llama aunque no haya nada pendiente; `lastOutcome` recibe el desenlace; un
    `Unreachable` deja todo pendiente y no lanza; cancelar a quien lo pidió no cancela la sincronización.
  - Revisión: completa.
- **[ ] 8b · Los tres momentos.** Sonnet 5.5, esfuerzo medio.
  - `LoginViewModel` (tras guardar la sesión), `EnrollmentViewModel` (tras inscribirse) y
    `ExerciseViewModel` (tras dominar) piden sincronizar con la `fun interface`.
  - Tests mínimos: cada uno la pide en su momento y no en otro (login fallido, aprobar ya aprobado).
  - Revisión: completa (emulador).
  - Emulador (**pedir permiso antes**: escribe en la base de desarrollo): dominar un módulo y ver la fila
    en el servidor; con el backend apagado nada se rompe, y al encenderlo y volver a dominar o iniciar
    sesión se sube lo pendiente.

### [ ] 11. Sesión guardada cifrada (parte D)

- La sesión sobrevive a cerrar la app, cifrada en el móvil; `clear` la borra. Implementa `SessionStore`
  sin cambiar el puerto. `SessionExpired` lleva a iniciar sesión. Requisito de 13.

### [ ] 12. Copia local del curso al inscribirse (parte D)

- Al inscribirse se guarda el curso; sin red, el camino y la práctica abren desde la copia.

### [ ] 13. WorkManager (parte D)

- Sincronización en segundo plano: trabajo único, con red, reintento con espera creciente; sin sesión
  no hace nada.

## Progreso

- 2026-11-25: creado el documento. Decisiones C-1 a C-5 aprobadas; C-6 decidida por los usuarios
  (página de relleno desde Buscar) con los ajustes del orquestador. Plan de la parte C aprobado.
  Inscribirse desde el camino, para después.
- 2026-11-25: **9a hecho y revisado** (11 tests, 57 clases sin fallos). `Course.tapModule(moduleId, mastered,
  access): ModuleTap?` en `course/progress/ModuleTap.kt`. Decisiones fuera del plan: un módulo
  inexistente da `null` antes de mirar el acceso (también sin inscripción); `Locked.missing` son los
  prerrequisitos **directos** no dominados, en el orden de `requires`; lo dominado abre aunque falte un
  prerrequisito (regla 5). Para 9b: usar `tapModule` con `studyAccess(enrolled)`.
- 2026-11-25: **9b hecho y revisado; probado en el emulador por los usuarios** (21 tests nuevos en `app/src/test`: `CourseViewModelGateTest`,
  `ExerciseViewModelGateTest`, `CourseNoticeMessagesTest`). `CourseViewModel` escucha `mastered` y
  `enrolled` con `combine` (sigue `Loading` hasta tener los dos) y decide el toque solo con `tapModule`.
  `CourseUiState.enrolled = false` por defecto. `ExerciseViewModel.access: StudyAccess?` se lee una vez
  (`first()`, decisión 7); `MainNavGraph` no enseña nada mientras es `null` y vuelve atrás con
  `NotEnrolled`. Las acciones de la práctica no se bloquean (decisión 3). `AppContainer`: el mismo
  almacén como `progressStore` y `enrollmentStore`. `ScreenCourse` sin cambios. Fuera del plan: el caso
  «dominado sin inscripción» usa un `ProgressStore` fijo de test (el almacén real no lo permite).

- 2026-11-25: **10a hecho y revisado** (12 tests en `app/src/test`, 8 en `app/src/androidTest`, en verde;
  previews claro/oscuro vistas). Ficheros nuevos: `ui/components/EnrollmentState.kt` y `EnrollButton.kt`,
  `ui/screens/enrollment/EnrollmentViewModel.kt` y sus dos clases de test. `EnrollmentState`
  (`Unknown`/`NotEnrolled`/`Enrolling`/`Enrolled`) vive en `components`. Dos toques: `onEnroll` solo actúa
  en `NotEnrolled` y marca `Enrolling` antes de lanzar; `Enrolling` no se apaga al terminar, lo apaga el
  `Flow` al emitir `true`. Fuera del plan: `kotlin.test` no está en el classpath de `app`, así que el
  test de «sin sesión» usa `Assert.assertThrows` de JUnit con `runTest` dentro (sin trucos); no pude
  comprobarlo por mutación (la edición de prueba fue denegada). Añadí tests de la factoría, de tocar en
  `Unknown`/ya `Enrolled`, y de inscripción externa durante `Enrolling`.

## Siguiente paso

Paso 10b: abrir un chat nuevo (Sonnet 5.5, esfuerzo medio) con el mensaje que ha preparado el orquestador.
