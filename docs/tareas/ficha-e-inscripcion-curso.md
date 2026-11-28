# Ficha del curso, inscripción y baja

Sustituir la pantalla de relleno actual (`ScreenCourseOverview`) al pulsar un curso en **Buscar**
por las vistas definitivas de inscripción y curso ya inscrito (`assets/mockups/course-inscription.png`
y `assets/mockups/course-already-inscripted.png`), incluyendo «Pausar curso» (provisional, sin
acción) y «Darse de baja» (en línea, con confirmación destructiva y borrado del progreso).

Este documento es el **índice común**: objetivo, decisiones aprobadas, reparto en partes, criterios
de aceptación y acuerdos de proceso. El detalle de cada parte vive en su propio documento para que
cada orquestador cargue solo lo suyo.

Estado: **Parte A (Backend) cerrada y contrato congelado — Parte B (`sharedLogic` y Room) lista para iniciar** (2026-11-28).

## Mapa de documentos (qué lee cada uno)

| Documento | Qué tiene | Lo leen |
|---|---|---|
| `AGENTS.md` | Reglas del proyecto | Todos |
| Este documento: **Relevo general** y **Decisiones** | Lo común a todas las partes | Todos los orquestadores |
| `backend/docs/api-progreso.md` | **Contrato congelado** de progreso entre backend y app (`POST /api/progress/sync` + `DELETE /api/progress/enrollments/{courseId}`) | Orquestadores A y B |
| `backend/docs/tareas/baja-curso-backend.md` | Parte A: endpoint de baja en el backend (**cerrada**) | Orquestador A y sus implementadores |
| `docs/arquitectura-progreso.md` | **Referencia viva** del progreso (piezas, invariantes, Room, sincronización) | Orquestadores B, C y D |
| `docs/tareas/ficha-curso.md` | Contexto de `ScreenCourseDetail` ya implementado en `ui/screens/detail/` | Orquestadores C y D |
| `assets/mockups/course-inscription.png` y `course-already-inscripted.png` | Diseño visual de referencia (adjuntar en el chat de la Parte C) | Orquestador C |

## Relevo general

Se **reescribe** cuando cambia algo que afecta a más de una parte; no se acumula.

- **Dónde estamos (2026-11-28):** **Parte A (Backend) terminada y cerrada** (ver
  `backend/docs/tareas/baja-curso-backend.md`).
  - `DELETE /api/progress/enrollments/{courseId}` está implementado, probado con 968 tests JUnit
    (`mvn test`) y comprobado contra PostgreSQL real (7 escenarios incluyendo `ON DELETE CASCADE` en
    `mastered_modules`, aislamiento por usuario/curso, idempotencia y reinscripción limpia), con
    petición en `backend/yaak/workspace/yaak.rq_kN8pQr4WvX.yaml`.
  - **Contrato congelado en `backend/docs/api-progreso.md`:** `DELETE /api/progress/enrollments/{courseId}`
    requiere `Authorization: Bearer <token>` y devuelve `204 No Content` sin cuerpo (idempotente
    incluso si el usuario no estaba inscrito o el UUID no corresponde a ningún curso), `401
    UNAUTHENTICATED` (`WWW-Authenticate: Bearer`) si la sesión no es válida, y `404 COURSE_NOT_FOUND`
    (`field: "courseId"`) si `{courseId}` no es un UUID canónico (`8-4-4-4-12` hex). `POST /api/progress/sync`
    no ha cambiado. La app trata cualquier respuesta distinta de `204` y `401` como `ServerFailure`.
  - En la app todavía no se ha tocado nada: `ProgressDatabase` sigue congelada en **v3**
    (`app/schemas/.../3.json`), `ScreenCourseDetail` sigue desconectada de Buscar y Buscar sigue
    navegando a `Route.CourseOverview`.
- **Siguiente:** abrir el chat orquestador/planificador de la **Parte B (`sharedLogic` y Room)**
  para crear `docs/tareas/ficha-curso-logica.md` e implementar `ProgressGateway.unenroll`
  (`UnenrollOutcome`), `EnrollmentStore.unenroll` (`InMemory` y `Room`), `UnenrollFromCourse` y el
  `@Transaction` de `ProgressDao` + `RoomProgressStore.markMastered`.
