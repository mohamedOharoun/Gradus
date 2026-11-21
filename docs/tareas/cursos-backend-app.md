# Cursos del backend en la app

Conectar la app con el backend de cursos del compañero: buscar cursos en el catálogo, abrir uno y
estudiarlo en el camino y en la práctica que ya existen
(`camino-curso-y-ejercicios.md`). Hasta ahora todo funcionaba con un curso de ejemplo escrito en
el código (`DemoCourse.kt`).

Contrato de la API: `backend/docs/api-cursos.md`. Formato del curso:
`backend/src/main/resources/schemas/course-v1.schema.json` (detalle en
`backend/docs/especificacion-cursos-v1.md`). Mismo enfoque que las cuentas
(`conexion-registro-app-backend.md`): puerto + adaptador HTTP con Ktor en `sharedLogic`, para que
cambiar a gRPC (u otra cosa) solo toque el adaptador.

Estado: **terminado** (2026-11-21). Decisiones aprobadas el 2026-11-20.

## Objetivo

1. **Buscar** cursos del servidor en la pestaña Buscar, por texto, con sus resultados.
2. **Abrir** un curso: se descarga y se ve su camino de módulos (dominado / frontera / bloqueado).
3. **Estudiar** un módulo como dice ahora la especificación (§4.3):
   - la parte formativa: lecciones y ejercicios intercalados (`steps`);
   - la **prueba de dominio**: se sortean `questions` ejercicios de la reserva (`review`) y, si se
     aciertan al menos `minCorrect`, el módulo queda dominado y se desbloquea lo que dependa de él.

## Lo que cambia respecto a lo que ya hay

| Hoy en la app | En el backend | Qué hacemos |
|---|---|---|
| `Concept` / «concepto» | `module` / «módulo» (la especificación ya dice módulo) | Renombrar a `Module` (decisión 1) |
| Un concepto tiene preguntas | Un módulo tiene `steps` (lecciones + ejercicios) y `review` (reserva) | Ampliar el modelo |
| Dominado = fijo en los datos de ejemplo | Dominado = superar la prueba de dominio | Lógica nueva + progreso guardado en memoria (decisión 3) |
| Opciones por índice (`correct = 1`) | Opciones con id (`"answer": "a"`) | El *mapper* pasa ids a índices; el resto de la app no cambia |
| Al fallar: «La respuesta era…» | Cada ejercicio trae `explanation` (Markdown) | Enseñar la explicación al comprobar |
| — | Lecciones (`lesson`, Markdown) | Pantalla de lección dentro de la práctica |

Los tres tipos de ejercicio (`singleChoice`, `multipleChoice`, `matching`) coinciden con los que
ya pinta la app: `AnswerOption` y `MatchingBoard` se reutilizan tal cual.

## Idea de diseño (código)

```
sharedLogic/commonMain/.../course/
├── Course, Module, Step (Lesson | Exercise), Mastery      modelo (ampliado)
├── CourseSummary                                          metadatos (id, versión, autor, etiquetas…)
├── mastery/MasteryTest                                    sortear de la reserva y aprobar o no
├── progress/ProgressStore + InMemoryProgressStore         qué módulos ha dominado el usuario
└── http/
    ├── CourseGateway (puerto)                             search(query, page) · course(id)
    ├── HttpCourseGateway (adaptador Ktor)                 GET /api/courses y /api/courses/{id}
    ├── CourseDtos + CourseDtoMappers                      JSON del backend → modelo
    └── FakeCourseGateway                                  para tests y previews
app/.../ui/screens/
├── ScreenSearch (+ ViewModel)                             búsqueda y resultados
├── course/  (cargar del gateway: cargando / error / camino)
└── exercise/ (lecciones, explicación y prueba de dominio)
```

- **Patrones** (los mismos de las cuentas, para que se defiendan igual):
  - Puerto/adaptador (*Adapter*): `CourseGateway` + `HttpCourseGateway`.
  - DTO + *mapper*: el JSON del backend nunca llega a la interfaz.
  - Resultados sellados en vez de excepciones: `Found | NotFound | Unreachable | ServerFailure`.
  - *Composition root*: `AppContainer`.
  - *Strategy*: `ProgressStore`, en memoria hoy y en Room mañana.

## Alcance

- Buscar por texto con paginación sencilla («Cargar más»).
- Descargar y mostrar un curso con su camino. Estados de carga, sin conexión y no encontrado.
- Estudiar un módulo: lecciones, ejercicios con su explicación y prueba de dominio.
- Progreso en memoria mientras la app está abierta: dominar un módulo desbloquea los siguientes
  en el camino.
