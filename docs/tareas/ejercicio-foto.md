# Ejercicio con foto (entregas)

Un tipo de ejercicio nuevo en el formato de curso: el alumno **lo resuelve en papel, le hace una
foto y la entrega**. La foto se **sube al servidor** y queda **pendiente de revisión**. La revisión
por el profesor (el autor del curso) es la fase 2 y no entra en esta tarea.

Estado: **hecho** (fase 1, 2026-11-25). Solo falta probar TalkBack a mano (ver «Pendiente»).
Decidido: solo fase 1; la foto no va a «Mis notas».
Reutiliza la cámara de `notas-camara.md` y amplía `backend/docs/especificacion-cursos-v1.md`.

## Lo que hay hoy

- Los cursos se escriben en JSON (formato v1) y se publican con `POST /api/courses`; la app no tiene
  editor. Tipos de paso: `lesson`, `singleChoice`, `multipleChoice`, `matching`. Los tres ejercicios
  se corrigen solos y al momento (`Question.isCorrect`).
- Cada curso tiene un autor (`courses.author_id`). No existe el papel de «profesor».
- El servidor solo recibe JSON. `maxRequestSize` (en `App.java`) es el tamaño máximo de un curso y
  vale para todas las rutas.
- La app ya tiene cámara (CameraX), galería y ficheros de fotos (`PhotoFiles`, con fotos temporales
  en `cache/captures/`).
- Hay sesión en la app (`SessionStore`, en memoria) y token Bearer en el servidor.

## Cómo se usa (bocetos)

En el JSON del curso, dentro de `steps`:

```json
{
  "type": "photo",
  "id": "bfs-a-mano",
  "prompt": "Recorre en anchura el grafo de la lección empezando por **A**. Escribe el orden de visita.",
  "explanation": "Orden: A, B, C, D, E. La cola pasa por [A] → [B, C] → [C, D] → [D, E] → [E]."
}
```

**Alumno**, en el estudio del módulo:

```
┌──────────────────────────────┐   ┌──────────────────────────────┐   ┌──────────────────────────────┐
│ Paso 3 de 5                  │   │ ┌──────────┐                 │   │ ✓ Entregada · pendiente de   │
│ Recorre en anchura el grafo… │   │ │ tu foto  │  [Repetir foto] │   │   revisión                   │
│                              │ → │ └──────────┘                 │ → │ Solución: Orden: A, B, …     │
│ Resuélvelo en papel y hazle  │   │                              │   │                              │
│ una foto para tu profesor.   │   │ [      Entregar       ]      │   │ [       Continuar       ]    │
│ [📷 Hacer foto] [🖼 Galería] │   │  (Enviando… / Sin conexión:  │   │                              │
└──────────────────────────────┘   │   Reintentar)                │   └──────────────────────────────┘
                                   └──────────────────────────────┘
```

Si se vuelve al paso más tarde: «Entregada el 24 nov · pendiente de revisión» y «Entregar otra vez».

## Decisiones

Decididas (2026-11-24):

- **K · La foto no va a «Mis notas».** Se hace como foto temporal (`cache/captures/`, como la cámara
  de notas) y se borra del móvil en cuanto el servidor confirma la entrega. Si se sale del paso sin
  entregar, también se borra.
- **Alcance: solo la fase 1** (entregar). La revisión queda para más adelante (ver «Fase 2»).

Pendientes de aprobar:

- **A · Tipo y campos:** `"type": "photo"` con `prompt` y `explanation` (obligatoria, como en los
  demás: es la solución que ve el alumno al entregar y la referencia del profesor en la fase 2).
- **B · Quién revisará (fase 2): el autor del curso.** En la fase 1 solo afecta a que la tabla guarde
  el curso de cada entrega; no hay que decidir nada más ahora.
- **C · El alumno no espera a la revisión.** Tras entregar ve la solución y sigue estudiando.
- **D · Solo en `steps`, nunca en `review`.** La prueba de dominio se corrige al momento; con una foto
  dependería de cuándo revise el profesor. El validador lo rechaza con el **error**
  `PHOTO_IN_REVIEW`.
- **E · Las fotos se guardan en PostgreSQL (`BYTEA`)**, en una tabla `submissions` (alumno, curso,
  versión, ejercicio, foto, fecha y estado; los campos de la revisión se añadirán en la fase 2).
  Alternativa: ficheros en un volumen de Docker y en la tabla solo la ruta.
- **F · Tamaño:** la app reduce la foto antes de subirla (lado mayor 2000 px, JPEG, con
  `Bitmap.compress`, sin librerías). El servidor acepta hasta **3 MB** solo en la ruta de entregas y
  comprueba que sea JPEG por sus primeros bytes. Hay que ajustar el límite global de `App.java`.
- **G · Cómo se sube:** `multipart/form-data`. Javalin lo lee con `ctx.uploadedFile(...)` y Ktor lo
  envía con `submitFormWithBinaryData`: **sin dependencias nuevas** (a comprobar al empezar).
- **H · Privacidad:** en la fase 1, una foto solo la puede descargar el alumno que la subió. Si borra
  su cuenta, se borran sus entregas (`ON DELETE CASCADE`).
- **I · Entregar otra vez:** cada envío es una entrega nueva; cuenta la última.
- **J · Sin conexión al entregar:** aviso y «Reintentar», como en el registro; la foto temporal se
  conserva hasta que se entregue o se salga del paso.
- **L · Hace falta sesión** para entregar: sin ella, el paso pide iniciar sesión y el resto del curso
  se sigue pudiendo estudiar.

## Contrato de la API (fase 1)

| Método y ruta | Quién | Respuestas |
|---|---|---|
| `POST /api/courses/{id}/exercises/{exerciseId}/submissions` (multipart: `photo`, `version`) | alumno con sesión | `201` entrega · `400` no es JPEG / falta la foto · `404` curso o ejercicio `photo` inexistente · `413` demasiado grande · `401` |
| `GET /api/submissions/mine?courseId=…` | alumno | `200` sus entregas (ejercicio, fecha, estado) |
| `GET /api/submissions/{id}/photo` | solo el alumno dueño | `200 image/jpeg` · `403` · `404` |

Estado de una entrega en la fase 1: siempre `PENDING`.

## Alcance

