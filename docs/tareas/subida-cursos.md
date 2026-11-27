# Subida de cursos (pestaña «Mis cursos»)

Funcionalidad completa de la pestaña **Mis cursos**: listar los cursos creados por el usuario,
subir un fichero JSON de curso (nuevo o nueva versión), validarlo contra el servidor, revisar
errores/avisos, publicarlo y gestionar borradores locales incluso sin conexión.

Referencia visual: `assets/mockups/my-courses.png` (Figma «5 · Mis cursos»), con los ajustes
acordados más abajo. Contrato de cursos del backend: `backend/docs/api-cursos.md`.

Estado: **en progreso (2026-11-27)** — Parte A (Backend: 1a–1b), Parte B (`sharedLogic/`: 2a–2c) y Parte C (`app/data/`: 3a–3b) cerradas; siguiente: Parte D (`app/ui/`: Paso 4a).

## Relevo

Se **reescribe** al final de cada sesión; no se acumula.

- **Dónde estamos (2026-11-27):** **Parte A (Backend)** cerrada (932 tests JUnit Jupiter), **Parte B
  (`sharedLogic/`)** cerrada (1219 tests en `:sharedLogic:testAndroidHostTest`, 100 % KMP puro) y
  **Parte C (`app/data/`) cerrada y revisada** (pasos 3a-1, 3a-2 y 3b en verde: 34 tests JVM nuevos,
  591 tests totales en `:app:testDebugUnitTest`; 67/67 tests instrumentados en verde en
  `emulator-5554` cubriendo `CourseDraftFilesAndroidTest`, `ProgressDatabaseMigrationTest`,
  `ProgressDatabaseTest`, `RoomCourseDraftStoreTest`, `CourseUploadWorkerTest`,
  `WorkUploadRequesterTest` y `AppContainerTest`; 0 errores ni avisos de compilación).
- **Contratos y cableado listos para la Parte D (`app/ui/`):**
  - **Modelos y utilidades puras (`es.ulpgc.pamn.dominio.course.publication`):**
    - `MAX_COURSE_BYTES = 2_097_152`, `CourseIssue`, `CourseReport` (`isValid`, `hasWarnings`,
      `EMPTY`, `formatAsPlainText(labels: ReportTextLabels)`), `ReportTextLabels`.
    - `PendingAction` (`VALIDATING`, `PUBLISHING`), `PendingNotice` (`NONE`, `NETWORK`,
      `SESSION_EXPIRED`, `SERVER_FAILURE`), `DraftRejection` (`ValidationReport`,
      `IdenticalToLatest`, `NotAuthor`, `CourseNotFound`, `TooLarge`, `FileMissing`), `DraftStatus`
      (`Pending`, `Accepted`, `Rejected`).
    - `CourseDraft` (`id`, `userId`, `targetCourseId`, `filePath`, `title`, `moduleCount`, `status`,
      `updatedAt: kotlin.time.Instant`, `isNewVersion`).
    - `AuthorCourseFilter` (`ALL`, `PUBLISHED`, `ACCEPTED`, `PENDING`, `REJECTED`),
      `AuthorCourseItem`, `mergeAuthorCourses(published, drafts)`, `filterBy(filter)`,
      `countByFilter()`, `CourseDraftPreview` (`fromBytes`, `fromJson`).
  - **Puertos de `app/data/publication/` y cableado en `AppContainer` (listos para `MyCoursesViewModel` y navegación):**
    - `AppContainer.coursePublicationGateway: CoursePublicationGateway` (`myCourses`, `validateNew`,
      `validateVersion`, `publishNew`, `publishVersion`).
    - `AppContainer.courseDraftStore: CourseDraftStore` (`RoomCourseDraftStore` sobre
      `ProgressDatabase` v3, `drafts(): Flow<List<CourseDraft>>` por usuario actual, `find`,
      `upsert`, `updateIfPresent`, `delete`, `clearForUser`).
    - `AppContainer.courseDraftFiles: CourseDraftStorage` (`CourseDraftFiles.android(context)`:
      `import(draftId, sourceUri): DraftImportResult` — `Imported(filePath, preview)` / `TooLarge` /
      `Unreadable` — con corte exacto a 2 MiB y protección *path traversal*, más `read`, `delete`,
      `deleteAll`).
    - `AppContainer.courseUploadRequester: CourseUploadRequester` (`WorkUploadRequester`:
      `enqueue(draftId)` con trabajo único `"course-upload-$draftId"`, `REPLACE`,
      `NetworkType.CONNECTED` y *backoff* exponencial; `cancel(draftId)`).
    - `AppContainer.processCourseDraft: ProcessCourseDraft` (usado por `CourseUploadWorker`,
      invalida automáticamente `AppContainer.courseGateway` al publicar una versión).
    - `AppContainer.clearUserDrafts: ClearUserDrafts` (`UserDraftsCleaner` ya cableado en
      `SettingsDestination.kt` → `DeleteAccountViewModel` para limpiar filas, ficheros y trabajos
      al eliminar cuenta).
