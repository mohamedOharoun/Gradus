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
  defecto. Se fija en 2 097 152 y se traduce al informe con `PAYLOAD_TOO_LARGE`. Corrección del
  2026-11-20: al leer el cuerpo, Javalin lanza un `HttpResponseException` genérico con estado 413, **no**
  la subclase `ContentTooLargeResponse` (comprobado en su código y con una petición real).
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

## Decisiones del paso 3 (aprobadas el 2026-11-19)

- **API de networknt 2.0.8** (comprobada en el jar): `SchemaRegistry.withDefaultDialect(DRAFT_2020_12, …)`,
  `getSchema(InputStream)` y `Schema.validate(JsonNode)` → `List<Error>`; la ruta sale de
  `Error.getInstanceLocation()` (`NodePath` con claves e índices). Motor de expresiones regulares del
  JDK (`JDKRegularExpressionFactory`): soporta el *lookahead* y su `\S` es el mismo que el de
  `Require`. Mensajes en español con `locale(es)`. YAML solo se usa en `YamlMapperFactory$Holder`
  (carga perezosa), así que se excluye `jackson-dataformat-yaml`.
- **`DUPLICATE_KEY`** apunta al objeto afectado con `parser.getParsingContext().pathAsPointer()`.
- **Fixtures (opción A):** cada fixture es un JSON con `expected` (código y ruta) y un `patch`
  (JSON Patch, RFC 6902: `add`, `replace`, `remove`) que se aplica al ejemplo. Los de la capa 1
  (`INVALID_JSON`, `DUPLICATE_KEY`) se generan en el test cambiando el texto del ejemplo.
- **División:** 3a (dependencia y `CourseSchema`), 3b (capa 1), 3c (puerto `CourseReader` y lector
  completo con el mapeo a records), 3d (fixtures).

## Decisiones del paso 4 (aprobadas el 2026-11-19)

- **Dominio de la publicación** en `domain/course`: `CourseAuthor`, `CourseMetadata` (derivado del
  JSON con `of(document, content)`), `CourseVersionDraft`, `CourseSummary` (sin contenido),
  `CourseVersion` (con contenido), `PageRequest`, `Page<T>` y `CourseSearch`.
- **Puerto `CourseRepository`:** `create`, `appendVersion` (resultado sellado `Appended` |
  `CourseNotFound` | `NotAuthor` | `IdenticalToLatest`), `findLatest`, `findVersion` y `search`.
- **Casos de uso:** `CourseInspector` (Facade sobre lector y validador, compartido), `ValidateCourse`,
  `PublishCourse`, `PublishCourseVersion`, `FindCourse` y `SearchCourses`, con resultados sellados.
- **Orden en `PublishCourseVersion` (opción A):** primero el recurso y el permiso (404, 403), después
  el cuerpo (413, 400, 422); `appendVersion` vuelve a comprobarlo todo de forma atómica.
- **`searchText`** = título + espacio + descripción, normalizados (§3.4).
- **Búsqueda:** `q` y `tag` se normalizan y los vacíos se ignoran; `language` exacto; solo `page` y
  `size` dan `INVALID_PARAMETER` (también si no son números).
- **Escapado de `q` para `LIKE`:** en el paso 5, junto al repositorio JDBC.
- **División:** 4a (dominio de la publicación), 4b (`CourseInspector` y `ValidateCourse`),
  4c (puerto, repositorio en memoria y publicación), 4d (`FindCourse` y `SearchCourses`).
- **Etiquetas en la búsqueda (confirmado):** Y lógico. Cada etiqueta pedida debe estar en el curso;
  al curso le pueden sobrar etiquetas. Más chips de etiqueta estrechan los resultados.

## Decisiones del paso 5 (aprobadas el 2026-11-19)

- **Paquete:** `infrastructure/persistence`, junto a los otros repositorios JDBC, para reutilizar
  `Timestamps`.