El tipo `photo` en el formato y el validador; la tabla `submissions`; subir una entrega, listar las
propias y descargar su foto; el paso con foto en la app (cámara o galería, entregar, reintentar,
solución tras entregar, «Entregar otra vez»).

## Fuera de alcance

Toda la revisión (fase 2), corregir la foto automáticamente (OCR o IA), guardar la foto en
«Mis notas», varios profesores por curso, clases o grupos, avisos push, varias fotos por entrega,
editar o recortar la foto, reintentar en segundo plano (WorkManager), un editor de cursos en la app.

## Tareas

1. **Backend: formato** (test primero, JUnit). Tipo `photo` en `course-v1.schema.json`, record
   `PhotoExercise`, lectura en `CourseNodeMapper`.
   *Acepta si:* un curso con un paso `photo` se publica (201); sin `explanation` da 422.
2. **Backend: regla** `PHOTO_IN_REVIEW` (error) en `CourseIssueCode` y `ExerciseRules`.
   *Acepta si:* un `photo` en `review` da error con su ruta; en `steps`, no.
3. **Backend: entregas.** Migración `005_create_submissions.sql`, dominio (`Submission`,
   `SubmissionStatus`, `Jpeg` que comprueba los bytes), casos de uso `SubmitPhoto`,
   `ListMySubmissions` y `DownloadSubmissionPhoto`, repositorio JDBC, controlador y rutas; límite de
   3 MB en esa ruta.
   *Acepta si:* desde Yaak: 201, 400 (no JPEG), 404 (ejercicio que no es `photo`), 413, 401; otra
   cuenta recibe 403 al pedir la foto. **Hecha (2026-11-25)**, con el límite en 2 MiB (decisiones F y N);
   el 413 se prueba con el guion del paso 3e-2, no desde Yaak.

   **Plan por pasos de la tarea 3** (propuesta del 2026-11-24, pendiente de aprobar). Cada paso
   termina con `mvn test` en verde; los pasos a–c no tocan base de datos ni HTTP.

   - **3a · Dominio** (test primero). `Jpeg` (objeto de valor: no vacío, empieza por `FF D8 FF`
     —la «firma» de un JPEG—, como mucho `MAX_BYTES`; no se fía del nombre ni del tipo que diga el
     cliente), `SubmissionStatus` (`PENDING`; los de la revisión llegan en la fase 2) y `Submission`
     (id, alumno, curso, versión, ejercicio, fecha, estado, tamaño). Inmutables.
   - **3b · Entregar** (test primero, repositorios en memoria). Puerto `SubmissionRepository` y caso de
     uso `SubmitPhoto`: el curso y la versión existen, el ejercicio es un `photo` **de los `steps`** de
     esa versión y la foto es un `Jpeg` válido. Resultado sellado `SubmitPhotoResult` (`Submitted`,
     `CourseNotFound`, `NotAPhotoExercise`, `InvalidPhoto`), como `RegistrationResult`.
   - **3c · Consultar** (test primero). `ListMySubmissions` (las del alumno en un curso, de la más
     reciente a la más antigua, **sin** los bytes de la foto) y `DownloadSubmissionPhoto` (solo su
     dueño: `Found`, `NotFound`, `Forbidden`).
   - **3d · Base de datos.** `db/init/005_create_submissions.sql` (tabla `submissions`, `photo BYTEA`,
     `ON DELETE CASCADE` desde `users`, índice por alumno y curso) y `JdbcSubmissionRepository`
     (listar sin leer la columna `photo`). Sin test automático del repositorio, como los demás: SQL
     probado a mano en un PostgreSQL desechable (insertar, listar, descargar, cascada, repetir `005`).
   - **3e · HTTP.** `SubmissionController` (leer el multipart: `photo` y `version`), las tres rutas en
     `App.java` con `RequireSession`, respuestas y códigos de error nuevos en `ErrorCode`
     (`INVALID_PHOTO`; los nombres exactos se deciden en el paso). Comprobar antes cómo se configura
     multipart en Javalin 7.2.4 (no se supone). Tests de las partes puras (respuestas, lectura de
     campos).
   - **3f · Yaak y prueba completa.** Carpeta «Entregas» (subir una foto de verdad, no JPEG, ejercicio
     que no es `photo`, sin sesión, la foto de otra cuenta → 403, listar), enviadas contra el servidor.

   Cambios que propone este plan sobre las decisiones:
   - **F · Límite de la foto: 2 MiB** (no 3 MB), el mismo que el de un curso. Así no se toca
     `maxRequestSize` de `App.java`, que hoy protege todas las rutas. La app reduce la foto a 2000 px
     (≈ 0,5–1,5 MB), así que sobra.
   - **M (nueva) · La app envía la versión del curso que estudia.** El servidor comprueba que en *esa*
     versión el ejercicio existe y es `photo`. Los ids son estables entre versiones (§2.4), así que
     en la fase 2 el profesor verá juntas las entregas de un ejercicio aunque cambie la versión.

   Decisiones del paso 3e (aprobadas el 2026-11-25). **Corrigen F:** `maxRequestSize` solo limita
   `ctx.bodyAsBytes()`; el multipart de Javalin 7.2.4 va aparte y por defecto **no tiene límite**
   (una foto de 20 MB se aceptaba y se escribía en el disco temporal; comprobado con una sonda).
   - **N · Límite del multipart en `App.java`:** `config.jetty.multipartConfig` a 2 MiB
     (`UploadLimit.MAX_REQUEST_BYTES`), con la foto en memoria y no en el disco. Solo afecta al multipart.
   - **O · El 413 en dos capas:** si el `Content-Length` pasa de 2 MiB, 413 sin leer nada; sin esa
     cabecera, el límite de Jetty lanza una `ServletException` («400: bad multipart», la misma que un
     multipart roto), que un manejador nuevo de `ExceptionHandlers` convierte en 400 `MALFORMED_BODY`.
   - **P · Códigos nuevos:** `INVALID_PHOTO` (400), `PHOTO_TOO_LARGE` (413), `EXERCISE_NOT_FOUND`
     (404), `SUBMISSION_NOT_FOUND` (404), `NOT_SUBMISSION_OWNER` (403). Se reutilizan `REQUIRED` (falta
     la foto o la versión; `courseId` en la consulta), `INVALID_PARAMETER` y `COURSE_NOT_FOUND`.
   - **Q · Paquete `web/submission`** con su propio lector de ids (no se toca `PathIds`). Una entrega:
     `{id, courseId, courseVersion, exerciseId, submittedAt, status, photoBytes}`; la lista,
     `{"submissions": [...]}`; la foto, `image/jpeg` con `Cache-Control: private, no-store`.

   Decisiones de la tarea 4 (aprobadas el 2026-11-25). **Cambian el plan:** no hay `PhotoTask` como
   `Question`.
   - **R · `Step.Photo`, no una `Question`:** no se corrige sola; como `Step` no puede acabar en la
     prueba de dominio (que sale de `review: List<Question>`). En el JSON, `PhotoDto` es un `StepDto`
     y no un `ExerciseDto`: en `review` da `Malformed`, como `PHOTO_IN_REVIEW` en el servidor.
   - **S · «Entregar más tarde»:** el paso cuenta como hecho si se entrega o se deja para más tarde;
     sin sesión o sin cámara no se bloquea el módulo. La prueba de dominio no depende de él.
   - **T · El puerto solo tiene `submit`** (`SubmissionGateway`, `SubmitOutcome`); listar y descargar,
     cuando haga falta. `ApiErrorCode` con los cinco códigos nuevos.
   - **U · Adaptador con `submitFormWithBinaryData`** (en `ktor-client-core` 3.6.1, sin dependencias
     nuevas); la foto como fichero (`filename="foto.jpg"`, `image/jpeg`) y 60 s de tiempo máximo.
   - **V · La app sigue compilando:** en la tarea 4, un hueco provisional en `ScreenExercise` (el
     enunciado y «Entregar más tarde»); la tarea 5 lo sustituye.

   Decisiones de la tarea 5 (aprobadas el 2026-11-25):
   - **W · La cámara de las notas con un «modo»:** `CameraViewModel` y `ScreenCamera` reciben el
     propósito (nota: pie y «Guardar»; entrega: «Repetir» y «Usar foto»). En modo entrega devuelve la
     ruta de la foto al paso con `savedStateHandle`. No se duplica el permiso ni la máquina de estados.
   - **X · Preparar la foto:** puerto `SubmissionPhotos` (en `app/data`): girarla según el EXIF,
     reducirla a 2000 px y guardarla como JPEG en `cache/submissions/`; el tamaño, con funciones puras
     en `sharedLogic` (`PhotoScaling`).
   - **Y · Sesión caducada:** «Tu sesión ha caducado. Vuelve a iniciar sesión para entregar» y «Entregar
     más tarde»; no se sale del estudio (se perdería lo hecho). No hay modo invitado: entrar en Inicio
     ya exige sesión.
   - **Z · Curso sin versión publicada** (el de ejemplo): no se puede entregar; solo «Entregar más
     tarde», con una frase que lo explica.
   Plan: 5a preparar la foto · 5b `ExerciseViewModel` · 5c la pantalla del paso · 5d cámara y galería ·
   5e prueba en el emulador contra el servidor.