- **Siguiente paso:** **Parte D · Paso 4a** (recursos vectoriales, aprobación explícita previa de
  tokens de color para badges «Publicado» y «Rechazado» en `Color.kt`/`Theme.kt`, y textos ES/EN en
  `values/strings.xml` y `values-en/strings.xml`).
- **Selección de modelos y esfuerzo:** ahora también podemos usar **Gemini Neon** en niveles
  **Low**, **Medium**, **High** y **Extra** (además de Sonnet/Opus si se prefieren). Criterio
  general (el mínimo que mantiene la calidad):
  - **Low:** tareas mecánicas, documentación, textos, pantallas provisionales o cableado trivial.
  - **Medium:** lógica pura estándar, DTOs/mappers, consultas SQL sencillas, componentes Compose sin
    estado y ViewModels directos.
  - **High:** contratos HTTP de validación/publicación, migraciones de Room, WorkManager/concurrencia,
    ViewModels con estado asíncrono complejo y revisiones a fondo.
  - **Extra:** solo si aparece un problema difícil de concurrencia, migración o depuración que lo
    justifique.
- **Acuerdos de proceso:**
  - Que un paso esté detallado aquí **no** autoriza a implementarlo directamente: quien implementa
    propone primero ficheros, diseño y tests, y espera el OK (`AGENTS.md`).
  - Lógica pura (`sharedLogic` y `backend`): tests primero (rojo → verde → refactor).
  - La casilla (`[x]`) la marca quien revisa; quien implementa deja «pendiente de revisión» en
    «Progreso».
  - Sin permiso previo, no se escribe en la base de desarrollo ni se modifica `Color.kt`/`Theme.kt`
    ni los ficheros de Gradle.
  - Nombres de tests en `sharedLogic` sin comas ni paréntesis (compatibilidad iOS); en
    `androidTest`, con `_`.
  - No hacer commits, push ni cambios de rama: los hacen los usuarios.

---

## Objetivo

1. Permitir al usuario elegir un fichero `.json` desde el móvil para subir un curso nuevo o una
   nueva versión de un curso propio.
2. Validar el curso contra el servidor en un primer paso (mostrando errores y avisos, incluido
   `REMOVED_IDS` antes de publicar una versión nueva) y publicarlo en un segundo paso.
3. Persistir los borradores locales por cuenta en el móvil y completar validaciones/publicaciones
   en segundo plano con WorkManager cuando vuelva la conexión.
4. Listar en «Mis cursos» los cursos publicados y los borradores del usuario, con filtro por estado
   y orden cronológico (más reciente arriba).

## Alcance

- **Backend (`backend/`):**
  - `GET /api/users/me/courses` (con sesión): devuelve `{"courses": [...]}` con los cursos publicados
    por el usuario autenticado (última versión de cada curso, ordenados por `published_at DESC, id`).
  - `POST /api/courses/{id}/versions/validate` (con sesión): valida una nueva versión sin guardarla
    (404 curso inexistente / id no UUID, 403 no es autor, 413 > 2 MiB, 200 con informe `valid: false`
    si hay errores de sintaxis/esquema/semántica —igual que `POST /api/courses/validate`—, 409
    `IDENTICAL_TO_LATEST` si es válido e idéntico a la última versión, y 200 con informe `valid: true`
    incluyendo `REMOVED_IDS`).
  - Actualizar `backend/docs/api-cursos.md` y peticiones en Yaak.
