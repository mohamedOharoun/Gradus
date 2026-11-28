# Ficha del curso, inscripción y baja — parte B (`sharedLogic` y Room)

Índice común, Relevo general, Decisiones 1–8 y acuerdos de proceso:
`docs/tareas/ficha-e-inscripcion-curso.md`.
Contrato congelado: `backend/docs/api-progreso.md` (`DELETE /api/progress/enrollments/{courseId}`).
Referencia viva del progreso: `docs/arquitectura-progreso.md`.

## Objetivo

Dar a la app, en Kotlin puro probado (`sharedLogic`) y en la persistencia local Room (`app/`),
todo lo necesario para darse de baja en línea de un curso: confirmar primero contra el servidor
(`DELETE /api/progress/enrollments/{courseId}`), borrar localmente inscripción y módulos dominados
solo si el servidor responde `204`, tratar el `401` con `clearIfCurrent` sin borrar progreso local
y mantener `ProgressDatabase` congelada en **v3** protegiendo las operaciones concurrentes con
`@Transaction`.

## Alcance

- **B1.** `ProgressGateway.unenroll(session, courseId)` + `UnenrollOutcome`, implementado en
  `HttpProgressGateway` (Ktor) y `FakeProgressGateway`, probado con `MockEngine`.
- **B2.** `EnrollmentStore.unenroll(userId, courseId)` + transformación pura en `UserProgress` e
  implementación en `InMemoryProgressStore`.
- **B3.** Caso de uso `UnenrollFromCourse` y `UnenrollResult` (`NoSession | Finished(outcome)`).
- **B4.** Room (`ProgressDao`, `RoomProgressStore`, comentario en `MasteredModuleEntity`,
  `ProgressModule` y exposición en `AppContainer`), adaptación de posibles fakes de tests en `app/`,
  tests instrumentados de Room y actualización de `docs/arquitectura-progreso.md`.

## Fuera de alcance

- `ProgressDatabase` v4 o migraciones (`app/schemas/.../3.json` no se toca; Decisión 4).
- Pantallas, `CourseDetailViewModel`, navegación desde Buscar o textos de UI (Partes C y D).
- Cambios en el backend o en `POST /api/progress/sync` (Parte A ya cerrada).
- Borrado de notas locales con cámara (`GradusDatabase`), copias de cursos (`course_copies`) o
  entregas con foto (`submissions`) (Decisión 5).
- «Pausar curso» y baja sin conexión (*offline-first*).

## Relevo

Se **reescribe** al final de cada sesión.

- **Estado (2026-11-28):** **B1 hecho y revisado** (`ProgressGateway.unenroll`, `UnenrollOutcome`,
  `HttpProgressGateway`, `FakeProgressGateway`, 23 tests nuevos en `sharedLogic` y 5 llamadas con
  lambda final ajustadas a `answer =` en `app/src/androidTest/`; 1242 tests en `sharedLogic` y 630
  unitarios en `app` en verde, `compileDebugAndroidTestKotlin` OK).
- **Toca:** paso **B2** (`EnrollmentStore.unenroll`, `UserProgress.withoutCourse` e
  `InMemoryProgressStore`, con tests TDD en `sharedLogic`).
- **Riesgos y notas técnicas:**
  - En **B2**, añadir `unenroll(userId, courseId)` a `EnrollmentStore` hará que `app/`
    (`RoomProgressStore` y posibles fakes de tests) no compile hasta **B4**; `:sharedLogic` compila
    y pasa sus tests de forma independiente.
  - Gradle: `--rerun` a veces da `testAndroidHostTest NO-SOURCE`. Orden fiable:
    `./gradlew :sharedLogic:compileAndroidHostTest --rerun :sharedLogic:testAndroidHostTest --rerun`.
  - En **B4**, `MasteredModuleEntity` **no** puede cambiar su anotación `@ForeignKey` (alteraría
    el hash del esquema v3); solo comentario para la futura `MIGRATION_3_4`.

## Lo que ya hay y sirve de modelo

- `sharedLogic/.../course/progress/`: `ProgressGateway.kt`, `FakeProgressGateway.kt`,
  `EnrollmentStore.kt`, `InMemoryProgressStore.kt`, `UserProgress.kt`, `UserProgressSync.kt`,
  `SessionProgressSync.kt`, `SessionSyncResult.kt`.
- `sharedLogic/.../course/progress/http/`: `HttpProgressGateway.kt` (usa `callServer` de
  `account/http/HttpAccountGateway.kt`).