4. **`sharedLogic`** (test primero). `PhotoTask` (`Question`), el estado del paso (sin foto → con foto
   → entregando → entregada / fallo), el puerto `SubmissionGateway` y su adaptador HTTP (multipart),
   probado con `MockEngine`.
5. **App: el paso con foto.** Cámara o galería, reducir la foto, «Entregar», aviso sin conexión y
   «Reintentar», solución tras entregar, borrar la foto temporal (K). Previews en claro y oscuro.
6. **Cierre:** curso de ejemplo en Yaak, prueba en el emulador (con y sin red, sin sesión),
   TalkBack, claro y oscuro; `especificacion-cursos-v1.md` y `especificacion.md`.

## Fase 2 (más adelante, no se hace ahora)

Revisar: listar las entregas pendientes de un curso (solo su autor), ver la foto con zoom, «Bien» /
«Mal» con comentario (`PUT /api/submissions/{id}/review`), y que el alumno vea el resultado.
Preguntas para entonces: ¿se revisa en la app o primero solo con la API? ¿el profesor es siempre el
autor del curso?

## Progreso

- 2026-11-24: primera propuesta (autoevaluación) sustituida: la foto se sube para que la revise un
  profesor. Decidido: solo la fase 1 y la foto no va a «Mis notas».
- 2026-11-24: decisiones A–J y L aprobadas. **Tarea 1 hecha** (falta la prueba por HTTP, ver abajo).
  - `domain/course/PhotoExercise` (record con `id`, `prompt`, `explanation`; mismas reglas que los
    demás ejercicios con `Require.exerciseFields`) y `Exercise` lo permite (`permits … PhotoExercise`).
  - `course-v1.schema.json`: `photo` en los tipos de `step` y de `exercise`, y su definición
    (`additionalProperties: false`, `explanation` obligatoria). Se acepta también en `review`: lo
    rechazará la regla `PHOTO_IN_REVIEW` (tarea 2), que da la ruta exacta.
  - `CourseNodeMapper`: `"photo"` en la factory por tipo.
  - El `switch` sellado avisó en los tres sitios que recorren ejercicios: `Locations.options` y
    `ExerciseRules.unknownOptions` (un `photo` no tiene opciones) y `GuessingOdds` (probabilidad 1:
    no se corrige solo; así la cota nunca se queda corta). También en `StepTest`, que existe para eso.
  - Tests: `PhotoExerciseTest` (5), `PhotoStepReadingTest` (8: lectura, curso válido sin avisos, sin
    `explanation` o sin `prompt` → error en `/modules/0/steps/4`, campo de otro ejercicio, enunciado
    largo, tipo desconocido, leer lo guardado), uno en `GuessingOddsTest` y `StepTest` con cinco
    casos. En rojo antes (no compilaban). **630 tests del backend en verde.**
  - Pendiente: 201 y 422 de verdad por HTTP; hay que reiniciar el servidor con el código nuevo. Las
    peticiones de Yaak con un curso con foto son de la tarea 3.
  - Visto: con el JDK 26, Maven no podía descargar `surefire-junit-platform` (`PKIX path building
    failed`: el JDK no confía en el certificado HTTPS que ve este ordenador). Se resolvió con
    `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` (usar los certificados de Windows).