- **Lógica compartida (`sharedLogic/`):**
  - Puerto `CoursePublicationGateway`, adaptador HTTP con Ktor (timeout 60 s y control previo de
    2 MiB) y `FakeCoursePublicationGateway`.
  - Modelos de borrador (`CourseDraft`), informe (`CourseReport`, `CourseIssue`), resultados
    sellados, combinación de cursos publicados + borradores (una sola tarjeta por curso), filtro por
    estado, contadores y orden cronológico.
  - Puerto de almacenamiento de borradores (`CourseDraftStore` + implementación en memoria para
    tests) y política pura de reintento en segundo plano.
  - Invalidación de caché en `CachingCourseGateway` al publicar una nueva versión.
- **App Android (`app/`):**
  - Almacenamiento local: JSON del borrador como fichero en carpeta privada (`files/course_drafts/`)
    y metadatos/estado en Room (`ProgressDatabase`, migración v2 → v3 con esquema congelado y
    borrado por cuenta al eliminar cuenta).
  - Lector de `.json` desde `Uri` (`ContentResolver` + `ActivityResultContracts.OpenDocument`, sin
    permisos nuevos), validando tamaño máximo de 2 MiB al leer.
  - Trabajo en segundo plano con WorkManager (`CourseUploadWorker` + `WorkUploadRequester`) con
    restricción de red (`NetworkType.CONNECTED`).
  - Pantalla `ScreenMyCourses` completa, cabecera «Subir curso», enlace a pantalla provisional de la
    guía de IA (solo permite volver atrás), selector desplegable de filtro con contadores, tarjetas
    por estado, botón papelera con confirmación para descartar borradores, hoja inferior de
    «Ver motivos» / «Ver avisos (N)» con «Copiar informe» y aviso inline ante problemas de red o
    sesión caducada.
  - Textos en español e inglés (`values/strings.xml` y `values-en/strings.xml`) y `@Preview` en
    claro y oscuro.
- **Documentación:** actualizar `docs/especificacion.md` §5 («Mis cursos») al cierre.

## Fuera de alcance

- Métricas de estudiantes, valoraciones y opiniones («48 estudiantes · ★ 4,6», «Ver opiniones»).
  El botón «Ver opiniones» se oculta y la línea de métricas queda preparada como componente opcional
  que no se dibuja sin datos (y `ignoreUnknownKeys` permite añadir campos en el servidor sin romper
  la app).
- Contenido real de la «Guía para crear cursos con IA» (queda enlazada a una pantalla temporal con
  botón «Volver»).
- Moderación o revisión manual por administradores (en Gradus la revisión es automática por el
  validador del servidor).
- Borrar cursos ya publicados (fuera de alcance en el backend).
- Notificaciones del sistema (`POST_NOTIFICATIONS`) cuando falle la red o termine un trabajo en
  segundo plano.
- Dependencias nuevas (Ktor, Room, WorkManager, Compose y `ActivityResultContracts.OpenDocument` ya
  están en el proyecto).

---

## Mockup frente a implementación

| En `my-courses.png` | En esta tarea | Motivo |
|---|---|---|
| «conceptos» | «módulos» | Unificación terminológica de la app y especificación |
| Estado «En revisión» | Estado «Pendiente» | Refleja espera de red/envío en segundo plano («Se validará…» / «Se publicará…») |
| «48 estudiantes · ★ 4,6» y «Ver opiniones» | Ocultos por ahora | Aún no existen valoraciones ni métricas en la API; se prepara el hueco sin riesgo |
| «Ver la guía para crear cursos con IA >» | Pantalla provisional con «Volver» | La guía se redactará en otra tarea |
| Solo contador «TUS CURSOS · 4» | «TUS CURSOS» + selector desplegable «Todos · 4 ▾» | Permite filtrar por estado (Todos, Publicados, Aceptados, Pendientes, Rechazados) |
| Sin acción de borrar borrador | Icono papelera en esquina superior de tarjetas con borrador | Permite descartar borradores sin esconderlo dentro de «Ver motivos» |
| Sin acceso a avisos en «Aceptado» | Enlace «Ver avisos (N)» si el informe válido trae avisos | Permite revisar `REMOVED_IDS` u otros avisos antes de pulsar «Publicar» |

---

## Decisiones (aprobadas el 2026-11-27)

