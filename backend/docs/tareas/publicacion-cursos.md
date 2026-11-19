# Tarea — Publicación de cursos (backend)

Rama: `dev/courses-management`. Proyecto: `backend/` (Maven, Java 25, Javalin 7, PostgreSQL en Docker).
Especificación: [`backend/docs/especificacion-cursos-v1.md`](../especificacion-cursos-v1.md) (la estructura
la define `src/main/resources/schemas/course-v1.schema.json`; la semántica, la especificación).

## Objetivo

Que los instructores puedan validar y publicar cursos en formato v1 (JSON), con versiones
inmutables, y que la app pueda buscarlos y descargarlos con los mismos bytes que se publicaron.

## Decisiones (aprobadas el 2026-11-19)

- **Dependencia nueva:** `com.networknt:json-schema-validator:2.0.8` (Apache 2.0). Es la última
  rama basada en Jackson 2 (compilada con 2.22.1); la 3.x usa Jackson 3 (`tools.jackson`) y no
  convive con el Jackson 2 de Javalin. Se queda nuestra `jackson-databind` 2.22.4. Si la librería
  funciona sin YAML, se excluye `jackson-dataformat-yaml` (y snakeyaml).
- **Límite de cuerpo en Javalin 7.2.4:** `config.http.maxRequestSize` (`long`), 1 000 000 bytes por
  defecto. Al superarlo, Javalin lanza `ContentTooLargeResponse` (413). Se fija en 2 097 152 y se
  traduce al informe con `PAYLOAD_TOO_LARGE`.
- **Arquitectura hexagonal**, igual que las tareas anteriores: dominio puro, puertos en
  `application`, adaptadores en `infrastructure`, `App` como raíz de composición e inyección por
  constructor. Subpaquetes `course` en cada capa.
- **Patrones previstos:**
  - Dominio: records inmutables y tipo algebraico `sealed interface Step` ← `Lesson` y
    `sealed interface Exercise` ← `SingleChoice`, `MultipleChoice`, `Matching` (así `review` es
    `List<Exercise>`); *factory methods* en `Issue`; `JsonPointer` inmutable construido por pasos.
  - Reglas: **Strategy** (cada regla es una función `CourseDocument → Stream<Issue>`),
    **Composite** (cada capa agrupa sus reglas) y **Pipeline / Chain of Responsibility** (se para
    si una capa da errores; las capas 4 y 5 corren juntas).
  - Lector: **Adapter** (capa anticorrupción) que implementa el puerto `CourseReader`.
  - Persistencia: **Execute Around** para la transacción (`commit`/`rollback` en un solo sitio).
  - Web: se reutiliza el **Decorator** `RequireSession`; DTOs con *factory* `from(...)`.

## Decisiones del paso 2 (aprobadas el 2026-11-19)

- **Avisos de dominio callados (opción A).** `MASTERY_POOL_SMALL_FOR_RETRY` y `MASTERY_GUESSABLE` de
  un módulo se omiten si su prueba efectiva da `MASTERY_MIN_EXCEEDS_QUESTIONS` o el módulo da
  `MASTERY_POOL_TOO_SMALL`. Los errores se informan siempre; `MODULE_WITHOUT_LESSON`,
  `MODULE_WITHOUT_EXERCISE` y `MATCHING_TOO_MANY_PAIRS` también, porque no dependen de la prueba.
  Motivo: con la opción B un fixture de `MASTERY_POOL_TOO_SMALL` daría siempre también
  `MASTERY_POOL_SMALL_FOR_RETRY` (reserva < q < 2q) y no podría producir «exactamente» su código (§6).
- **Orden de documento.** Los problemas se ordenan por su ruta: índices de menor a mayor y claves en
  el orden del schema. A igual ruta, en el orden en que los producen las reglas.
- **`REMOVED_IDS`.** Un único aviso en la raíz (`""`) con la lista de ids eliminados.
- **División:** 2a (base y capa 3), 2b (grafo), 2c (dominio y pedagogía), 2d (validador y
  `compareVersions`), en el paquete `domain/course/rules`.

## Alcance

- Validador del formato v1 (capas 1–5 de la especificación, §3) con informe de errores y avisos.
- Persistencia de cursos y versiones inmutables (`004_create_courses.sql`, §4.1–4.2).
- Endpoints (§4.3): `POST /api/courses/validate`, `POST /api/courses`,
  `POST /api/courses/{id}/versions`, `GET /api/courses`, `GET /api/courses/{id}` y
  `GET /api/courses/{id}/versions/{version}`.
- Aviso `REMOVED_IDS` al publicar una versión que quita ids (§4.6).
- Límite de 2 MiB con respuesta en el formato del informe (§4.7).

## Fuera de alcance