- 2026-11-24: **tarea 2 hecha.**
  - `CourseIssueCode.PHOTO_IN_REVIEW` (error) y `ExerciseRules.photosInReview`: recorre la reserva de
    cada módulo y da un error por cada `photo`, en su ruta (`/modules/1/review/2`), con un mensaje que
    nombra el ejercicio y dice que se mueva a `steps`. Añadida a la capa 3 de `CourseValidator`.
  - Va en la capa 3 y no en la 5 (dominio) a propósito: el validador para tras la primera capa con
    errores, así que no sale además un `MASTERY_GUESSABLE` (la foto cuenta como acierto seguro).
  - Tests: `PhotoInReviewTest` (7: curso normal sin el error, error y ruta, mensaje, en `steps` no,
    varias fotos en orden del documento, el validador lo rechaza, y que sea el único problema),
    `CourseIssueCodeTest` con el código nuevo y el fixture `PHOTO_IN_REVIEW.json` (lo exige
    `CourseFixturesTest`: un fixture por código). En rojo antes. **638 tests del backend en verde.**
  - Pendiente (tarea 3): documentar el código en el catálogo §3.3 de `especificacion-cursos-v1.md`.

- 2026-11-24: **documentos y Yaak del formato** (parte de la tarea 6, adelantada; la tarea 3,
  entregas, sigue sin empezar).
  - `backend/docs/especificacion-cursos-v1.md`: `photo` en la tabla de tipos (§2.5) con un ejemplo y
    qué es su `explanation`; `PHOTO_IN_REVIEW` en el catálogo (§3.3); probabilidad 1 en §3.6; en el
    contrato con la app (§5), que una foto no se da por corregida y se entrega sin esperar.
  - Yaak, carpeta «Cursos · Publicación»: «Publicar curso con foto (201)» (`rq_0lfUT0fN3s`, el curso
    compacto con un paso `photo`) y «Foto en la reserva (422)» (`rq_JmzZqefikj`, un `photo` en
    `review`). Escritas a mano con el formato de las demás (no hay CLI de Yaak en este ordenador).
  - Comprobado sin servidor: los dos cuerpos pasados por `JacksonCourseReader` y
    `CourseValidator.standard()` reales. El primero es válido (sus 3 avisos de dominio son los del
    curso compacto, que tiene 1 pregunta); el segundo da solo `PHOTO_IN_REVIEW /modules/0/review/0`.
  - Pendiente: enviarlas de verdad desde Yaak con el servidor reiniciado.

- 2026-11-24: plan de la tarea 3 aprobado, con F (la foto cabe en una petición de 2 MiB) y M (la app
  envía la versión). **Paso 3a hecho** (paquete `domain/submission`).
  - `Jpeg`: solo se crea con `Jpeg.check(bytes)`, que devuelve `JpegCheck` (sellado: `Valid`, `Empty`,
    `NotJpeg`, `TooLarge(size)`). Firma `FF D8 FF`; no mira nombre ni `Content-Type`. El tamaño se
    mira antes que la firma. `MAX_BYTES = 2 000 000`: deja ~95 KB de margen para el resto del
    multipart dentro de los 2 MiB de `maxRequestSize` (lo vigila un test). Clase y no record: copia
    los bytes al entrar y al salir, `equals` por contenido y `toString` sin los bytes.
  - `SubmissionStatus` (`PENDING`) y `Submission` (sin los bytes de la foto, solo `photoBytes`;
    versión ≥ 1, ejercicio no vacío, tamaño entre 1 y `MAX_BYTES`), con la factory `pending(…, Jpeg)`.
  - Tests: `JpegTest` (16) y `SubmissionTest` (6), en rojo antes. **660 tests del backend en verde.**

- 2026-11-24: **paso 3b hecho** (paquete `application/submission`).
  - Puerto `SubmissionRepository.add(submission, photo)` (las dos cosas juntas).
  - `SubmitPhotoCommand` (el alumno sale de la sesión; `toString` sin los bytes) y `SubmitPhotoResult`
    sellado: `Submitted`, `CourseNotFound`, `NotAPhotoExercise`, `InvalidPhoto(reason)` con el
    `JpegCheck` que falló (no admite uno válido).
  - `SubmitPhoto`: versión (reutiliza `FindCourse`, que ya trata la versión < 1) → ejercicio `photo`
    en los `steps` de esa versión (los de `review` no cuentan) → foto. Lo publicado se relee solo con
    el lector; si no se entiende, `IllegalStateException` (500): es fallo del servidor. La fecha se
    trunca a microsegundos, como en `RegisterUser`. Cada envío es una entrega nueva (decisión I).
  - Tests: `SubmitPhotoTest` (15) y `SubmitPhotoValuesTest` (4), con `InMemorySubmissionRepository`
    (nuevo) e `InMemoryCourseRepository` / `FakeCourseReader`, que pasan a ser públicos para usarlos
    desde este paquete (solo cambia la visibilidad). En rojo antes. **679 tests del backend en verde.**

- 2026-11-24: **paso 3c hecho** (consultar, en memoria).
  - `SubmissionRepository`: `mine(alumno, curso)` (sin orden y sin los bytes), `find(id)` y
    `photo(id)`. `InMemorySubmissionRepository` las implementa (más `addWithoutPhoto`, solo para probar
    el caso imposible de una entrega sin foto).
  - `ListMySubmissions`: las del alumno en ese curso, la más reciente primero y, a igual instante, por
    id de mayor a menor (`NEWEST_FIRST`). El orden se decide en el caso de uso, no en el repositorio:
    la regla está en un sitio y el JDBC del 3d no tiene que repetirla. Un curso que no existe da una
    lista vacía (no se cuenta qué cursos existen).
  - `DownloadSubmissionPhoto` + `DownloadPhotoResult` (`Found(entrega, foto) | NotFound | Forbidden`):
    ¿existe? → ¿es suya? → la foto. Si no existe, `NotFound` para cualquiera. Una entrega sin foto
    guardada es fallo del servidor (`IllegalStateException`, 500).
  - Tests: `ListMySubmissionsTest` (7) y `DownloadSubmissionPhotoTest` (6), en rojo antes (8 fallaban).
    **692 tests del backend en verde.**
  - Arreglado de paso: `docs/tareas/notas-camara.md` tenía marcas de conflicto subidas en el merge
    `3fce63e`; se juntaron las dos versiones.