- **A · Subida en dos pasos.** «Subir curso» / «Subir nueva versión» valida primero
  (`POST /api/courses/validate` o `POST /api/courses/{id}/versions/validate`). Si no hay errores,
  pasa a **Aceptado** y habilita «Publicar» (`POST /api/courses` o `POST /api/courses/{id}/versions`).
  Si hay errores, pasa a **Rechazado** con «Ver motivos».
- **B · Dos rutas nuevas en el backend.**
  1. `GET /api/users/me/courses` (con sesión) en lugar de `/api/courses/mine`, para no colisionar con
     `GET /api/courses/{id}` y seguir el estilo de `/api/users/me`.
  2. `POST /api/courses/{id}/versions/validate` (con sesión) para validar versiones nuevas y recibir
     `REMOVED_IDS` y `409 IDENTICAL_TO_LATEST` **antes** de publicar, manteniendo el validador del
     backend como única fuente de verdad.
- **C · Estado «Pendiente» en segundo plano.** Cuando el usuario sube o pulsa «Publicar», el
  borrador entra en **Pendiente** distinguiendo la operación (`VALIDATING` / `PUBLISHING`): el badge
  dice «Pendiente» y el texto aclara «Se validará cuando haya conexión» o «Se publicará cuando haya
  conexión». Lo ejecuta WorkManager con `NetworkType.CONNECTED`. Sin red o ante fallos de red se
  muestra un aviso dentro de «Mis cursos» (sin notificaciones del sistema). Timeout HTTP de 60 s.
- **D · Persistencia de borradores.** Fichero JSON en carpeta privada (`files/course_drafts/`),
  nunca dentro de SQLite (evita `SQLiteBlobTooBigException` cerca de 2 MiB). En Room
  (`ProgressDatabase`, migración v2 → v3) se guardan solo ruta, metadatos mínimos (título extraído
  o nombre de fichero, nº de módulos si se pudo leer), estado, operación pendiente, informe
  serializado y fecha de última actualización. Se guardan por `user_id`: sobreviven al cerrar sesión
  (solo los ve su dueño) y se eliminan (filas, ficheros y trabajos pendientes) al eliminar la cuenta
  (`DeleteAccountViewModel`).
- **E · Una sola tarjeta por curso.** Si un curso ya publicado tiene un borrador de nueva versión
  (Pendiente, Aceptado o Rechazado), se muestra una sola tarjeta con el estado de la versión nueva y
  una línea «Publicada: versión N».
- **F · Descartar borradores.** Papelera directa (`ic_delete.xml`, color `onSurfaceVariant`, 48 dp,
  `contentDescription` con el título) en la fila superior de la tarjeta (título · badge · papelera),
  solo cuando hay borrador. Pide confirmación en un diálogo («Descartar borrador» / «Descartar
  versión nueva»), cancela su trabajo de WorkManager y borra fila y fichero. *Caso límite:* si el
  worker ya estaba transmitiendo una publicación al pulsar descartar, el servidor puede llegar a
  publicar el curso; al refrescar aparecerá como Publicado sin duplicar tarjetas.
- **G · Hoja de motivos y avisos.** «Ver motivos» (en Rechazado) y «Ver avisos (N)» (en Aceptado con
  avisos) abren la misma `ModalBottomSheet` con la lista de errores y avisos (mensaje en español y
  `path` JSON Pointer) más un botón «Copiar informe» al portapapeles para pegarlo en un LLM.
- **H · Filtro y orden.** Cabecera «TUS CURSOS» a la izquierda y botón desplegable `Todos · N ▾` a
  la derecha (`DropdownMenu` con Todos, Publicados, Aceptados, Pendientes, Rechazados y sus
  contadores, mostrando también los de 0). Orden cronológico inverso (más reciente arriba) por
  última actividad (`updatedAt` del borrador o `publishedAt` del publicado; si un curso publicado
  tiene borrador activo, cuenta `updatedAt`). El filtro vive en el ViewModel (se mantiene entre
  pestañas, se reinicia al cerrar la app). Sin ningún curso no se enseña el selector.
- **I · Colores del tema.** Los badges «Publicado» (verde) y «Rechazado» (contenedor rojo suave)
  necesitan tokens nuevos en `Color.kt` (y su equivalente oscuro en `Theme.kt`): se pedirá OK
  explícito antes de tocar el tema (`AGENTS.md` §9).

---

## Plan de tareas