- **Dependencias entre partes:**
  - ~~Parte B depende del contrato `DELETE` congelado por la Parte A.~~ (**desbloqueada**)
  - Parte C depende de `UnenrollFromCourse` y `EnrollmentStore.unenroll` de la Parte B.
  - Parte D depende de la Parte C probada en el emulador.
- **Nota de deuda técnica deliberada (para «Pausar curso»):** `ProgressDatabase` se queda en **v3**
  en esta rama (borrando `mastered_modules` y `enrollments` en un `@Transaction` en `ProgressDao`).
  Cuando en el futuro se cree `MIGRATION_3_4` para implementar «Pausar curso», se aprovechará esa
  misma migración para añadir `onDelete = ForeignKey.CASCADE` a `MasteredModuleEntity` y alinear
  Room con PostgreSQL.

## Objetivo

Al pulsar un curso en **Buscar**, mostrar su ficha definitiva:

- **Si el estudiante no está inscrito** (`course-inscription.png`): ver la información del curso
  (etiquetas, título, autor, módulos, fecha de publicación, descripción, «Qué aprenderás»,
  «Incluye») y poder inscribirse con **«Inscribirse al curso»**. Al inscribirse, la pantalla pasa
  automáticamente al estado de curso ya inscrito.
- **Si ya está inscrito** (`course-already-inscripted.png`): ver además su progreso (porcentaje,
  barra lineal y módulos dominados), entrar al camino con **«Ir al curso →»** y abrir el menú `⋮`
  con dos opciones:
  - **«Pausar curso»**: visible pero sin acción hasta que se diseñe esa funcionalidad.
  - **«Darse de baja»**: tarea destructiva con diálogo de confirmación; confirma contra el servidor,
    borra inscripción y progreso del curso en servidor y móvil, y devuelve la vista al estado de
    inscripción.

## Decisiones aprobadas (2026-11-28)

1. **Sin valoraciones ni opiniones en esta rama:** quedan fuera la puntuación con estrellas, la
   columna de opiniones, las pestañas «Sobre el curso / Opiniones» (se muestra directamente «Sobre
   el curso») y el icono de marcador/guardar de `course-inscription.png`.
2. **Baja en línea (Opción A):** «Darse de baja» requiere conexión con el servidor. La app llama
   primero al nuevo endpoint `DELETE`; solo cuando el servidor confirma la baja se borra en Room. Si
   no hay red o el servidor falla, se informa en el diálogo y no se borra nada. Así ningún usuario
   cree estar dado de baja mientras otro dispositivo sigue viendo el curso, ni `POST /api/progress/sync`
   vuelve a resucitar el progreso.
3. **Contrato de `POST /api/progress/sync` sin cambios:** `sync` mantiene su semántica de unión sin
   borrados; la baja se añade como ruta `DELETE` independiente en `backend/docs/api-progreso.md`.
4. **Room se mantiene en v3 (sin migración en esta rama):** en `ProgressDao`, el borrado local de
   una inscripción ejecuta `DELETE` de `mastered_modules` y luego `DELETE` de `enrollments` dentro
   de un mismo `@Transaction` (todo o nada). Se deja documentado (en `docs/arquitectura-progreso.md`
   y comentario en `MasteredModuleEntity`) añadir `onDelete = ForeignKey.CASCADE` cuando se haga
   `MIGRATION_3_4` para «Pausar curso».
5. **Alcance del borrado al darse de baja:** se borran únicamente la inscripción (`enrollments`) y
   los módulos dominados (`mastered_modules`) de ese usuario en ese curso. No se borran las notas
   locales con cámara (`GradusDatabase`), ni el historial de entregas con foto en el servidor
   (`submissions`), ni la copia local del curso (`course_copies`).
6. **«Pausar curso» provisional:** aparece en el menú `⋮` de la vista ya inscrita junto a «Darse de
   baja», pero no realiza ninguna acción todavía.