- 2026-11-24: **paso 3d hecho** (base de datos).
  - `db/init/005_create_submissions.sql`: tabla `submissions` (foto en `photo BYTEA` y su tamaño en
    `photo_bytes`), `student_id` con `ON DELETE CASCADE` desde `users` (H), clave ajena
    `(course_id, course_version)` → `course_versions` (M: la versión existe), `CHECK` con nombre
    (versión ≥ 1, ejercicio no en blanco, estado conocido, tamaño 1–2 000 000, `photo_bytes =
    octet_length(photo)`) e índice `(student_id, course_id)`. Idempotente (`IF NOT EXISTS`).
  - `JdbcSubmissionRepository`: `add` en una sola sentencia; `mine` y `find` sin leer la columna
    `photo`; `photo` vuelve a pasar los bytes por `Jpeg.check` (si no valen, 500). Sin `ORDER BY`:
    ordena `ListMySubmissions`.
  - Probado en un PostgreSQL 18 desechable (Docker, `postgres:18-alpine`, borrado al acabar; el
    `gradus-postgres` del equipo no se tocó y aún no tiene la `005`): migraciones 001–005 y repetir
    la 005 sin efecto; un programa de prueba (scratchpad) con la clase real: 15/15 (guardar, una foto
    de casi 2 MB, buscar con microsegundos, listar, leer la foto, `ListMySubmissions` y
    `DownloadSubmissionPhoto` sobre JDBC, versión inexistente y id repetido rechazados, cascada al
    borrar la cuenta sin tocar las de otros); y los cuatro `CHECK` rechazando datos imposibles.
  - Sin test automático del repositorio, como los demás JDBC. **692 tests del backend en verde.**
  - Visto: Docker Desktop estaba cerrado; al abrirlo arrancó solo `gradus-postgres`. El puerto
    55432 está reservado por Windows (Hyper-V): el contenedor de prueba usó uno libre.

- 2026-11-25: **paso 3e-1 hecho** (partes puras de la capa web, decisiones N–Q).
  - `ErrorCode`: los cinco códigos de P (al final; no se renombra ninguno). `ErrorCodeTest` los incluye.
  - Paquete `web/submission`:
    - `SubmissionParams`: `id` (UUID canónico) y `version` (entero positivo de hasta 9 cifras).
    - `UploadLimit`: `MAX_REQUEST_BYTES` (2 MiB) y `exceededBy(contentLength)` (`-1` = sin cabecera, no se juzga).
    - `SubmissionResponse`, `SubmissionListResponse`.
    - `JsonReply` (estado + cuerpo) y `SubmissionReplies`: cada desenlace de `SubmitPhotoResult` y cada
      error de las tres rutas, como funciones puras (`switch` sobre los sellados).
  - Tests, en rojo antes: `SubmissionParamsTest` (9), `UploadLimitTest` (5), `SubmissionResponsesTest`
    (5), `SubmissionRepliesTest` (15). Corregido el comentario de `JpegTest` que daba por buena la
    suposición de F. **726 tests del backend en verde.**

- 2026-11-25: **paso 3e-2 hecho** (HTTP).
  - `SubmissionController` (`web/submission`): `submit` (UUID del curso → `Content-Length` → `version`
    → `photo` → `SubmitPhoto`), `mine` y `photo` (`image/jpeg`, `Cache-Control: private, no-store`,
    `X-Content-Type-Options: nosniff`). Lo que responde lo decide `SubmissionReplies` (3e-1).
  - Cambio sobre O: el manejador de la `ServletException` es `web/submission/MultipartErrors` y no un
    método en `ExceptionHandlers` (es del compañero: así no hay conflicto). `App.java` lo registra.
  - `App.java`: límite del multipart (N) con `UploadLimit.MAX_REQUEST_BYTES` (total, fichero y en
    memoria), las piezas de entregas y las tres rutas con `RequireSession`.
  - Probado con el servidor real (`App`) contra un PostgreSQL 18 desechable (borrado al acabar): un
    guion de peticiones (scratchpad) da **37/37**. Entregar (también 2 000 000 bytes y *chunked*), 413
    por la foto (2 000 001) y por el `Content-Length` (3 MB, 20 MB), no JPEG, sin foto, sin versión,
    versión «01» o inexistente, curso inexistente o no UUID, ejercicio que no es `photo`, sin sesión,
    multipart roto, JSON; listar (orden, solo las propias, `courseId` que falta o no vale, sin sesión);
    descargar (mismos bytes, cabeceras, 403 a otra cuenta, 404, sin sesión); y las rutas de cursos
    siguen igual. En la base de datos, solo las 3 entregas válidas.
  - Visto al probar (Jetty): un campo de **texto** del multipart no puede pasar de 200 000 bytes; si
    `photo` llega como texto grande → 400 `MALFORMED_BODY`, si es pequeño → 400 `REQUIRED`. Sin
    `Content-Length`, al pasar el límite el servidor corta la conexión a mitad del envío.
  - **Para la tarea 4 (app):** el campo `photo` tiene que ir como **fichero** (con `filename` en
    `Content-Disposition`); si no, el servidor no lo ve como foto.
  - Sin test automático del controlador (haría falta una dependencia, `javalin-testtools`).
    **726 tests del backend en verde.**

- 2026-11-25: **paso 3f hecho; tarea 3 terminada.**
  - Yaak, carpeta nueva «Entregas» (`fl_ei3GYikdgC`, 14 peticiones): entregar (201), otra vez (201),
    no JPEG (400), sin foto (400), sin versión (400), en una lección (404), en una versión que no
    existe (404), sin sesión (401), mis entregas (200), sin `courseId` (400), descargar la propia (200),
    la de otra cuenta (403), una que no existe (404) y sin sesión (401). La alumna es «otra cuenta» y
    el curso, «Publicar curso con foto (201)» de «Cursos · Publicación».
  - Fotos de prueba en `backend/yaak/fixtures/` (un JPEG de 17 KB con un grafo y un PNG), generadas
    con `ImageIO`. Yaak usa la ruta del fichero tal cual (leído en su código: `crates/yaak-http`,
    `build_multipart_body`) y sustituye las variables también en el cuerpo, así que la ruta es
    `${[ fixturesDir ]}/…`, una variable de un entorno **privado** de cada ordenador (README de Yaak).
  - Arreglado `rq_JmzZqefikj` («Foto en la reserva (422)»): su `description` tenía un `: ` sin
    comillas y no era YAML válido; Yaak no podía leerlo. Los 75 ficheros del workspace se leen ya sin error.
  - Comprobado sin abrir Yaak: un simulador (scratchpad) lee los YAML de verdad, sustituye las
    plantillas y arma el multipart como Yaak (separador `------YaakFormBoundary`, `filename`,
    `Content-Length`), contra el servidor real y un PostgreSQL desechable (borrado): **22/22**, con la
    foto descargada igual byte a byte y «Foto en la reserva (422)», que estaba pendiente.
  - Pendiente: enviarlas desde Yaak de verdad (crear antes el entorno con `fixturesDir` y ejecutar
    `./gradus.sh`, que aplica la `005` a `gradus-postgres`).