### Parte A · Backend (`backend/`, Java 25, JUnit Jupiter)

- [x] **1a · Listar cursos propios (`GET /api/users/me/courses`).**
  *Modelo sugerido:* Gemini Neon · **Medium**.
  - `CourseRepository.findByAuthor(UUID authorId)` (devuelve `List<CourseSummary>`, últimas
    versiones ordenadas por `published_at DESC, c.id`), `JdbcCourseRepository` (reutilizando
    `CourseRows.FROM_LATEST` e índice `courses_author_id_idx`), `InMemoryCourseRepository`, caso de
    uso `ListMyCourses`, `CourseListResponse` (`{"courses": [...]}`), `MyCoursesController` y ruta
    protegida en `App.java`.
  - *Acepta si:* tests unitarios del caso de uso y repositorio en memoria (autor con varios cursos,
    orden por fecha y desempate por id, autor sin cursos, no devuelve cursos de otros) y `mvn test`
    en verde; comprobación manual contra la base de desarrollo (con permiso) dando 200 y 401.

- [x] **1b · Validar nueva versión (`POST /api/courses/{id}/versions/validate`).**
  *Modelo sugerido:* Gemini Neon · **High**.
  - Caso de uso `ValidateCourseVersion`: mismo orden de comprobación inicial que
    `PublishCourseVersion` (404 curso inexistente / id no UUID, 403 no es autor, 413 > 2 MiB,
    lectura + validación con 200 `valid: false` si hay errores —igual que `POST /api/courses/validate`—,
    409 `IDENTICAL_TO_LATEST` si es válido e idéntico, y 200 con informe incluyendo `REMOVED_IDS`
    comparando con la última versión sin guardar nada).
  - Controlador (`CoursePublicationController`), ruta protegida en `App.java`, actualización de
    `backend/docs/api-cursos.md` (los dos endpoints nuevos) y peticiones en Yaak.
  - *Acepta si:* tests unitarios para todos los desenlaces (válido sin avisos, válido con
    `REMOVED_IDS`, errores de sintaxis/esquema/semántica, > 2 MiB, 403, 404, 409 idéntico) y
    `mvn test` en verde; comprobación manual con `curl`/Yaak (con permiso).

### Parte B · Lógica compartida (`sharedLogic/`, Kotlin puro, test primero)

- [x] **2a · Modelos de publicación, borrador e informe.**
  *Modelo sugerido:* Gemini Neon · **Medium**.
  - En `course/publication/` (o `course/author/`): `CourseIssue`, `CourseReport` (con formateador
    de texto plano para «Copiar informe»), `DraftStatus` (`Pending(PendingAction)`, `Accepted`,
    `Rejected`), `CourseDraft`, `AuthorCourseItem` (fusión de curso publicado + borrador opcional en
    una sola tarjeta), `AuthorCourseFilter` (`ALL`, `PUBLISHED`, `ACCEPTED`, `PENDING`, `REJECTED`),
    función pura de fusión/filtrado/contadores/orden cronológico y lectura ligera de metadatos de
    vista previa (título y nº de módulos desde el JSON del fichero si es parseable, con *fallback* al
    nombre del fichero).
  - *Acepta si:* tests en `commonTest` cubren fusión (publicado solo, borrador nuevo, publicado +
    borrador nueva versión), orden cronológico y desempates, contadores por filtro (incluidos ceros),
    formateo del informe y extracción tolerante de título/módulos en JSON válido y roto.

- [x] **2b · Puerto `CoursePublicationGateway` y adaptador HTTP (Ktor).**
  *Modelo sugerido:* Gemini Neon · **High**.
  - Puerto con `myCourses(session)`, `validateNew(session, bytes)`, `validateVersion(session,
    courseId, bytes)`, `publishNew(session, bytes)` y `publishVersion(session, courseId, bytes)`,
    con resultados sellados (incluyendo `SessionExpired`, `Unreachable`, `ServerFailure`, `TooLarge`,
    `IdenticalToLatest`, `NotAuthor`, `NotFound`).
  - `HttpCoursePublicationGateway` (timeout 60 s, comprobación local de 2 MiB antes de enviar,
    `Bearer` token, DTOs con `ignoreUnknownKeys` y hueco limpio para futuras métricas opcionales),
    `CoursePublicationGateways` (factory) y `FakeCoursePublicationGateway`.
  - Invalidar entrada en `CachingCourseGateway` (`evict(id)` o `invalidate(id)`) para que abrir un
    curso recién actualizado descargue la versión nueva.
  - *Acepta si:* tests con `MockEngine` cubren todos los códigos HTTP (200, 201, 400, 401, 403, 404,
    409, 413, 422, 500 y fallo de red), control previo de > 2 MiB sin abrir socket y que el token no
    se filtre; tests de `CachingCourseGateway`.