Ver §8 de la especificación: progreso de los alumnos, imágenes y `.zip`, otros tipos de ejercicio,
editar o borrar cursos, valoraciones, pool de conexiones, errores 500 en JSON, tests automáticos de
repositorios y endpoints, y portar las reglas a la app Android.

## Tareas

- [x] **0. Preparación.** Este documento; copiar la especificación, el schema y el ejemplo al
  repositorio; comprobar Javalin 7 y la librería de JSON Schema.
  *Acepta si:* los tres ficheros están en las rutas de la especificación y son idénticos a los
  originales.
- [x] **1. Dominio del curso** (paquete `domain/course`). Records (`CourseDocument`, `Module`,
  `Mastery`, `sealed interface Step` con `Lesson`, `SingleChoice`, `MultipleChoice`, `Matching`),
  normalización de texto, `Issue`, `CourseIssueCode` y `JsonPointer`. Tests primero.
  *Acepta si:* los records rechazan en el constructor lo que el schema ya prohíbe (por ejemplo,
  listas vacías), y los tests de normalización pasan.
- [x] **2. Reglas de validación** (capas 3–5) y `compareVersions`, como funciones puras.
  *Acepta si:* hay al menos un test por código de las capas 3–5, los cuatro valores de la cota
  binomial coinciden y el mensaje de `CYCLE` muestra el ciclo concreto.
- [ ] **3. Lector** (capas 1–2): BOM, UTF-8 estricto, Jackson con detección de claves duplicadas,
  schema y mapeo a records. Informe completo. Añade la dependencia aprobada.
  *Acepta si:* el ejemplo es válido y sin avisos, hay un fixture por cada código de §3.3 con su
  ruta, y un `title` con salto de línea final se rechaza.
- [ ] **4. Casos de uso**: `ValidateCourse`, `PublishCourse`, `PublishCourseVersion`, `FindCourse`
  y `SearchCourses`, con el puerto `CourseRepository` y un repositorio en memoria para los tests.
  *Acepta si:* los tests cubren cada caso de cada resultado sellado, y `REMOVED_IDS` aparece al
  publicar una versión que quita ids.
- [ ] **5. Infraestructura**: `004_create_courses.sql` y `JdbcCourseRepository` con transacción y
  `FOR UPDATE`.
  *Acepta si:* el SQL se prueba a mano según §6 (incluida la concurrencia con dos sesiones de
  `psql`) y `./gradus.sh db` aplica el `004` sobre la base existente.
- [ ] **6a. Web: validación y publicación.** `CourseController.validate`, `publish` y
  `publishVersion`, DTOs del informe y de los metadatos, códigos `COURSE_NOT_FOUND`,
  `NOT_COURSE_AUTHOR` e `IDENTICAL_TO_LATEST`, límite de 2 MiB y carpeta de Yaak
  «Cursos · Publicación».
  *Acepta si:* desde Yaak se obtienen 200, 201, 400, 401, 403, 404, 409, 413 y 422.
- [ ] **6b. Web: consulta y búsqueda.** `CourseController.search`, `latest` y `version`, código
  `INVALID_PARAMETER` y carpeta de Yaak «Cursos · Consulta».
  *Acepta si:* desde Yaak se obtienen 200, 400 y 404, y el SHA-256 del contenido devuelto coincide
  con el publicado.

## Progreso

- 2026-11-19: plan aprobado (rama `dev/courses-management`, networknt 2.0.8).
- 2026-11-19: paso 0 hecho. Especificación, schema y ejemplo copiados al repositorio.
- 2026-11-19: `docs/especificacion.md` alineada con el formato v1: el grafo es de módulos (no de
  conceptos), el dominio se decide con la prueba de dominio (no con «4 de las 5 últimas») y los
  tipos de pregunta de v1 son `singleChoice`, `multipleChoice` y `matching` (el resto, más
  adelante). Imágenes y etiquetas cerradas siguen como decisiones abiertas. Ktor → Javalin en
  `especificacion.md` de la raíz.
- 2026-11-19: paso 1 dividido en 1a (piezas base), 1b (pasos y ejercicios) y 1c (módulo y curso).
  Decisiones: la gravedad la fija `CourseIssueCode`; los constructores solo rechazan lo que el schema
  ya prohíbe; `Step` y `Exercise` sellados; `Module.mastery` es `Optional<Mastery>`.
- 2026-11-19: paso 1a hecho (193 tests en verde, 18 nuevos; sin avisos de compilación).
  `TextNormalizer`, `JsonPointer`, `Severity`, `CourseIssueCode` (25 códigos, incluidos
  `PAYLOAD_TOO_LARGE` y `REMOVED_IDS`) e `Issue`.