- 2026-11-25: **paso 4a hecho** (decisiones R–V).
  - `sharedLogic`: `Step.Photo(id, prompt, explanation)`; `PhotoDto` (`"type": "photo"`, `StepDto` y
    no `ExerciseDto`) y su conversión; `StepState.Photo(photo, later)` (hecho si `later`);
    `StepSession.submitPhotoLater()` y `ModuleStudy.submitPhotoLater()` (fuera de una foto, nada).
  - App (provisional, V): `PhotoTaskContent` en `ScreenExercise` (enunciado, sin la explicación: es la
    solución, decisión C), `PhotoLaterBar` en `ExerciseParts`, `onSubmitPhotoLater` en
    `ExerciseActions`, `ExerciseViewModel` y `MainNavGraph`, y la preview `PhotoTaskPreview`. Los
    cursos con foto ya se abren en la app (antes, `CourseDecoding.Malformed`).
  - Tests, en rojo antes: `CourseDtoMappersTest` (+4: se lee, no es un ejercicio del módulo, en
    `review` y sin `explanation` dan `Malformed`), `ModuleStudyTest` (+7) y `ExerciseViewModelTest`
    (nuevo, 2). Este último encontró un fallo: el ViewModel avanzaba siempre tras «más tarde», así
    que un toque tardío en una lección la saltaba; ahora solo avanza si había una foto que dejar.
  - **760 tests de `sharedLogic` y 322 JVM de la app en verde**; los instrumentados compilan.

- 2026-11-25: **paso 4b hecho** (estado de la tarea con foto, `sharedLogic`).
  - Adelantado del 4c lo que el estado necesita: paquete `submission` con `Submission(id, submittedAt)`,
    `PhotoRejection` (`MISSING`, `NOT_JPEG`, `TOO_LARGE`) y `SubmitOutcome` (sellado: `Submitted` y los
    `Problem` `PhotoRejected`, `NotFound`, `SessionExpired`, `Unreachable` y `ServerFailure`, cada uno
    con `canRetry`).
  - `PhotoStep` (máquina inmutable) y `PhotoTask` (`Empty`, `Ready`, `Sending`, `Failed`, `Sent`):
    `take`, `discard`, `send`, `finished`, `again` y `leaveForLater`; `isDone` (hay entrega o «más
    tarde»), `canSend`, `canLeaveForLater` (no mientras se entrega), `showsSolution` y `photoPath` (el
    fichero mientras hace falta, para que la app lo borre, decisión K). Solo cuenta la respuesta de un
    envío en curso; una foto rechazada o un ejercicio que ya no existe no se reintentan; si falla una
    segunda entrega, la primera sigue contando.
  - `StepState.Photo(step: PhotoStep)` sustituye al `later` provisional del 4a;
    `StepSession.updatePhoto(stepId, …)` y `ModuleStudy.updatePhoto` / `currentPhoto`: la foto se cambia
    por el id del paso, para que una respuesta de la red vaya al suyo.
  - Tests, en rojo antes: `PhotoStepTest` (27) y `ModuleStudyTest` (+6). **793 tests de `sharedLogic`
    y 322 JVM de la app en verde.**

- 2026-11-25: **paso 4c hecho** (puerto de entregas, `sharedLogic`).
  - `PhotoSubmission(courseId, courseVersion, exerciseId, jpeg)`: clase y no data class (un `ByteArray`
    se compara por identidad): copia los bytes al entrar y al salir, `equals`/`hashCode` por contenido y
    `toString` con el tamaño y sin los bytes; versión ≥ 1, ids no en blanco, foto no vacía.
  - Puerto `SubmissionGateway.submit(session, submission): SubmitOutcome` (decisión T) y
    `FakeSubmissionGateway`: por defecto acepta cada foto como una entrega nueva («entrega-1», …);
    también responde siempre lo mismo o lo que diga una función, y recuerda lo recibido y con qué sesión.
  - `ApiErrorCode`: los cinco códigos nuevos, antes de `UNKNOWN`.
  - Tests, en rojo antes: `PhotoSubmissionTest` (8), `FakeSubmissionGatewayTest` (5) y el contrato
    de `ApiErrorCode` en `RegisterProblemTest`. **806 tests de `sharedLogic` y 322 JVM de la app en verde.**

- 2026-11-25: **paso 4d hecho; tarea 4 terminada.**
  - `HttpSubmissionGateway` (`submission/http`): `submitFormWithBinaryData` con `version` y `photo`
    (fichero `foto.jpg`, `image/jpeg`), token y 60 s (`timeout { … }`). 201 → `Submitted`; 400
    `INVALID_PHOTO` / `REQUIRED` en `photo` → `PhotoRejected` (`NOT_JPEG` / `MISSING`); otro 400 →
    `ServerFailure` (sería un fallo de la app); 413 → `TOO_LARGE`; 404 → `NotFound`; 401 →
    `SessionExpired`; sin red o sin tiempo → `Unreachable`. `SubmissionGateways.http(baseUrl)`.
  - Encontrado contra el servidor real: con más de 2 MiB, el servidor responde 413 sin leer (decisión
    O) y corta la conexión; OkHttp lo ve como fallo de red y daba `Unreachable` («sin conexión»).
    Arreglo: `PhotoSubmission.MAX_BYTES` (2 000 000, como `Jpeg.MAX_BYTES`) e `isTooLarge`; el
    adaptador rechaza la foto con `TOO_LARGE` **sin enviarla**.
  - Tests, en rojo antes: `HttpSubmissionGatewayTest` (19, con `MockEngine`: ruta, token, multipart,
    bytes intactos, 60 s, cada respuesta y el límite sin petición) y `PhotoSubmissionTest` (+2).
  - Probado contra el servidor real (`App`) y un PostgreSQL desechable (borrado), con una sonda
    temporal en `androidHostTest` (OkHttp de verdad; borrada al acabar): JPEG real y de 1 999 000 bytes
    → entregadas; PNG → `NOT_JPEG`; 2 000 001 y 3 000 000 bytes → `TOO_LARGE`; una lección y la versión
    99 → `NotFound`; token inventado → `SessionExpired`; servidor apagado → `Unreachable`. La foto
    descargada después es igual byte a byte.
  - **827 tests de `sharedLogic` y 322 JVM de la app en verde.**