- [x] **2c · Puerto `CourseDraftStore`, coordinador de subida y política de reintento.**
  *Modelo sugerido:* Gemini Neon · **High**.
  - Puerto `CourseDraftStore` (por usuario: `drafts(): Flow<List<CourseDraft>>`, `find(draftId)`,
    `upsert`, `delete(draftId)`, `clearForUser(userId)`) + `InMemoryCourseDraftStore`.
  - Caso de uso/coordinador puro `ProcessCourseDraft`: dado un `draftId`, lee borrador + bytes +
    sesión, ejecuta la operación pendiente (`VALIDATING` o `PUBLISHING`), actualiza el estado en el
    store (o borra el borrador e invalida caché si se publicó), y devuelve decisión para WorkManager
    (`Done | Retry | GiveUp`).
  - Casos límite probados: borrador descartado antes de empezar (no envía), borrador descartado
    mientras se enviaba (no resucita la fila al recibir respuesta), 401 deja en Pendiente con aviso
    de sesión, 409/403/404 pasan a Rechazado con mensaje claro, red caída reintenta.
  - *Acepta si:* tests en `commonTest` pasan con `./gradlew :sharedLogic:testAndroidHostTest` sin
    fallos ni avisos.

### Parte C · Persistencia y segundo plano (`app/data/`)

- [x] **3a · Ficheros de borrador, lector `OpenDocument` y migración Room v2 → v3.**
  *Modelo sugerido:* Gemini Neon · **High**.
  - `CourseDraftFiles`: copia desde `Uri` (`ContentResolver`) a `files/course_drafts/<id>.json`
    cortando la lectura si supera 2 MiB (sin cargar archivos enormes en memoria), lee bytes y borra
    ficheros individuales o por lista.
  - `CourseDraftEntity`, `CourseDraftDao`, `RoomCourseDraftStore` (serializando el `CourseReport`
    como JSON en columna `TEXT`) y `MIGRATION_2_3` en `ProgressDatabase` (version 3, nuevo esquema
    exportado en `app/schemas/.../3.json`, sin borrado destructivo).
  - Limpieza al eliminar cuenta: `DeleteAccountViewModel` borra los borradores locales del usuario
    (filas, ficheros y trabajos pendientes) al confirmarse `Deleted`.
  - *Acepta si:* tests instrumentados de Room (CRUD por usuario, aislamiento entre cuentas,
    migración real v2 → v3 preservando inscripciones/progreso/copias) y tests unitarios de
    `CourseDraftFiles` (límite de 2 MiB, lectura y borrado).

- [x] **3b · WorkManager (`CourseUploadWorker` y `WorkUploadRequester`).**
  *Modelo sugerido:* Gemini Neon · **High**.
  - `CourseUploadRequester` (puerto: `enqueue(draftId)`, `cancel(draftId)`) + `WorkUploadRequester`
    usando `enqueueUniqueWork("course-upload-$draftId", REPLACE, …)` con `NetworkType.CONNECTED` y
    *backoff* exponencial.
  - `CourseUploadWorker` (`CoroutineWorker` con `WorkerFactory` de test, igual que
    `ProgressSyncWorker`).
  - Cableado en `AppContainer`.
  - *Acepta si:* tests con `TestListenableWorkerBuilder` / `WorkManagerTestInitHelper` verifican
    éxito, reintento en fallo de red, fallo definitivo, cancelación al descartar y `enqueueUniqueWork`
    sin duplicados.

### Parte D · Interfaz y navegación (`app/ui/`)