- `sharedLogic/src/commonTest/.../course/progress/`: `FakeProgressGatewayTest.kt`,
  `SessionProgressSyncTest.kt`, `http/HttpProgressGatewayRequestTest.kt`,
  `http/HttpProgressGatewayResponseTest.kt`, `http/ProgressMockServer.kt`.
- `app/.../data/progress/`: `ProgressDao.kt`, `RoomProgressStore.kt`, `MasteredModuleEntity.kt`,
  `ProgressDatabase.kt` (v3), `ProgressModule.kt`; y `app/.../data/AppContainer.kt`.
- Tests instrumentados existentes en `app/src/androidTest/.../data/progress/`:
  `RoomProgressTestSupport.kt`, `RoomSyncTestSupport.kt`, `RoomEnrollmentTest.kt`,
  `RoomMasteryRequiresEnrollmentTest.kt`, `RoomProgressSyncTest.kt`.

## Decisiones de la parte B (aprobadas el 2026-11-28)

- **B-1. `UnenrollResult` es espejo de `SessionSyncResult`:** `sealed interface UnenrollResult` con
  `NoSession` y `Finished(val outcome: UnenrollOutcome)`. Evita duplicar los 4 casos de
  `UnenrollOutcome` y mantiene coherencia con `SessionProgressSync`.
- **B-2. `EnrollmentStore.unenroll(userId: String, courseId: String)` recibe `userId` explícito:**
  no lee `SessionStore` (misma razón que `ProgressSyncStore`, decisión 11 de progreso). Así, si la
  sesión cambia o se cierra mientras `DELETE` viaja por la red, se borra el progreso del usuario
  original (`session.account.id`) y nunca el de otro usuario, ni lanza por falta de sesión. Es
  idempotente.
- **B-3. `UnenrollFromCourse` no captura errores del almacén ni cancelación:** solo captura errores
  de red/servidor a través de `ProgressGateway` (`UnenrollOutcome`). Con `SessionExpired` ejecuta
  `sessions.clearIfCurrent(session)` y no borra progreso local. Con `Unreachable`/`ServerFailure`
  no toca nada. Un solo intento por llamada.
- **B-4. `{courseId}` en HTTP como segmento único:** `HttpProgressGateway.unenroll` usa
  `DELETE {baseUrl}/api/progress/enrollments/{courseId}` con `encodeSlash = true` para `courseId`,
  cabecera `Authorization: Bearer <token>`, sin cuerpo, y no lee el cuerpo de la respuesta en ningún
  código (`204 → Unenrolled`, `401 → SessionExpired`, resto → `ServerFailure`).
- **B-5. Room v3 sin migración y `@Transaction` contra carreras:**
  - `ProgressDao.deleteEnrollmentWithMastered(userId, courseId)` (`@Transaction`) borra primero
    `mastered_modules` y luego `enrollments` para respetar la clave foránea sin `CASCADE` en v3.
  - `ProgressDao.markMasteredIfEnrolled(module)` (`@Transaction`) devuelve `Boolean` comprobando
    inscripción + `insertMasteredModuleIfAbsent` en la misma transacción; `RoomProgressStore` sigue
    siendo quien lanza la `IllegalStateException` del puerto cuando devuelve `false`. Como Room
    serializa los `@Transaction`, `markMastered` y `unenroll` no pueden entrelazarse.
  - Comentario en `MasteredModuleEntity` anotando `onDelete = ForeignKey.CASCADE` para la futura
    `MIGRATION_3_4` («Pausar curso»), sin tocar las anotaciones de entidad.
- **B-6. `AppContainer.kt` se conecta en B4:** en B4 se añade `val unenrollFromCourse` en
  `AppContainer` delegando en `progress.unenroll` (1 línea), de modo que la Parte C solo necesite
  tocar `ui/`.

## Tareas

La casilla la marca el orquestador de la parte tras revisar. Cada paso sigue: propuesta → OK →
tests primero (rojo → verde → refactor en lógica pura) → revisión → mensaje de commit.

### [x] B1. `ProgressGateway.unenroll`, `UnenrollOutcome`, `HttpProgressGateway` y `FakeProgressGateway`

- **Modelo y esfuerzo mínimos:** Claude Sonnet 4.5 / 4.6 (esfuerzo **bajo–medio**) · Gemini 2.5
  Flash (esfuerzo **medio**) o 2.5 Pro (esfuerzo **bajo**).