- **Piezas:** `Transactions` (Execute Around: una conexión, `autoCommit` desactivado, *commit* o
  *rollback*), `LikePattern` (escapado de `%`, `_` y `\`, con test), `CourseSearchSql` (Builder del
  `WHERE` de la búsqueda, con test) y `JdbcCourseRepository`.
- **Pruebas a mano** en un contenedor desechable `gradus-pg-scratch` (`postgres:18-alpine`, puerto
  55432, sin volumen). Permiso para crearlo y borrarlo, y para ejecutar `./gradus.sh db` sobre la base
  de desarrollo al terminar el 5a; nada más se modifica sin permiso previo.
- **División:** 5a (migración y pruebas del SQL), 5b (`LikePattern` y `CourseSearchSql`),
  5c (`Transactions` y escritura con concurrencia), 5d (lectura y búsqueda).

## Decisiones del paso 6 (aprobadas el 2026-11-19)

- **Dos controladores** en `web/course`: `CoursePublicationController` (6a: validar y publicar) y
  `CourseCatalogController` (6b: consulta y búsqueda). Las rutas, las de §4.3.
- **DTO** con *factory* `from(...)` y test del JSON exacto: `IssueResponse`, `CourseReportResponse`,
  `AuthorResponse`, `CourseResponse` y `PublishedCourseResponse`.
- **Límite de 2 MiB:** `config.http.maxRequestSize`; el controlador captura `ContentTooLargeResponse` al
  leer el cuerpo y responde 413 con el informe. El mensaje de `PAYLOAD_TOO_LARGE` vive en el dominio.
- **Yaak:** cuerpos con un curso compacto; el ejemplo completo solo en la petición que comprueba los
  bytes (6b); el caso de más de 2 MiB, con `curl` y un fichero en `/tmp`.
- **Permiso** para arrancar el servidor contra la base de desarrollo y crear en ella cursos, versiones y
  una segunda cuenta de prueba (para el 403).
- **División:** 6a1 (códigos, retoque del dominio y DTO), 6a2 (controlador, límite y `App`),
  6a3 (Yaak «Cursos · Publicación»), y después 6b.

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
- [x] **3. Lector** (capas 1–2): BOM, UTF-8 estricto, Jackson con detección de claves duplicadas,
  schema y mapeo a records. Informe completo. Añade la dependencia aprobada.
  *Acepta si:* el ejemplo es válido y sin avisos, hay un fixture por cada código de §3.3 con su
  ruta, y un `title` con salto de línea final se rechaza.
- [x] **4. Casos de uso**: `ValidateCourse`, `PublishCourse`, `PublishCourseVersion`, `FindCourse`
  y `SearchCourses`, con el puerto `CourseRepository` y un repositorio en memoria para los tests.
  *Acepta si:* los tests cubren cada caso de cada resultado sellado, y `REMOVED_IDS` aparece al
  publicar una versión que quita ids.
- [x] **5. Infraestructura**: `004_create_courses.sql` y `JdbcCourseRepository` con transacción y
  `FOR UPDATE`.
  *Acepta si:* el SQL se prueba a mano según §6 (incluida la concurrencia con dos sesiones de
  `psql`) y `./gradus.sh db` aplica el `004` sobre la base existente.
- [x] **6a. Web: validación y publicación.** `CourseController.validate`, `publish` y
  `publishVersion`, DTOs del informe y de los metadatos, códigos `COURSE_NOT_FOUND`,
  `NOT_COURSE_AUTHOR` e `IDENTICAL_TO_LATEST`, límite de 2 MiB y carpeta de Yaak
  «Cursos · Publicación».
  *Acepta si:* desde Yaak se obtienen 200, 201, 400, 401, 403, 404, 409, 413 y 422.
- [x] **6b. Web: consulta y búsqueda.** `CourseController.search`, `latest` y `version` y carpeta de
  Yaak «Cursos · Consulta» (el código `INVALID_PARAMETER` ya existe desde el paso 4d).
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
- 2026-11-19: paso 3a hecho (364 tests en verde, 6 nuevos; sin avisos de compilación). Dependencia
  `json-schema-validator` 2.0.8 en el `pom.xml` sin `jackson-dataformat-yaml`; el árbol resuelve
  Jackson 2.22.4, `itu` 1.14.0 y nada de YAML, y todo funciona sin él. `infrastructure/course/CourseSchema`
  (Adapter): compila el schema del classpath una vez y convierte cada `Error` en `SCHEMA_VIOLATION`
  con su ruta. Los mensajes de los patrones (`id`, texto plano, Markdown, idioma) se sustituyen por
  una explicación legible con el valor encontrado; el resto usa los mensajes en español de la
  librería. El ejemplo cumple el schema y un `title` con salto de línea final se rechaza.
- 2026-11-19: paso 3b hecho (380 tests en verde, 16 nuevos; sin avisos, también con
  `showDeprecation`). `domain/course/CourseContent` (el texto del autor; `sha256()` y `sizeBytes()`
  se calculan del propio texto, entre 1 byte y 2 MiB). En `infrastructure/course`: `Utf8Text` (quita el
  BOM y decodifica con `REPORT`, con la posición del primer byte inválido), `SyntaxCheck` (sellado:
  `Passed(content, tree)` | `Failed(issue)`) y `CourseJson` (mapper propio con
  `STRICT_DUPLICATE_DETECTION` y `FAIL_ON_TRAILING_TOKENS`; `INVALID_JSON` con línea y columna,
  `DUPLICATE_KEY` en el objeto que repite la clave, cuerpo vacío y `PAYLOAD_TOO_LARGE` si supera
  2 MiB). Comportamiento de Jackson 2.22 comprobado antes con `jshell`.
- 2026-11-19: paso 3c hecho (391 tests en verde, 11 nuevos; sin avisos). Puerto
  `application/course/CourseReader` y resultado sellado `CourseReadResult`: `Parsed(course, content)`,
  `TooLarge` (413), `Unreadable` (capa 1, 400) e `Invalid` (capa 2, 422). `JacksonCourseReader`
  (Adapter, recibe `CourseSchema` por constructor): `UNSUPPORTED_FORMAT_VERSION` antes del schema
  (solo si `formatVersion` es un entero, también `2.0`, distinto de 1; un `"1"` lo decide el schema).
  `CourseNodeMapper`: mapeo manual con una factoría por `type` para los ejercicios. El ejemplo se lee
  y pasa `CourseValidator` sin problemas; el contenido guardado se vuelve a leer igual.
- 2026-11-19: paso 3d hecho (423 tests en verde, 32 nuevos; sin avisos). 21 fixtures en
  `src/test/resources/courses/fixtures/<CÓDIGO>.json` (descripción, `expected` y `patch`), uno por
  código de las capas 2–5; los de la capa 1 (`INVALID_JSON` por JSON roto y por UTF-8 inválido,
  `DUPLICATE_KEY`) se generan en el test cambiando el texto del ejemplo. `JsonPatch` de test (`add`,
  `replace`, `remove`, `copy`) con sus propios tests. `CourseFixturesTest` comprueba que cada fixture
  produce exactamente su código en su ruta, que el catálogo entero está cubierto y que cada fichero
  se llama como su código. **Paso 3 completado.**
- 2026-11-19: paso 4a hecho (448 tests en verde, 25 nuevos; sin avisos). En `domain/course`:
  `CourseAuthor`, `CourseMetadata` (`of(document, content)`: etiquetas y texto de búsqueda
  normalizados, SHA-256 y tamaño del contenido), `CourseVersionDraft` (trunca a microsegundos y
  exige que los metadatos describan su contenido), `CourseSummary` (versión ≥ 1), `CourseVersion`
  (el contenido debe coincidir con su SHA-256), `PageRequest` (página ≥ 0, tamaño 1–100, 20 por
  defecto), `Page<T>` (con `map`) y `CourseSearch` (Specification: `of` normaliza y descarta vacíos;
  `matches` define la búsqueda). En los tests, `PublishedCourses` reúne las piezas de ejemplo.
- 2026-11-19: paso 4b hecho (459 tests en verde, 11 nuevos; sin avisos). En `application/course`:
  `Inspection` (sellado: `Accepted(course, content, report)` solo con avisos | `TooLarge` |
  `Unreadable` | `Rejected`; todos exponen `report()`), `CourseInspector` (Facade: lector y, si lee,
  `CourseValidator`), `CourseValidationResult` (`Checked` | `TooLarge`) y `ValidateCourse`. En los
  tests el lector es una lambda (`body -> result`): la aplicación se prueba sin Jackson ni schema.
- 2026-11-19: paso 4c hecho (477 tests en verde, 18 nuevos; sin avisos). Puerto `CourseRepository`
  (`create`, `appendVersion`, `findLatest`, `findVersion`; `search` llegará en el 4d) y
  `AppendVersionResult`. `Publication` reúne los desenlaces; cada record implementa
  `PublishResult` (curso nuevo) y/o `PublishVersionResult` (versión nueva), así que publicar un
  curso nuevo nunca puede dar 403, 404 ni 409. `PublishCourse` recibe un `CourseAuthor` (lo creará
  la web a partir del usuario de la sesión) y `PublishCourseVersion` el id de quien publica; aplica
  la opción A y añade `REMOVED_IDS` leyendo la versión anterior solo con el lector (si no se puede
  leer, no se compara). Dominio: `CourseVersion.of(courseId, version, author, draft)`. En los tests,
  `InMemoryCourseRepository` (métodos `synchronized` en lugar del `FOR UPDATE`) y `FakeCourseReader`
  (cada texto se lee como el curso que se le asigna).
- 2026-11-19: paso 4d hecho (489 tests en verde, 12 nuevos; sin avisos). `CourseRepository.search`
  (últimas versiones, `CourseSearch.matches`, orden `publishedAt` descendente y después id del curso;
  en memoria, el id se compara como texto para coincidir con el orden de los UUID en PostgreSQL).
  `FindCourse` (`latest` y `version`; una versión menor que 1 no consulta el repositorio).
  `SearchCourses` con `SearchCoursesQuery` (parámetros como texto) y `SearchResult` (`Found` |
  `Rejected(List<ValidationError>)`): `page` y `size` vacíos toman los valores por defecto; si no son
  enteros o están fuera de rango, `INVALID_PARAMETER` (se informan los dos a la vez con
  `Result.combine`). `ErrorCode.INVALID_PARAMETER` se adelanta del paso 6b al 4d para reutilizar el
  formato de errores del registro; `ErrorCodeTest` actualizado. **Paso 4 completado.**
- 2026-11-19: paso 5a hecho (489 tests en verde, sin cambios en Java). `db/init/004_create_courses.sql`,
  idéntico byte a byte al SQL de §4.1. Probado a mano en `gradus-pg-scratch` (PostgreSQL 18.7,
  `server_encoding` UTF8): aplicar el `004` dos veces no tiene efecto (solo avisos «already exists,
  skipping»); 19 comprobaciones en OK: las 8 `CHECK` (con 13 casos), la clave primaria, las dos claves
  ajenas, borrar un usuario con cursos falla por `courses_author_id_fkey`, y `VARCHAR(120)` admite 120
  emojis (480 bytes). `./gradus.sh db` aplicó solo el `004` sobre la base de desarrollo de Mohamed
  (5 usuarios intactos, UTF8); falta aplicarlo en la de Joel. La concurrencia y la búsqueda se
  prueban en el 5c y el 5d.
- 2026-11-19: paso 5b hecho (501 tests en verde, 12 nuevos; sin avisos). En `infrastructure/persistence`:
  `LikePattern` (escapa primero `\` y después `%` y `_`; `containing` lo envuelve en `%…%`),
  `SqlParameter` (sellado: `Text` | `TextArray`), `SqlWhere` (inmutable; `NONE` y `and` forman un
  monoide) y `CourseSearchSql` (traduce `CourseSearch` a `v.search_text LIKE ? ESCAPE '\'`,
  `v.search_tags @> ?` y `v.language = ?`, solo con los filtros presentes). Comprobado también en
  `gradus-pg-scratch`: los patrones escapados toman `%`, `_` y `\` literalmente y `@>` exige todas las
  etiquetas.
- 2026-11-19: paso 5c hecho (504 tests en verde, 3 nuevos; sin avisos). `Transactions` (Execute Around:
  `autoCommit` desactivado, commit, rollback ante `SQLException` o `RuntimeException`, el fallo del
  rollback se adjunta como *suppressed*; probado sin base de datos con conexiones `Proxy`).
  `JdbcCourseRepository.create` (dos `INSERT` en una transacción) y `appendVersion` (`SELECT … JOIN
  users … FOR UPDATE OF c`, autor, SHA-256 de la última versión, `INSERT` y `UPDATE latest_version`).
  Los métodos de lectura lanzan `UnsupportedOperationException` hasta el 5d. Probado a mano en
  `gradus-pg-scratch`: con dos sesiones de `psql`, el `FOR UPDATE` de la segunda espera 2 s hasta el
  commit de la primera y después ve `latest_version` = 2; con `jshell` sobre el ejemplo real, `create`,
  `Appended`, `IdenticalToLatest`, `NotAuthor` y `CourseNotFound`, y dos publicaciones simultáneas desde
  dos hilos dan las versiones 2 y 3 sin choques. En la base de datos, el SHA-256 del `content` coincide
  con la columna `sha256` y `octet_length(content)` con `size_bytes`.
- 2026-11-19: paso 5d hecho (505 tests en verde, 1 nuevo; sin avisos). `Transactions.inSnapshot`
  (solo lectura y `REPEATABLE READ`: el total y la página de una búsqueda ven los mismos datos; si no,
  un curso publicado entre las dos consultas daría más resultados que el total y `Page` lo rechazaría).
  `CourseRows` (columnas, `FROM` con los `JOIN` y conversión de filas; no lee el contenido para los
  resúmenes). `JdbcCourseRepository`: `findLatest`, `findVersion` y `search` (`COUNT` + página con
  `ORDER BY v.published_at DESC, c.id LIMIT ? OFFSET ?`); ya no queda ningún método provisional.
  Probado con `jshell` en `gradus-pg-scratch`, 13 búsquedas en OK: `%` literal, texto normalizado,
  etiquetas (todas), idioma, solo últimas versiones y paginación con tres cursos a la misma hora (en
  orden de id, sin repetir ni saltar). Lecturas de la última versión y de una concreta, inexistentes
  vacías y contenido idéntico al publicado. Contenedor desechable borrado. **Paso 5 completado.**
- 2026-11-20: paso 6a1 hecho (512 tests en verde, 7 nuevos; sin avisos). `ErrorCode` gana
  `COURSE_NOT_FOUND`, `NOT_COURSE_AUTHOR` e `IDENTICAL_TO_LATEST` (`ErrorCodeTest` actualizado).
  `domain/course/SizeLimit` (mensaje de `PAYLOAD_TOO_LARGE` con o sin tamaño), que ahora usa también
  `CourseJson`. En `web/course`: `IssueResponse`, `CourseReportResponse`, `AuthorResponse`,
  `CourseResponse` (sin `searchText`, `searchTags` ni `sizeBytes`) y `PublishedCourseResponse`, con
  el JSON exacto de §3.7 y §4.4 comprobado en `CourseResponsesTest`.
- 2026-11-20: paso 6a2 hecho (526 tests en verde, 14 nuevos; sin avisos). `PathIds` (un `{id}` que no
  es un UUID canónico, o una versión que no es un entero positivo, es vacío → 404), con test.
  `CoursePublicationController` (`validate`, `publish`, `publishVersion`; cuerpo con `bodyAsBytes`;
  autor de la sesión). `App`: schema, lector, validador, inspector, `JdbcCourseRepository`, casos de
  uso, `maxRequestSize` = 2 MiB y las tres rutas con `requireSession.around`. Comprobado con `curl`
  contra la base de desarrollo (cuenta de prueba `autora.cursos@example.com`): 401 sin sesión, 200
  válido con el ejemplo, 200 con `INVALID_JSON` para JSON roto, 200 con un cuerpo de 1,2 MB, 413 con el
  informe para 2,2 MB (validar y publicar) y 404 para un id que no es UUID. La primera prueba del 413
  devolvió el texto de Javalin: el `catch` esperaba `ContentTooLargeResponse`; ahora reconoce el estado
  413 de `HttpResponseException`.
- 2026-11-20: paso 6a3 hecho (526 tests en verde). Carpeta de Yaak «Cursos · Publicación»
  (`fl_pUw2uUvgMh`) con 15 peticiones creadas con la CLI y enviadas una a una en orden contra la base de
  desarrollo: registro e inicio de sesión de la autora y de otra cuenta (`otra.cuenta@example.com`),
  401, 200 válido (con avisos), 200 con errores, 201, 400, 422, 201 de la versión 2 (con
  `REMOVED_IDS` de `intro-r1`), 409, 403, 404 por curso inexistente y 404 por id que no es UUID. El
  413 se comprobó con `curl` (2,2 MB → informe `PAYLOAD_TOO_LARGE`). El id del curso se reutiliza con
  `${[ response.body.path(request='rq_jAYMWnPgxc', path='$.course.id') ]}`. **Paso 6a completado.**
- 2026-11-20: paso 6b hecho (529 tests en verde, 3 nuevos; sin avisos). `CourseVersionResponse`
  (`@JsonUnwrapped` de los metadatos y `@JsonRawValue` del contenido), `CoursePageResponse` y
  `CourseCatalogController` (`search`, `latest`, `version`; rutas públicas) en `App`. Carpeta de Yaak
  «Cursos · Consulta» (`fl_jjUAUPZHxR`, 14 peticiones, con su posición desde el principio) enviada en
  orden: 201 del ejemplo completo; 200 sin filtros, por texto normalizado, `%` literal (0 resultados),
  dos etiquetas, idioma y segunda página; 400 por `size` fuera de rango y por `page` y `size` a la vez;
  200 de la última versión y de la 1; 404 por versión inexistente, por versión que no es número y por
  curso inexistente. **Bytes conservados (§6):** el SHA-256 del fichero publicado, el `sha256` de la
  respuesta 201 y el del `content` devuelto por el GET (cortado del texto sin reformatearlo) coinciden,
  y `cmp` da los 17 374 bytes idénticos. **Paso 6 completado: tarea completada** (salvo la
  sincronización de Yaak, ver «Pendiente»).

## Pendiente para más adelante

- ~~Duda sobre `MASTERY_POOL_TOO_SMALL`~~: resuelta el 2026-11-19 (opción A, ver «Decisiones del
  paso 2»).
- ~~Sincronización de Yaak~~: resuelta el 2026-11-20. «Cursos · Publicación» está en el orden 1–15
  tanto en los YAML como en la base de datos de Yaak, y «Cursos · Consulta» (`yaak.fl_jjUAUPZHxR.yaml`
  y sus 14 peticiones, en el orden 1–14) ya está en `backend/yaak/workspace`, con el ejemplo completo
  y las plantillas del token y del id del curso. Nota para otra vez: con la CLI 0.7.1, crear cada
  petición ya con su `sortPriority`; `yaak request update` no llega a los YAML.
- Comprobar los bytes conservados desde la propia app: Yaak no calcula SHA-256; se hizo con `curl`,
  `sha256sum` y `cmp` (cortando el `content` de la respuesta sin reformatearlo).
- La app guarda la lista de códigos de error en `ApiErrorCode` (`sharedLogic`): añadir
  `INVALID_PARAMETER`, `COURSE_NOT_FOUND`, `NOT_COURSE_AUTHOR` e `IDENTICAL_TO_LATEST` cuando la app
  use los cursos (mientras tanto los trata como `UNKNOWN`).
- Mensaje de `MASTERY_POOL_SMALL_FOR_RETRY` con un solo ejercicio: «1 ejercicios» (falta el singular).
- `REMOVED_IDS` compara con la última versión leída antes de la transacción de `appendVersion`: si
  dos publicaciones del mismo curso coinciden, el aviso puede compararse con una versión que ya no es
  la última. Solo afecta al aviso; el número de versión lo protege el `FOR UPDATE`.

## Siguiente paso

Tarea completada en el backend. Siguiente: integrar el catálogo en la app (buscar, descargar y guardar
en SQLite), con su propio documento en `docs/tareas/`.