- [ ] **4a · Recursos, iconos, colores (con OK) y textos ES/EN.**
  *Modelo sugerido:* Gemini Neon · **Low**.
  - Icono vectorial `ic_upload.xml` (y los pequeños necesarios para avisos/reloj/desplegable).
  - Previa aprobación: tokens de color para badge «Publicado» y contenedor «Rechazado» en `Color.kt`
    y `Theme.kt` (claro y oscuro).
  - Todos los textos de «Mis cursos», selector, diálogo de descartar, hoja de motivos/avisos,
    errores y guía temporal en `values/strings.xml` y `values-en/strings.xml`.
  - *Acepta si:* compila sin errores y todas las claves existen en ambos idiomas.

- [ ] **4b · Componentes sin estado de «Mis cursos» y previews.**
  *Modelo sugerido:* Gemini Neon · **Medium**.
  - En `ui/screens/mycourses/`:
    - Cabecera con «Subir curso» y enlace «Ver la guía para crear cursos con IA >».
    - Cabecera de lista «TUS CURSOS» + selector desplegable `DropdownMenu` con contadores.
    - `AuthorCourseCard`: título, badge de estado (Publicado, Aceptado, Pendiente, Rechazado),
      papelera en borradores (48 dp, `onSurfaceVariant`), línea de métricas opcional (oculta cuando
      es `null`), nº de módulos, subtítulo/aviso por estado («Publicada: versión N», «Se validará…»,
      «Se publicará…», caja de error en Rechazado) y fila de acciones («Subir nueva versión»,
      «Publicar», «Ver avisos (N)», «Ver motivos»).
    - `CourseReportSheet` (`ModalBottomSheet` con errores, avisos y «Copiar informe») y diálogo de
      confirmación de descarte.
    - `ScreenMyCourses` sin estado + `ScreenMyCoursesPreviews` (todos los estados, vacío, filtro sin
      resultados, aviso de red, claro y oscuro).
  - *Acepta si:* previews visibles y limpias en claro y oscuro, zonas táctiles ≥ 48 dp y
    `contentDescription` en iconos con significado; tests de UI en emulador para estados, selector,
    papelera y hoja de informe.

- [ ] **4c · `MyCoursesViewModel`, pantalla provisional de guía y navegación.**
  *Modelo sugerido:* Gemini Neon · **High**.
  - `MyCoursesViewModel`: carga `myCourses` del servidor, combina con el `Flow` de borradores de
    Room, gestiona el filtro seleccionado, lanza validación/publicación tras elegir fichero,
    descarta borradores, maneja recarga (p. ej. al volver o tras publicarse un trabajo en segundo
    plano) y expone avisos inline de red/sesión.
  - `Route.CourseGuide` (pantalla provisional con `PlaceholderScreen` y «Volver») y destino de
    `MyCourses` en `ui/navigation/` con `ActivityResultContracts.OpenDocument` (`arrayOf("application/json", "text/plain", "*/*")`, filtrando/validando el contenido sin bloquear a dispositivos que no reconocen el MIME `.json`).
  - *Acepta si:* tests JVM de `MyCoursesViewModel` (carga, filtro, subir, nueva versión, publicar,
    descartar, sin conexión, 401) y prueba en el emulador en claro y oscuro.

### Parte E · Cierre

- [ ] **5 · Repaso final, prueba de punta a punta y documentación.**
  *Modelo sugerido:* Gemini Neon · **Low** (revisión final **Medium** si hace falta).
  - Prueba manual completa en emulador contra backend local: subir JSON inválido → Rechazado → Ver
    motivos → Copiar informe → subir JSON válido → Aceptado (con y sin avisos) → Publicar →
    Publicado → subir nueva versión con id eliminado → aviso `REMOVED_IDS` antes de publicar →
    sin red (modo avión) → Pendiente → recuperar red → completa solo → descartar borrador → filtro
    por estados → guía temporal y volver → claro, oscuro, inglés y TalkBack.
  - Batería completa de tests (`backend`, `sharedLogic`, `app` JVM e instrumentados).
  - Actualizar `docs/especificacion.md` §5 («Mis cursos») y este documento.
  - *Acepta si:* todo compila sin avisos nuevos, todas las suites pasan y las comprobaciones
    manuales están verificadas.

---

## Progreso

- 2026-11-27: documento creado con todas las decisiones aprobadas (A–I) y plan dividido en pasos
  1a–5 con modelo sugerido.