- Los 4 códigos de error nuevos en `ApiErrorCode`.
- Markdown básico en lecciones y explicaciones: negrita, cursiva y código en línea (decisión 2).

## Fuera de alcance (otras tareas)

- **Guardar cursos y progreso en el móvil (Room / SQLite)** y estudiar sin conexión. Es otro
  requisito de la asignatura, así que irá en su propio documento: `ProgressStore` ya deja el hueco.
- **Publicar cursos desde la app** (Mis cursos → «Subir curso»: elegir fichero, validar,
  publicar). Usa `POST /api/courses/validate` y `POST /api/courses`.
- Filtros de la búsqueda (etiquetas, idioma), valoraciones y repasos SM-2.
- Comprobar el SHA-256 de la descarga: en `commonMain` no hay SHA-256 sin añadir una librería.
  Tendrá sentido al guardar el curso en el móvil (decisión 4).

## Decisiones para revisar

1. **Renombrar `Concept` → `Module`** en la app y en `sharedLogic`, también en textos y en la
   especificación §5. Recomiendo **sí**: la especificación, el backend y la API ya dicen «módulo»,
   y tener dos palabras para lo mismo confunde (y en la defensa, más). Es un cambio mecánico y se
   hace en un paso aparte, con los tests como red.
2. **Markdown sin librería nueva**: una función propia y pequeña que entiende `**negrita**`,
   `*cursiva*` y `` `código` `` y lo pasa a `AnnotatedString`; el resto se ve como texto. Recomiendo
   esto en vez de añadir una dependencia. Si más adelante hacen falta listas o tablas, se valora
   una librería.
3. **Progreso en memoria** (`InMemoryProgressStore`, como `InMemorySessionStore`): se pierde al
   cerrar la app, pero permite ver ya el camino desbloqueándose. Room llegará en su documento.
4. **Sin comprobar el SHA-256 por ahora** (ver «Fuera de alcance»). Alternativa: añadir ya una
   librería multiplataforma de hashes (hay que pedir permiso para la dependencia).
5. **El curso de ejemplo** (`DemoCourse.kt`): se queda **solo para previews y tests**. El botón
   provisional «Abrir curso de ejemplo» de Inicio se quita: los cursos se abren desde Buscar.
6. **Arreglos de documentación pendientes de la revisión del backend**: la §7 duplicada,
   `10.0.2.2` en `api-cursos.md` y «concepto»/«módulo» en la §5. Se hacen en la tarea 1.

## Tareas

### Parte A · Datos y red (`sharedLogic`, test primero)

1. ✅ **Hecha (2026-11-20).** **Renombrar a módulo + arreglos de documentación** (decisiones 1 y 6).
   - ✅ Criterio: compila, los 266 tests pasan y no queda «concepto» en el código ni en las pantallas.
2. ✅ **Hecha (2026-11-20).** **Modelo ampliado**.
   - `Course` (+ descripción, idioma, etiquetas, `defaults.mastery`) y `Module` (`steps`, `review`,
     `mastery`).
   - `Step` sellado: `Lesson(text)` o un ejercicio; `explanation` en cada pregunta.
   - `CourseSummary` con los metadatos de la búsqueda.
   - ✅ Criterio: tests del modelo; el camino y la práctica siguen funcionando.
3. ✅ **Hecha (2026-11-21).** **DTOs y *mapper*** del JSON del curso y de los metadatos.
   - Ids de opción → índices. Un `formatVersion` distinto de 1 o un tipo desconocido → error claro,
     sin romper la app.
   - ✅ Criterio: tests con el JSON real de `backend/src/test/resources/courses/valid/ejemplo-grafos.json`.
4. ✅ **Hecha (2026-11-21).** **`CourseGateway` + `HttpCourseGateway` + `FakeCourseGateway`**.
   - `search(query, page)` y `course(id)`, con resultados sellados.
   - Los 4 códigos nuevos en `ApiErrorCode`.
   - ✅ Criterio: tests con `MockEngine`: 200, página vacía, 404, 400, sin conexión y 5xx.
5. ✅ **Hecha (2026-11-21).** **Prueba de dominio y progreso**.
   - `MasteryTest`: sortea `questions` de la reserva (con semilla, para los tests) y aprueba con
     `minCorrect`. Usa el valor del módulo o, si no tiene, el del curso.
   - `ProgressStore` + `InMemoryProgressStore`.
   - ✅ Criterio: tests de sortear, aprobar justo, suspender y el valor por defecto del curso.