- 2026-11-25: **paso 5a hecho** (preparar la foto; decisiones W–Z aprobadas).
  - `sharedLogic`: `PixelSize` y `PhotoScaling` (`MAX_SIDE` = 2000; `fit`: proporción, sin agrandar,
    redondeo y al menos 1 px; `sampleSize`: la mayor potencia de 2 que deja el lado mayor en 2000 o más).
    `PhotoScalingTest` (10), en rojo antes. **837 tests de `sharedLogic` en verde.**
  - App: puerto `SubmissionPhotos` (`prepare`, `read`, `delete`) y `AndroidSubmissionPhotos`: lee la
    URI una vez, decodifica reducida, gira según el EXIF y escala al tamaño final en una sola matriz, y
    comprime en JPEG con la mejor calidad que quepa en `PhotoSubmission.MAX_BYTES` (85, 70, 55, 40).
    Solo lee y borra dentro de su carpeta; no toca la foto de origen.
  - `AndroidSubmissionPhotosTest` (instrumentado, 10: grande → 2000 × 1500, pequeña igual, tumbada →
    derecha, PNG → JPEG, ruido cabe en 2 MB, fichero nuevo y origen intacto, no imagen → nada, leer,
    borrar y no borrar fuera). En rojo antes (no compilaba). **Pendiente de ejecutarlo en el emulador.**

- 2026-11-25: **paso 5b hecho** (`ExerciseViewModel` con el paso con foto).
  - `sharedLogic`: `ModuleStudy.photoPaths` (los ficheros de foto aún en uso en la parte formativa) y
    `PhotoStep.leaveForLater()` suelta la foto (al salir del paso sin entregar se borra, decisión K).
    Tests +4 (`ModuleStudyTest` 3, `PhotoStepTest` 1), en rojo antes. **841 tests de `sharedLogic`.**
  - `ExerciseViewModel`: recibe `SubmissionGateway`, `SessionStore`, `SubmissionPhotos` y un ámbito
    `cleanup` que sobrevive al ViewModel. `onPhotoChosen` (prepara la foto; aviso `PhotoNotice`
    `PREPARING` / `UNREADABLE`; ignorada mientras se entrega o ya entregada), `onDiscardPhoto`,
    `onSubmitPhoto` (entregar o reintentar; un doble toque envía una vez; la respuesta va a su paso),
    `onSubmitAgain`, `onSubmitPhotoLater`, `onLeave` / `onCleared` y `canSubmitPhotos` (Z). Sin sesión →
    `SessionExpired` sin enviar (Y); foto desaparecida de la caché → `PhotoRejected(MISSING)`.
    **Todo cambio pasa por `change()`**, que borra las fotos que dejan de estar en `photoPaths` (al
    entregar, cambiar, descartar, dejar para más tarde o acabar los pasos).
  - `AppContainer`: `submissionGateway`, `submissionPhotos` (`cache/submissions/`) y `backgroundScope`
    (`SupervisorJob` + `Dispatchers.IO`); `MainNavGraph` pasa las piezas nuevas a la factory.
  - Tests: `ExerciseViewModelTest` (20; 18 nuevos, en rojo antes) con `FakeSubmissionGateway`,
    `InMemorySessionStore` y `FakeSubmissionPhotos` (nuevo, en `app/src/test`). **340 tests JVM de la
    app en verde**; los instrumentados compilan.
  - Para el 5d: la foto de la cámara (`cache/captures/`) es un origen más; después de prepararla hay que
    tirarla (`PhotoFiles.discardCapture`), porque `prepare` no toca el origen.

- 2026-11-25: **paso 5c hecho** (la pantalla del paso con foto; sustituye al hueco provisional del 4a).
  - `PhotoStepTexts.kt` (puro, con test JVM): el texto de cada `SubmitOutcome.Problem`;
    `mainButton(canSubmit)` («Entregar», «Entregando…», «Reintentar» o, entregada, «Continuar») y
    `secondaryButton()` (`SecondaryPhotoButton`: «Entregar más tarde», «Entregar otra vez» o, si ya
    hay una entrega que cuenta, «Continuar sin entregar otra»; ninguno mientras se entrega).
  - `PhotoStepParts.kt`: `PhotoStepContent` (módulo, enunciado en Markdown como encabezado, la foto
    con «Tu foto» para TalkBack, el aviso de `PhotoNotice` con liveRegion, «Hacer foto» / «Hacer otra»,
    «De la galería» y «Quitar la foto»; sin ellos mientras se entrega, ya entregada o en un curso sin
    publicar, que lo explica) y `PhotoStepBottomBar` (el fallo con liveRegion, «Entregada · pendiente
    de revisión» con la solución, y los dos botones). `PhotoStepUi(notice, canSubmit)`.
  - `ScreenExercise` recibe `photo: PhotoStepUi`; `ExerciseActions` con `onTakePhoto` (null: sin
    cámara), `onPickPhoto`, `onDiscardPhoto`, `onSubmitPhoto` y `onSubmitAgain`; `BottomPanel` pasa a
    `internal`; fuera `PhotoLaterBar` y `PhotoTaskContent` (provisionales). `MainNavGraph` pasa las
    acciones del ViewModel y el `PhotoStepUi`; la cámara y la galería se conectan en el 5d.
  - Previews en claro y oscuro de cada estado (sin foto, preparando, con foto, entregando, sin conexión,
    rechazada, entregada, curso sin publicar).
  - Tests: `PhotoStepTextsTest` (16, JVM, en rojo antes). `PhotoStepScreenTest` (14, Compose,
    instrumentado; escrito después de la pantalla y **pendiente de ejecutar en el emulador**).
    **356 tests JVM de la app en verde**; los instrumentados compilan.