- 2026-11-27: Paso 1a (`GET /api/users/me/courses`) completado y revisado (`ListMyCourses`,
  `JdbcCourseRepository.findByAuthor`, `CourseListResponse`, `MyCoursesController`, ruta protegida
  en `App.java`; 919 tests en verde, 0 avisos).
- 2026-11-27: Paso 1b (`POST /api/courses/{id}/versions/validate`) completado y revisado
  (`ValidateCourseVersion`, `VersionValidationResult`, `RemovedIdsCheck` compartido con
  `PublishCourseVersion`, `CoursePublicationController.validateVersion`, ruta en `App.java`,
  contrato congelado en `backend/docs/api-cursos.md` y 7 peticiones en `backend/yaak/workspace/`;
  932 tests en verde, 0 avisos). **Parte A cerrada.**
- 2026-11-27: Paso 2a (`sharedLogic/`, paquete `course.publication`) completado y revisado
  (`CourseIssue`, `CourseReport`, `ReportTextLabels`, `PendingAction`, `PendingNotice`,
  `DraftRejection`, `DraftStatus`, `MAX_COURSE_BYTES`, `CourseDraft`, `AuthorCourseFilter`,
  `AuthorCourseItem`, `mergeAuthorCourses`, `filterBy`, `countByFilter` y `CourseDraftPreview`;
  45 tests nuevos en `commonTest`, 1115 tests totales en verde, 0 avisos).
- 2026-11-27: Paso 2b (`sharedLogic/`, puerto `CoursePublicationGateway`, adaptador HTTP con Ktor,
  `CourseReportJson`, `CoursePublicationGateways`, `FakeCoursePublicationGateway`,
  `CourseCacheInvalidator` e invalidación en `CachingCourseGateway`) completado y revisado (54 tests
  nuevos en `commonTest`, 1169 tests totales en verde, 0 avisos).
- 2026-11-27: Paso 2c (`sharedLogic/`, puertos `CourseDraftStore` y `CourseDraftContentStore` +
  `InMemory*`, política `DraftWorkDecision` y coordinador puro `ProcessCourseDraft`) completado y
  revisado (50 tests nuevos en `commonTest`, 1219 tests totales en verde, 0 avisos). **Parte B
  cerrada.**
- 2026-11-27: Subpaso 3a-1 (`app/data/publication/`: `CourseDraftStorage`, `CourseDraftFiles`,
  `ClearUserDrafts`, `UserDraftsCleaner`, integración en `DeleteAccountViewModel`) completado y
  revisado (20 tests JVM nuevos, 577 en `:app:testDebugUnitTest` en verde, 0 avisos; 5 tests
  instrumentados en `CourseDraftFilesAndroidTest` compilados con `:app:compileDebugAndroidTestKotlin`,
  pendientes de pasar en emulador junto a los de 3a-2).
- 2026-11-27: Subpaso 3a-2 (`CourseDraftEntity`, `CourseDraftMapper`, `CourseDraftDao`,
  `RoomCourseDraftStore`, `ProgressDatabase` v2 → v3 con `MIGRATION_2_3` y esquema `3.json`)
  completado y revisado (9 tests JVM nuevos en `CourseDraftMapperTest`, 586 en
  `:app:testDebugUnitTest` en verde, 0 avisos; `ProgressDatabaseMigrationTest` y
  `RoomCourseDraftStoreTest` compilados con `:app:compileDebugAndroidTestKotlin`, pendientes de
  ejecutar en emulador junto a los de 3a-1 y 3b). **Paso 3a cerrado en código.**
- 2026-11-27: Paso 3b (`CourseUploadRequester`, `WorkUploadRequester`, `CourseUploadWorker`,
  cableado en `AppContainer` y `SettingsDestination.kt`, y tests JVM e instrumentados) completado y
  revisado (5 tests JVM nuevos en `AppContainerInitTest`, 591 en `:app:testDebugUnitTest`; 31 tests
  instrumentados nuevos; batería completa de Parte C + `ProgressDatabaseTest` con 67/67 en verde en
  `emulator-5554`, 0 avisos). **Parte C cerrada.**

## Siguiente paso

Iniciar la **Parte D (`app/ui/`)** con el **paso 4a** (iconos vectoriales, aprobación previa de
tokens de color en `Color.kt`/`Theme.kt` y cadenas ES/EN en `values/strings.xml` y
`values-en/strings.xml`).