- **Ficheros:**
  - `sharedLogic/.../course/progress/ProgressGateway.kt`
  - `sharedLogic/.../course/progress/http/HttpProgressGateway.kt`
  - `sharedLogic/.../course/progress/FakeProgressGateway.kt`
  - Tests nuevos/ampliados en `sharedLogic/src/commonTest/.../course/progress/` (comprobar también
    si algún otro test en `sharedLogic/` o `app/` implementa `ProgressGateway`).
- **Criterios de aceptación y tests mínimos:**
  - `UnenrollOutcome` sellado (`Unenrolled | SessionExpired | Unreachable | ServerFailure`) y
    `when` exhaustivo sin `else`.
  - Petición HTTP (`MockEngine`): método `DELETE`, ruta `/api/progress/enrollments/{courseId}`
    (incluyendo `courseId` con `/` o caracteres especiales codificados en un solo segmento),
    cabecera `Authorization: Bearer <token>`, sin cuerpo.
  - Respuestas: `204 → Unenrolled`, `401 → SessionExpired` (sin leer cuerpo), `404 → ServerFailure`,
    `400 → ServerFailure`, `500`/`503` → `ServerFailure`, `200` inesperado → `ServerFailure`.
  - Errores: `IOException` (sin red / timeout) → `Unreachable`; otra excepción no de red →
    `ServerFailure`; `CancellationException` se relanza.
  - `FakeProgressGateway`: `unenrollAnswer` `suspend` (por defecto `Unenrolled`), `unenrollCalls`
    con `UnenrollCall(userId, courseId)` (sin token), registrado antes de suspender.
  - `./gradlew :sharedLogic:compileAndroidHostTest --rerun :sharedLogic:testAndroidHostTest --rerun`
    en verde.

### [ ] B2. `EnrollmentStore.unenroll` e `InMemoryProgressStore`

- **Modelo y esfuerzo mínimos:** Claude Sonnet 4.5 / 4.6 (esfuerzo **bajo–medio**) · Gemini 2.5
  Flash (esfuerzo **medio**) o 2.5 Pro (esfuerzo **bajo**).
- **Ficheros:**
  - `sharedLogic/.../course/progress/EnrollmentStore.kt`
  - `sharedLogic/.../course/progress/UserProgress.kt`
  - `sharedLogic/.../course/progress/InMemoryProgressStore.kt`
  - Tests en `sharedLogic/src/commonTest/.../course/progress/`.
- **Criterios de aceptación y tests mínimos:**
  - `UserProgress.withoutCourse(courseId)` pura e `InMemoryProgressStore.unenroll(userId, courseId)`
    atómica con `progress.update`.
  - Borra inscripción, todos los módulos dominados y pendientes (`pending(userId)` queda sin ese
    curso).
  - Idempotente: dar de baja sin estar inscrito o dos veces seguidas no lanza ni modifica nada.
  - Aislamiento: no afecta a otros cursos del usuario ni al mismo curso de otro usuario.
  - Funciona sin sesión o con otro usuario en `SessionStore` (actúa sobre `userId`).
  - Los `Flow` activos (`enrolled(courseId)` y `mastered(courseId)`) emiten `false` y `emptySet()`,
    sin emitir duplicados a oyentes de otros cursos.
  - Reinscripción limpia tras baja (nueva fecha, `pending = true`, 0 dominados) y `markMastered`
    tras baja lanza `IllegalStateException` sin dejar nada escrito.
  - `./gradlew :sharedLogic:compileAndroidHostTest --rerun :sharedLogic:testAndroidHostTest --rerun`
    en verde.

### [ ] B3. Caso de uso `UnenrollFromCourse` y `UnenrollResult`

- **Modelo y esfuerzo mínimos:** Claude Sonnet 4.5 / 4.6 (esfuerzo **medio**) · Gemini 2.5 Pro
  (esfuerzo **medio**).
- **Ficheros:**
  - `sharedLogic/.../course/progress/UnenrollFromCourse.kt`
  - `sharedLogic/.../course/progress/UnenrollResult.kt`
  - Tests en `sharedLogic/src/commonTest/.../course/progress/UnenrollFromCourseTest.kt`.