### Parte B · Pantallas (`app`)

6. ➡️ **No es nuestra: Buscar la hace el compañero** (igual que Ajustes y los filtros).
   - Lo que necesita ya está en `sharedLogic`: `CourseGateway.search(query, page)` →
     `SearchOutcome` con `CoursePage` (`hasMore` para «Cargar más») y `FakeCourseGateway` para
     previews; `AppContainer.courseGateway` ya existe. Al pulsar un resultado debe navegar a
     `Route.Course(courseId)`.
7. ✅ **Hecha (2026-11-21).** **Abrir un curso del servidor**.
   - `Route.Course(courseId)` carga del gateway: cargando → camino, o el error que toque.
   - El camino usa el progreso guardado.
   - ✅ Criterio: en el emulador, Buscar → curso → camino; atrás vuelve a Buscar.
8. ✅ **Hecha (2026-11-21).** **Estudiar un módulo**.
   - Lecciones con Markdown básico, ejercicios con su explicación y, al final, la prueba de
     dominio con su resultado («¡Módulo dominado!» o «Te faltó 1 para aprobar»).
   - Al aprobar, el camino se actualiza.
   - ✅ Criterio: en el emulador, se domina un módulo del curso de ejemplo del backend y se
     desbloquea el siguiente.
9. ✅ **Hecha (2026-11-21).** **Repaso final**: TalkBack, modo oscuro, especificación y `resumen-proyecto.md`.

## Progreso

- 2026-11-20: documento creado. Decisiones 1–6 aprobadas.
- 2026-11-20: **tarea 1 hecha**.
  - Renombrado en `app/` y `sharedLogic/`:
    - `Concept` → `Module`, `ConceptState` → `ModuleState`, `ConceptNode` → `ModuleNode`;
    - `conceptId` → `moduleId`, `concepts` → `modules`, `onConceptClick` → `onModuleClick`;
    - los textos de pantalla («módulo»).
  - Ficheros renombrados: `ModuleState.kt`, `ModuleStateTest.kt` y `ModuleNode.kt`.
  - 266 tests, compila para Android e iOS y lint limpio.
  - Especificación:
    - §5 dice «módulo»;
    - §7 ya no repite «emparejar» como tipo futuro (quedó así al fusionar): se explica junto a
      `matching` en el formato v1.
  - `backend/docs/api-cursos.md`: la base en desarrollo es `http://localhost:7070` con
    `adb reverse`, no `10.0.2.2`.
  - Nota en `camino-curso-y-ejercicios.md`: conserva los nombres antiguos porque es historia.
  - **Sin cambiar, pendiente de decidir:** dos textos de pantalla que vienen del Figma todavía
    dicen «concepto»:
    - Bienvenida: «Cada curso es un mapa de conceptos…» (`ScreenWelcome.kt`).
    - Registro: «cada concepto se desbloquea…» (`RegisterStepContent.kt`).
  - Decidido después: se quedan como están (es lenguaje de marketing).
- 2026-11-20: **tarea 2 hecha**.
  - `course/Course.kt`:
    - `Mastery` (se valida al crearla: `questions` ≥ 1 y `minCorrect` entre 1 y `questions`).
    - `Step` sellado (`Step.Lesson`, `Step.Exercise`).
    - `Module` con `steps`, `review`, `mastery` y `exercises`.
    - `Course` con `description`, `language`, `tags` y `defaultMastery`.
  - `course/CourseSummary.kt` (+ `Author`): los metadatos de la búsqueda.
  - Cambio de diseño: los ejercicios viven **dentro de su módulo**, como en el backend.
    - `Question` pierde `moduleId` y gana `explanation` (por defecto, vacía).
    - `Course` pierde la lista `questions`.
    - `questionsFor(moduleId)` devuelve los ejercicios de los `steps` del módulo.
  - `DemoCourse.kt` rehecho con la forma nueva, con lecciones y explicaciones (para las tareas 7–8).
  - 10 tests nuevos (`CourseModelTest`) y 1 quitado (el de la lista suelta de preguntas); 275 en
    total, todos pasan.