- 2026-11-25: **paso 5d hecho** (la cámara en modo entrega y la galería).
  - Fotos de la cámara: `SubmissionPhotos.prepareCapture` (prepara y tira la de la cámara) y
    `discardCapture` (solo dentro de `cache/captures/`); `AndroidSubmissionPhotos` recibe esa carpeta.
    `ExerciseViewModel.onPhotoTaken`: si el paso no acepta foto ahora (se está entregando), la tira sin
    prepararla. `onPhotoChosen` y `onPhotoTaken` comparten `usePhoto`.
  - Cámara con propósito (W, patrón Strategy): `CameraPurpose.Note(courseId, moduleId, notes)` o
    `CameraPurpose.Submission`; estado nuevo `CameraStage.Used(capture)` («Usar foto»: ni se guarda ni
    se tira, ahora es del paso); `CameraUiState.forSubmission` (sin pie, «Usar foto» y otro texto al pedir
    el permiso). `onShutter` y `onCaptureFailed` ahora copian el estado (antes lo creaban de nuevo y se
    perdía el propósito).
  - Navegación: lo de Android de la cámara (permiso, CameraX, ajustes) pasa de `TakeNoteDestination` a
    `CameraRoute` (compartido); `Route.TakeSubmissionPhoto` y `TakeSubmissionPhotoDestination`, que
    devuelve la foto con el `savedStateHandle` de la pantalla anterior; `rememberExercisePhotoSources`
    (recoge esa foto, el Photo Picker y si hay cámara). `MainNavGraph`: 15 líneas en total desde el 4a.
  - Tests, en rojo antes: `ExerciseViewModelTest` (+3) y `CameraViewModelTest` (+6). Instrumentados,
    compilados y **pendientes de ejecutar**: `AndroidSubmissionPhotosTest` (+3, 13 en total),
    `ScreenCameraTest` (+2) y `PhotoStepScreenTest` (14). **365 tests JVM de la app en verde.**

- 2026-11-25: **paso 5e hecho; tarea 5 terminada** (emulador `Medium_Phone_API_37.0`).
  - Tests instrumentados, solo los de este trabajo: `AndroidSubmissionPhotosTest` (13),
    `ScreenCameraTest`, `PhotoStepScreenTest` (14) y `StudyAccessibilityTest`: **47/47**. Uno falló al
    principio y era real: el enunciado (MarkdownText) no llegaba a TalkBack como encabezado, porque
    `heading()` quedaba en el contenedor y no junto al texto; arreglado con
    `semantics(mergeDescendants = true)`.
  - Servidor con `./gradus.sh` (aplicó la `005` a `gradus-postgres`); curso publicado con las peticiones
    de Yaak de «Cursos · Publicación»; la app con la cuenta de prueba. Comprobado:
    - Un curso con foto se abre (antes, `Malformed`) y el paso se ve como en el boceto.
    - Cámara en modo entrega: permiso, «Foto del ejercicio · Introducción», sin pie, «Repetir» /
      «Usar foto»; la foto de la cámara se tira y la preparada ocupa 80 902 bytes.
    - Sin conexión (quitando el `adb reverse`): aviso, la foto se conserva, «Reintentar» la entrega.
    - Entregada: «Entregada · pendiente de revisión», la solución, «Continuar» y «Entregar otra vez»; la
      foto se borra del móvil. En la base de datos: `intro-foto`, versión 1, `PENDING`, 80 902 bytes.
    - «Entregar otra vez» desde la galería (el Photo Picker de este emulador pide «Done»): «Continuar
      sin entregar otra» mientras tanto; segunda entrega en el servidor (15 603 bytes).
    - «Entregar más tarde» y cerrar el módulo con una foto pendiente: la foto se borra (1 → 0).
    - Modo oscuro; al cambiarlo Android recrea la pantalla y la foto y el estado se conservan.
  - No probado a mano: TalkBack (no se maneja con adb; lo cubren los tests de semántica), una foto
    rechazada (la app siempre prepara un JPEG que cabe) y la sesión caducada (habría que borrar las
    sesiones de la cuenta de prueba en `gradus-postgres`); los dos últimos, con tests JVM.
  - **365 tests JVM de la app y 841 de `sharedLogic` en verde.**

- 2026-11-25: **tarea 6 hecha** (cierre; documentos).
  - `backend/docs/api-entregas.md` (nuevo): las tres rutas, el multipart (`version` y el fichero
    `photo`), los límites (2 000 000 bytes la foto, 2 MiB la petición, 200 000 los campos de texto), las
    respuestas y los códigos nuevos y reutilizados. Aparte de `api-cursos.md`, que es sobre todo del
    compañero.
  - `docs/especificacion.md`: el ejercicio con foto entre las funcionalidades, la tarea con foto en
    «Estudiar un módulo» y en §7, y en §8 la cámara y el backend.
  - `backend/docs/especificacion-cursos-v1.md` §5: «Entregar más tarde» y el enlace a `api-entregas.md`.
  - `docs/resumen-proyecto.md` (local, en `.gitignore`): la fila del ejercicio con foto, la ruta de
    entregar, la cámara y un límite conocido.
  - El curso de ejemplo con foto ya estaba en Yaak («Publicar curso con foto (201)»).

## Pendiente para más adelante

- **Probar TalkBack a mano** en el paso con foto (no se maneja con adb). Lo cubren `PhotoStepScreenTest`
  y `StudyAccessibilityTest` (encabezado, «Tu foto», avisos con liveRegion, 48 dp).
- Fase 2: la revisión por el autor del curso (ver arriba).
- Enseñar en la app las entregas anteriores al volver al paso («Entregada el 24 nov»); el servidor ya
  las lista (`GET /api/submissions/mine`).
- Si se guarda la sesión al cerrar la app (`registro-cuentas-reales.md`, tarea 6), «sesión caducada»
  será más frecuente: se podría ofrecer iniciar sesión desde el paso.

## Siguiente paso

Ninguno en esta funcionalidad: está terminada (fase 1). Queda la prueba de TalkBack a mano.