7. **Diálogo destructivo de baja:** sigue el estilo del proyecto (`AlertDialog`, acción principal
   en `MaterialTheme.colorScheme.error`, similar a `DeleteAccountDialog`), avisa de que se perderá
   todo el progreso del curso, muestra estado de envío y posibles errores de red/servidor, y al
   completarse cierra el diálogo y muestra la vista de inscripción.
8. **Convenciones de UI sobre los mockups:**
   - **Sin barra inferior de navegación**, coherente con el resto de pantallas de detalle/curso.
   - Terminología **«módulos»** (como en `especificacion.md`, Buscar y el camino) en lugar de
     «conceptos».
   - Sección «Incluye» en `course-inscription.png` con chips informativos fijos del formato Gradus
     (el orquestador C puede confirmar los textos exactos con los usuarios).
   - Textos en español e inglés (`values/strings.xml` y `values-en/strings.xml`).

## Partes y tareas

La casilla ✅ la marca el revisor (el orquestador de la parte), no quien implementa.

| Parte | Qué cubre | Documento | Orquestador |
|---|---|---|---|
| **A · Backend** ✅ | Endpoint `DELETE /api/progress/enrollments/{courseId}`, caso de uso `UnenrollCourse`, JDBC, contrato `api-progreso.md`, Yaak y tests JUnit | `backend/docs/tareas/baja-curso-backend.md` (cerrada) | Propio |
| **B · `sharedLogic` y Room** | `ProgressGateway.unenroll` + HTTP + Fake, `EnrollmentStore.unenroll` (`InMemory` y `Room`), caso de uso `UnenrollFromCourse`, `@Transaction` en `ProgressDao` y `markMastered` seguro, tests `kotlin.test` + instrumentados Room | `docs/tareas/ficha-curso-logica.md` (lo crea el orquestador B) | Propio |
| **C · Interfaz (`ui/screens/detail/`)** | `CourseDetailViewModel` con inscripción y baja, rediseño de `ScreenCourseDetail` según ambos mockups (sin valoraciones), menú `⋮` («Pausar curso» + «Darse de baja»), diálogo destructivo, ES/EN, previews claro/oscuro y tests | `docs/tareas/ficha-curso-ui.md` (lo crea el orquestador C) | Propio |
| **D · Integración y cierre** | Conectar Buscar → `Route.CourseDetail`, retirar `ScreenCourseOverview` y `Route.CourseOverview` (con permiso), prueba manual en emulador, batería completa y actualización de `docs/especificacion.md` y `docs/arquitectura-progreso.md` | Este documento (o con el orquestador C) | El último que termine |

### Criterios mínimos por parte

- **Parte A (Backend):**
  - `DELETE /api/progress/enrollments/{courseId}` requiere sesión (`RequireSession`: `401` sin
    token o token inválido).
  - Borra la inscripción del usuario autenticado en ese curso y, por `ON DELETE CASCADE` en
    PostgreSQL, todos sus `mastered_modules`; devuelve `204 No Content` (idempotente si no estaba
    inscrito; nunca borra inscripciones de otros usuarios ni otros cursos).
  - Decidir y probar el caso de `{courseId}` que no es un UUID válido (coherente con `PathIds` de
    cursos o `404`/`400`, documentado en `backend/docs/api-progreso.md`).
  - Tests JUnit (caso de uso, controlador/ruta y persistencia) en verde con `mvn test`, más
    petición en `backend/yaak/` y contrato actualizado en `backend/docs/api-progreso.md`.
- **Parte B (`sharedLogic` y Room):**
  - `ProgressGateway.unenroll(session, courseId)` devuelve tipo sellado `UnenrollOutcome`
    (`Unenrolled | SessionExpired | Unreachable | ServerFailure`) probado con `MockEngine`
    (204, 401, 404/400, 5xx, sin red).
  - `UnenrollFromCourse`: sin sesión no llama; si el servidor responde `Unenrolled` borra en
    `EnrollmentStore`; con `Unreachable`/`ServerFailure` no borra nada; con `SessionExpired` borra
    la sesión con `clearIfCurrent` y no borra el progreso local.
  - `ProgressDao` borra `mastered_modules` y `enrollments` en un `@Transaction` (sin cambiar
    esquema v3), y `RoomProgressStore.markMastered` queda protegido en `@Transaction`.
  - Tests `sharedLogic` (`:sharedLogic:testAndroidHostTest`) y tests instrumentados de Room en verde.