- 2026-11-21: **tarea 3 hecha**.
  - `course/http/CourseDtos.kt`: DTO de metadatos, página de búsqueda, versión con contenido y
    contenido v1. Pasos y ejercicios con polimorfismo de kotlinx.serialization (campo `type`).
  - `course/http/CourseDtoMappers.kt`:
    - `decodeCourseVersion(body)` → `CourseDecoding` sellado: `Decoded | UnsupportedFormat | Malformed`.
      Primero mira `formatVersion` y solo si es 1 lee el resto.
    - `decodeCoursePage(body)` → `CoursePage?`.
    - Ids de opción → índices; una respuesta que no es ninguna opción → `Malformed`.
  - `CoursePage` (+ `hasMore`, para «Cargar más») en `CourseSummary.kt`.
  - Test con el curso real: `ExampleCourseJson.kt` es una **copia** de `ejemplo-grafos.json` (en
    `commonTest` no se leen ficheros); si el del backend cambia, hay que volver a copiarlo.
  - 20 tests nuevos (`CourseDtoMappersTest`); 295 en total, todos pasan.

- 2026-11-21: **tarea 4 hecha**.
  - `course/CourseGateway.kt`: puerto con `search(query, page)` y `course(id)`, y sus desenlaces
    sellados `SearchOutcome` (`Found | Unreachable | ServerFailure`) y `CourseOutcome`
    (`Found | NotFound | UnsupportedFormat | Unreachable | ServerFailure`).
  - `course/http/HttpCourseGateway.kt`: adaptador Ktor; 20 resultados por página; sin texto no
    manda `q`; el id va como un solo trozo de la ruta (un `/` se manda como `%2F`).
  - `course/FakeCourseGateway.kt` (busca en título y descripción) y `course/CourseGateways.kt`
    (*factory* `http(baseUrl)`).
  - `ApiErrorCode`: `INVALID_PARAMETER`, `COURSE_NOT_FOUND`, `NOT_COURSE_AUTHOR`, `IDENTICAL_TO_LATEST`.
  - `callServer` de `HttpAccountGateway.kt` pasa de `private` a `internal` para reutilizarlo.
  - Un 400 en la búsqueda se trata como `ServerFailure`: solo pasa si la app pide una página imposible.
  - 22 tests nuevos (`HttpCourseGatewayTest`, `FakeCourseGatewayTest`); 317 en total, todos pasan.
  - Sin probar todavía contra el servidor real (estaba apagado): se hará en la tarea 6.

- 2026-11-21: **tarea 5 hecha**.
  - `course/mastery/MasteryTest.kt`:
    - `Course.masteryFor(module)`: la prueba del módulo o, si no tiene, la del curso.
    - `Course.masteryTest(moduleId, random)` (*factory*): sortea sin repetir de `review`; null si
      no hay reserva. Si la reserva fuese menor que la prueba, se usa entera.
    - `MasteryTest.start(random)` → `PracticeSession` (se responde con la práctica que ya hay).
    - `resultFor` / `resultOf` → `MasteryResult` sellado: `Passed | Failed(missing)`.
  - `course/progress/ProgressStore.kt` (puerto) + `InMemoryProgressStore.kt`: `mastered(courseId)`
    es un `Flow` (el camino se entera solo) y `markMastered(courseId, moduleId)`.
  - 18 tests nuevos (`MasteryTestTest`, `InMemoryProgressStoreTest`); 335 en total, todos pasan.

- 2026-11-21: la **tarea 6 (Buscar) pasa al compañero**. Se había empezado por error y se ha
  quitado entera (pantalla, ViewModel, componentes y la lógica `CourseSearch`). Se queda
  `AppContainer.courseGateway`, que la tarea 7 también necesita. 335 tests.

- 2026-11-21: **tarea 7, código hecho**.
  - `course/CachingCourseGateway.kt` (sharedLogic, patrón *Decorator*): guarda en memoria los
    cursos descargados; `cached(id)` los da sin esperar. Los fallos y las búsquedas no se guardan.
    4 tests nuevos; 339 en total.
  - `AppContainer`: `courseGateway` (con caché) y `progressStore` (`InMemoryProgressStore`).
  - `CourseViewModel(courseId, gateway, progress)`: descarga el curso y escucha el progreso.
    `CourseScreenState` = `Loading | Problem(CourseProblem) | Ready(CourseUiState)`; «Reintentar»
    solo sin conexión o con fallo del servidor.
  - `ScreenCourse` elige según el estado; `CourseStatusContent.kt`: descargando y problemas (con
    «atrás»), con previews.
  - La práctica (`Route.Exercise`) lee el curso de la caché; si no está, vuelve al camino.
  - Inicio, provisional: «Abrir curso del servidor» abre el más reciente (hasta que exista Buscar).
    Si no puede, dice por qué. `demoCourse` ya solo se usa en previews.
  - Compila sin avisos y lint sin avisos nuevos. El servidor local responde y su curso más
    reciente es «Introducción a los grafos».