- 2026-11-19: paso 1b hecho (235 tests en verde, 42 nuevos; sin avisos de compilación). `Step` y
  `Exercise` sellados, `Lesson`, `SingleChoice`, `MultipleChoice`, `Matching`, `Option`,
  `MatchingPair` y el ayudante `Require` (paquete privado). Lo que solo detectan las reglas de la
  capa 3 (respuesta inexistente, todas correctas, lados repetidos) se puede construir a propósito.
  El `\S` del Markdown no usa `UNICODE_CHARACTER_CLASS`, para no ser más estricto que el schema.
- 2026-11-19: paso 1c hecho (279 tests en verde, 44 nuevos; sin avisos de compilación). `Mastery`,
  `Module` (con `effectiveMastery`) y `CourseDocument`; `Require` gana `ids`, `plainText`,
  `language` y `range`. En los tests, `TestCourses` (factory methods) y los builders `CourseBuilder`
  y `ModuleBuilder`: `course().build()` es un curso válido pensado para no dar avisos (se
  comprobará con las reglas del paso 2). **Paso 1 completado.**
- 2026-11-19: paso 2a hecho (301 tests en verde, 22 nuevos; sin avisos de compilación). Paquete
  `domain/course/rules`: `CourseRule` (Strategy + `all` como Composite), `IdentityRules`
  (`DUPLICATE_ID`, `DUPLICATE_TAG`), `ExerciseRules` (`DUPLICATE_OPTION_ID`, `DUPLICATE_OPTION_TEXT`,
  `UNKNOWN_OPTION`, `ALL_OPTIONS_CORRECT`, `DUPLICATE_MATCHING_SIDE`) y `PrerequisiteRules`
  (`UNKNOWN_PREREQUISITE`, `SELF_PREREQUISITE`). Ayudantes de paquete: `Located`, `Locations`
  (recorridos con su ruta) y `Duplicates` (repeticiones por clave). La ruta de `DUPLICATE_ID` es la
  del objeto que lleva el id (`/modules/1/steps/0`), como la de `DUPLICATE_OPTION_ID`. Los builders
  de test pasan a ser públicos para usarlos desde `rules`.
- 2026-11-19: paso 2b hecho (314 tests en verde, 13 nuevos; sin avisos de compilación).
  `PrerequisiteGraph` (inmutable): Kahn con cola, `findCycle` según §3.5 (primer módulo restante y
  primer prerrequisito restante) y `path` (recorrido en anchura, camino más corto). `GraphRules`:
  `CYCLE` con el mensaje exacto de §3.5 en el módulo donde empieza el ciclo, y
  `REDUNDANT_PREREQUISITE` (solo sin ciclos) citando el camino alternativo, por ejemplo
  «(c requiere b, que requiere a)».
- 2026-11-19: paso 2c hecho (337 tests en verde, 23 nuevos; sin avisos de compilación).
  `GuessingOdds` (probabilidad por tipo de ejercicio y cota binomial; los cuatro valores de §3.6 con
  tolerancia 1e-12), `MasteryRules` (los cinco códigos `MASTERY_*`, con la opción A) y
  `PedagogyRules` (`MODULE_WITHOUT_LESSON`, `MODULE_WITHOUT_EXERCISE`, `MATCHING_TOO_MANY_PAIRS`).
  La cota se muestra en porcentaje con un decimal y coma decimal («31,3 %»). Los ejercicios de
  `review` no cuentan como ejercicios formativos.
- 2026-11-19: paso 2d hecho (358 tests en verde, 21 nuevos; sin avisos de compilación). En
  `domain/course`: `DocumentOrder` (comparador de rutas: índices numéricos, contenedor antes que
  contenido, claves en el orden del schema) y `CourseReport` (inmutable, siempre ordenado, `isValid`,
  `errors`, `warnings`, `with`). En `rules`: `CourseValidator` (etapas `IDENTITY_AND_REFERENCES` y
  `GRAPH_AND_MASTERY`; se para tras la primera con errores) y `VersionComparison.compareVersions`
  (`REMOVED_IDS` en la raíz, ids en el orden de la versión anterior; los ids de las opciones no
  cuentan). El curso por defecto de los tests es válido y sin avisos. **Paso 2 completado:** hay
  tests para los 19 códigos de las capas 3–5, los cuatro valores de la cota y el ciclo concreto.

## Pendiente para más adelante

- ~~Duda sobre `MASTERY_POOL_TOO_SMALL`~~: resuelta el 2026-11-19 (opción A, ver «Decisiones del
  paso 2»).

## Siguiente paso

Paso 3: lector (capas 1–2). Antes, añadir la dependencia aprobada al `pom.xml`, comprobar su API
real en el jar y si funciona sin YAML, y proponer cómo dividir el paso.
