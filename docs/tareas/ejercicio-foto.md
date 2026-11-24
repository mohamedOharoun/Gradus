# Ejercicio con foto (entregas)

Un tipo de ejercicio nuevo en el formato de curso: el alumno **lo resuelve en papel, le hace una
foto y la entrega**. La foto se **sube al servidor** y queda **pendiente de revisión**. La revisión
por el profesor (el autor del curso) es la fase 2 y no entra en esta tarea.

Estado: **propuesta** (2026-11-24). Sin código. Decidido: solo fase 1; la foto no va a «Mis notas».
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
   cuenta recibe 403 al pedir la foto.
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

## Siguiente paso

Probar 201 / 422 por HTTP tras reiniciar el servidor; después, tarea 2 (`PHOTO_IN_REVIEW`).