- 2026-11-21: **tarea 8, código hecho**.
  - sharedLogic:
    - `text/Markdown.kt`: `parseMarkdown` (párrafos y listas `-` / `1.`) y `parseInline`
      (`**negrita**`, `*cursiva*`, `` `código` ``). Un `*` suelto se queda como texto.
    - `course/study/StepSession.kt`: la parte formativa (`StepState.Reading | Answering`).
    - `course/study/ModuleStudy.kt`: máquina de estados `Learning → TestIntro → Testing →
      Finished` (o `Done` sin reserva). Al acabar los pasos se sortea la prueba; «Repetir la
      prueba» sortea otra sin volver a las lecciones; fallar en la parte formativa no frena.
    - 25 tests nuevos (`MarkdownTest`, `ModuleStudyTest`); 364 en total.
  - app:
    - `MarkdownText` (componente): bloques → `Text` con `AnnotatedString`.
    - `ExerciseViewModel` usa `ModuleStudy` y, **al aprobar**, `progressStore.markMastered`: el
      camino lo escucha y desbloquea lo siguiente.
    - `ScreenExercise` pinta cada etapa; `ExerciseParts`: explicación tras comprobar (desplazable),
      `ContinueBar`, `CloseBar` y `StudyMessage` (entrada a la prueba, resultado, fin sin prueba;
      sustituye a `PracticeSummary`). Previews por etapa en `ScreenExercisePreviews.kt`.
    - `DemoCourse`: «grafos» tiene reserva y prueba (3 preguntas, 2 aciertos) para las previews.
  - Compila sin avisos y lint sin avisos nuevos.

- 2026-11-21: **tareas 7 y 8 probadas en el emulador** contra el servidor local, con el curso
  «Introducción a los grafos»: descarga, camino, lecciones, ejercicios (también emparejar),
  prueba de dominio y desbloqueo del módulo siguiente. La prueba final la hizo el usuario.

- 2026-11-21: **tarea 9, documentos y revisión**.
  - `especificacion.md`: §4 (el progreso hoy va en memoria) y §5 (abrir un curso del servidor y
    sus errores; estudiar un módulo: lecciones, explicación, prueba de dominio y resultado).
  - `resumen-proyecto.md` al día (2026-11-21): §1, §3.4 camino y ejercicios, §3.5 cursos del
    servidor, contrato de cursos, §4.5 el viaje de un curso, conceptos de Kotlin y Compose,
    patrones (Decorator, Strategy, máquinas de estados, funciones puras), 364 tests, preguntas y límites.
  - Accesibilidad (revisión del código): iconos decorativos sin descripción y los que significan
    algo, con ella. Dos mejoras: el título de cada lección es encabezado y cada elemento de lista
    se lee entero.
- 2026-11-21: **tarea 9 terminada en el emulador**.
  - Modo oscuro y claro, con el curso del servidor: camino → lecciones → ejercicio con explicación
    → emparejar → «Prueba de dominio» → «Te faltaron 3» → «Repetir la prueba» → «¡Módulo dominado!»
    → BFS pasa a frontera.
  - `androidTest/.../StudyAccessibilityTest.kt` (6 tests, en el emulador): encabezados, «Paso 1 de 2»
    y «Pregunta 1 de 1», avisos que se leen solos, estado de cada opción y de cada módulo, y zonas
    táctiles de 48 dp. Encontró dos fallos de verdad, ya arreglados:
    - La ✕ de la práctica medía 40 dp (en esta versión de Material 3 `IconButton` es más pequeño):
      ahora 48 dp.
    - `BackButton` medía 44 dp (se creía que el `Surface` pulsable llegaba a 48 y no): ahora el
      círculo sigue siendo de 44 dp, dentro de una zona táctil de 48. Afecta también a registro e
      inicio de sesión (se ven igual).
  - «al menos 3» ya no deja el número solo en otra línea.
  - **Sin cambiar (no es de este documento):** el ojo de la contraseña (`PasswordField`) es un
    `IconButton` y probablemente también mide 40 dp.

## Siguiente paso

Documento terminado. Lo siguiente, en otros documentos: guardar cursos y progreso en el móvil
(Room), quitar el botón provisional de Inicio cuando exista Buscar (compañero) y, si se hace,
Mis cursos (necesita una ruta nueva en el backend).