- **Criterios de aceptación y tests mínimos:**
  - Sin sesión: devuelve `UnenrollResult.NoSession`, sin llamar al gateway ni al almacén.
  - `Unenrolled`: llama al gateway con `(session, courseId)`, borra `(session.account.id, courseId)`
    en `EnrollmentStore` y devuelve `Finished(Unenrolled)`.
  - `Unreachable` y `ServerFailure`: no borra nada ni toca la sesión; un solo intento.
  - `SessionExpired`: llama a `sessions.clearIfCurrent(session)`, no borra el progreso local y no
    reintenta.
  - Concurrencia con `CompletableDeferred` en `FakeProgressGateway`:
    - si la sesión cambia mientras viaja el `DELETE` y llega `SessionExpired`, la nueva sesión no
      se borra;
    - si la sesión cambia a otro usuario (o se cierra) mientras viaja y llega `Unenrolled`, se
      borra el curso del usuario original y nunca el del nuevo usuario.
  - Excepciones del almacén y `CancellationException` se propagan.
  - `when` exhaustivo sobre `UnenrollResult`.
  - `./gradlew :sharedLogic:compileAndroidHostTest --rerun :sharedLogic:testAndroidHostTest --rerun`
    en verde.

### [ ] B4. Room (`ProgressDao`, `RoomProgressStore`, `MasteredModuleEntity`, `ProgressModule`, `AppContainer`) y referencia viva

- **Modelo y esfuerzo mínimos:** Claude Sonnet 4.5 / 4.6 (esfuerzo **medio–alto**) · Gemini 2.5 Pro
  (esfuerzo **medio**).
- **Ficheros:**
  - `app/.../data/progress/ProgressDao.kt`
  - `app/.../data/progress/RoomProgressStore.kt`
  - `app/.../data/progress/MasteredModuleEntity.kt` (solo comentario para `MIGRATION_3_4`)
  - `app/.../data/progress/ProgressModule.kt`
  - `app/.../data/AppContainer.kt` (exponer `unenrollFromCourse`)
  - Fakes de `EnrollmentStore` en `app/src/test` o `app/src/androidTest` si hiciera falta compilar
  - Tests instrumentados en `app/src/androidTest/.../data/progress/`
  - `docs/arquitectura-progreso.md` (actualizar piezas, flujo e invariantes)
- **Criterios de aceptación y tests mínimos:**
  - `ProgressDatabase` sigue en **v3** y `app/schemas/.../3.json` **sin cambios**.
  - `ProgressDao.deleteEnrollmentWithMastered` (`@Transaction`) borra `mastered_modules` y luego
    `enrollments`; `ProgressDao.markMasteredIfEnrolled` (`@Transaction`) protege `markMastered`.
  - Tests instrumentados de Room (base en memoria):
    - borra inscripción y todos los `mastered_modules` (pendientes y sincronizados) de
      `(userId, courseId)`;
    - no afecta a otros cursos, otros usuarios ni `course_copies`;
    - idempotente (sin inscripción o dos veces);
    - funciona sin sesión o con otra sesión activa;
    - `watchEnrolled` y `watchMasteredIds` emiten `false` y `emptySet()` (con `Channel`, sin
      `delay`) y no avisan a otros cursos;
    - `pending(userId)` deja de incluir el curso y sus módulos pendientes;
    - reinscripción limpia tras baja;
    - `markMastered` tras baja lanza `IllegalStateException` y no deja filas;
    - `ProgressModule.unenroll` usa el mismo `RoomProgressStore` que `enrollmentStore` y
      `progressStore` (confirmado con `FakeProgressGateway` y base en memoria).
  - `./gradlew :app:testDebugUnitTest` y tests instrumentados de `data.progress` en verde.

## Progreso

- 2026-11-28: documento creado por el orquestador B tras aprobar el plan B1–B4 y las decisiones
  B-1 a B-6 (`UnenrollResult` como `NoSession | Finished(outcome)`, `AppContainer.unenrollFromCourse`
  en B4).
- 2026-11-28: **B1 hecho y revisado.** Implementados `ProgressGateway.unenroll`, `UnenrollOutcome`,
  `HttpProgressGateway.unenroll` (`encodeSlash = true`, sin leer cuerpo) y `FakeProgressGateway`
  (`unenrollAnswer`, `UnenrollCall` sin token), con 16 tests en `HttpProgressGatewayUnenrollTest` y
  7 en `FakeProgressGatewayUnenrollTest`; ajustadas 5 llamadas con lambda final en
  `app/src/androidTest/` (`ProgressModuleTest`, `ProgressSyncWorkerTest`, `WorkSyncRequesterTest`).

## Siguiente paso

Ejecutar el paso **B2** con su implementador (`EnrollmentStore.unenroll`,
`UserProgress.withoutCourse` e `InMemoryProgressStore`).