- **Parte C (Interfaz):**
  - Vista no inscrito fiel a `course-inscription.png` (sin valoraciones ni marcador) y vista ya
    inscrito fiel a `course-already-inscripted.png` (sin valoraciones), ambas en claro y oscuro.
  - Inscribirse pide `progressSync` y `courseCopy` y cambia a la vista ya inscrito.
  - Menú `⋮` con «Pausar curso» (sin acción) y «Darse de baja» (diálogo de confirmación con manejo
    de carga y error; al confirmarse vuelve a la vista de inscripción con progreso a 0).
  - Textos ES y EN, TalkBack revisado (zonas táctiles ≥ 48 dp, `contentDescription` en `⋮` y
    atrás), tests unitarios del ViewModel y tests instrumentados de UI.
- **Parte D (Integración y cierre):**
  - Buscar abre `Route.CourseDetail(id)`.
  - `ScreenCourseOverview` y `Route.CourseOverview` retirados tras confirmación del usuario.
  - Prueba completa en emulador contra el servidor local (`gradus.sh up`) y documentación
    actualizada (`docs/especificacion.md` y `docs/arquitectura-progreso.md`, incluyendo la nota
    para `MIGRATION_3_4` + `ON DELETE CASCADE`).

## Alcance y fuera de alcance

- **Dentro:** lo listado en las partes A, B, C y D.
- **Fuera:** valoraciones y opiniones (pestaña «Opiniones», puntuación media, escribir reseña);
  guardar/marcar cursos como favoritos; lógica y persistencia de «Pausar curso»; baja sin conexión
  (*offline-first*); borrado de notas con cámara o entregas con foto al darse de baja; migración de
  `ProgressDatabase` v3 → v4 (se pospone a «Pausar curso»).

## Acuerdos de proceso (para todas las partes)

- **Un orquestador por parte** (o uno nuevo si el chat se alarga). Al empezar lee `AGENTS.md`, el
  Relevo general, las Decisiones de este documento y el de su parte; al terminar reescribe el Relevo
  de su parte y actualiza el Relevo general si cambia algo común.
- **El orquestador** planifica los pasos pequeños, indica **modelo y esfuerzo** recomendados para
  cada paso, redacta el mensaje para quien implementa, revisa el trabajo, marca las casillas y
  propone el **mensaje de commit** (`add:`/`fix:`/`test:`/`docs:` + descripción en inglés +
  `(course-detail task N)`). Los commits los hacen los usuarios.
- **Primero proponer y esperar OK:** antes de tocar código en cualquier paso, el implementador
  propone ficheros, forma del código y tests, y espera aprobación.
- **Lógica pura con test primero** (rojo → verde → refactor). Los tests también documentan casos
  límite y comportamiento.
- **Sin permiso, nada se escribe en la base de desarrollo** (PostgreSQL local).
- **No modificar esquemas ni borrar ficheros sin permiso expreso** (`AGENTS.md` §9).

## Progreso

- 2026-11-28: análisis del repositorio completado (confirmado el conflicto con el invariante «no hay
  baja» del progreso actual y la existencia de `ScreenCourseDetail` en `ui/screens/detail/`).
  Decisiones 1–8 aprobadas por el usuario (baja en línea Opción A, Room en v3 con nota para
  `MIGRATION_3_4` + `ON DELETE CASCADE`, sin valoraciones). Creado este documento índice para lanzar
  el chat orquestador/planificador de la Parte A (Backend).
- 2026-11-28: **Parte A (Backend) cerrada (tareas A1–A4).** Implementado
  `DELETE /api/progress/enrollments/{courseId}` (`UnenrollCourse`, `JdbcEnrollmentRepository.delete`,
  `CanonicalUuid`, `ProgressController.unenroll` y `App.java`), 968 tests JUnit en verde, 7
  escenarios comprobados contra PostgreSQL real, petición añadida en Yaak
  (`yaak.rq_kN8pQr4WvX.yaml`) y ampliación congelada en `backend/docs/api-progreso.md`. **Parte B
  desbloqueada.**
