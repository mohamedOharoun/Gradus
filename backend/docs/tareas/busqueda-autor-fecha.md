# Tarea — Búsqueda por autor y fecha de publicación (backend)

Rama: `dev/backend-course-search`. Proyecto: `backend/` (Maven, Java 25, Javalin 7, PostgreSQL en Docker).
Especificación: [`backend/docs/especificacion-cursos-v1.md`](../especificacion-cursos-v1.md) §4.5 (búsqueda) y
§1.2 (convenciones). Contrato para la app: [`backend/docs/api-cursos.md`](../api-cursos.md).
Origen: tarea 10 de `docs/tareas/buscar-cursos.md` (fase 2 de la pestaña Buscar). En el Figma «4 · Buscar
con filtros», «Creador» es un campo de texto («Nombre del creador») y «Fecha de publicación» ofrece
Cualquiera, Último mes y Último año.

## Objetivo

Que `GET /api/courses` filtre por creador y por fecha de publicación, para que la hoja de filtros de la
app pueda ofrecer «Creador» y «Fecha de publicación» sin romper la paginación ni el total (filtrar en el
móvil los resultados ya recibidos no sirve: decisión 1 de `buscar-cursos.md`).

## Punto de partida (lo que ya existe)

Rutas desde `backend/src/main/java/es/ulpgc/pamn/dominio/backend/` (los tests, en la misma ruta bajo
`src/test/java/`):

- `domain/course/CourseSearch`: record `(text, tags, language)` y Specification. `of(...)` normaliza con
  `TextNormalizer` y descarta los vacíos; `matches(CourseMetadata)` define qué significa buscar.
- `application/course/SearchCourses` con `SearchCoursesQuery` (los parámetros tal como llegan, como texto) y
  `SearchResult` (`Found` | `Rejected`). Solo `page` y `size` pueden ser inválidos; se informan los dos a la
  vez con `Result.combine`.
- `infrastructure/persistence/CourseSearchSql` (traduce `CourseSearch` a un `SqlWhere`, que es un monoide),
  `SqlParameter` (sellado: `Text` | `TextArray`), `LikePattern` (escapa `\`, `%` y `_`),
  `JdbcCourseRepository.search` con su `bind`, y `CourseRows.FROM_LATEST` (alias `c`, `v` y `u`: el nombre
  del autor ya está a mano como `u.name`).
- `web/course/CourseCatalogController.search` y `App` (raíz de composición; ya tiene un `Clock`).
- Tests: `CourseSearchTest`, `SearchCoursesTest`, `CourseSearchSqlTest`, `InMemoryCourseRepository`,
  `PublishedCourses` (autor «Ana», `summary(version)`) y el builder `TestCourses.course()`.
- **529 tests en verde** (`mvn test`, 2026-11-22).

## Decisiones (aprobadas el 2026-11-22)

**A. Nombres y formato de los parámetros.** `author=<texto>` y `published=month|year`. Si faltan o están en
blanco, no filtran, como `q`, `tag` y `language`. «Cualquiera» se pide **no enviando** `published` (no hay
`any`): una sola forma de decir «sin filtro». No se usa `publishedSince=<fecha>`: la app tendría que
calcular «hace un mes» en `commonMain`, sin `java.time` (pediría `kotlinx-datetime`, una dependencia
nueva), y el reloj lo tiene el servidor.

**B. Un `published` no válido** (`week`, `Month`, `1`, `any`) da 400 `INVALID_PARAMETER` con `field`
`published` y el mensaje `Debe ser «month» o «year».`, como `page` y `size`. Se admiten espacios
alrededor (como en `page`), pero solo en minúsculas. Si hay varios parámetros inválidos se informan todos,
en el orden `page`, `size`, `published`. Ignorarlo devolvería el catálogo entero sin avisar, y un fallo de
la app parecería un filtro que funciona. `author` nunca es inválido, como `q`.

**C. «Último mes» y «último año».** Desde la misma hora de hace un mes o un año, **en el calendario y en
UTC**, con el límite incluido: `published_at >= desde`.

- El 2026-11-22 a las 10:00 Z, «último mes» empieza el 2026-10-22 a las 10:00 Z y «último año», el
  2025-11-22 a las 10:00 Z.
- Un 31 de marzo, «último mes» empieza el 28 de febrero (el 29 si es bisiesto): `minusMonths` ajusta al
  último día del mes. Un 29 de febrero, «último año» empieza el 28 de febrero del año anterior.
- El instante límite se **trunca a microsegundos** al guardarlo en `CourseSearch` (como hace
  `CourseVersionDraft`): PostgreSQL guarda microsegundos, y así Java y SQL comparan exactamente lo mismo.
- Diseño:
  - `SearchCourses` recibe un `Clock` por constructor (como `PublishCourse`) y lo lee **en cada búsqueda**.
  - Enum `domain/course/PublishedWithin` (`LAST_MONTH` ↔ `"month"`, `LAST_YEAR` ↔ `"year"`):
    `fromParameter(String)` → `Optional<PublishedWithin>` y `since(Instant now)` → `Instant`, función pura
    (cada constante sabe restar su periodo: **Strategy** dentro del enum).
  - `CourseSearch` guarda el instante (`Optional<Instant> publishedSince`), no la palabra `month`: así
    `matches` no depende de la hora.

**D. Comparar el nombre del autor.**

- El texto pedido se normaliza con `TextNormalizer` (NFC, espacios recortados y agrupados, minúsculas),
  como `q`, y se escapa con `LikePattern` (`%` y `_` son literales).
- SQL: `lower(u.name) LIKE ? ESCAPE '\'`. Specification: `name.toLowerCase(Locale.ROOT).contains(author)`,
  que es lo mismo.
- Sin quitar tildes: «angel» no encuentra «Ángel» (igual que `q`).
- **Límite aceptado:** `UserName` solo recorta los extremos, así que un nombre con dos espacios seguidos
  («Ana␣␣Ruiz») no se encuentra con «ana ruiz». Pasa igual en Java y en SQL, y tiene su test.
- **A comprobar en el 10c:** `lower()` de PostgreSQL depende del *locale* de la base (`lc_ctype`). Si en
  el contenedor desechable `lower('ÁNGEL ÑÚÑEZ')` no da `ángel ñúñez`, la alternativa es
  `lower(u.name COLLATE pg_c_utf8)` (colación integrada desde PostgreSQL 17, que no depende del sistema;
  también hay que comprobarla). En ese caso **el paso se detiene y pregunta** antes de seguir.
- Descartado aquí: una columna normalizada `users.search_name` (como `search_text`). Es lo más exacto, pero
  pide una migración y tocar el registro (ver «Pendiente»).

**E. La forma de `CourseSearch`.** Pasa de 3 a 5 criterios: `(text, tags, language, author,
publishedSince)`. Un `of(...)` con cinco parámetros tendría tres `String` seguidos, y confundirlos
compilaría. Por eso:

- `of(text, tags, language)` se queda igual (los tests de hoy no cambian) y deja autor y fecha vacíos.
- Dos *withers* inmutables (devuelven una copia; el original no cambia), como `CourseQuery` en la app:
  - `withAuthor(String raw)`: normaliza; nulo o en blanco → sin filtro de autor.
  - `publishedSince(Instant since)`: no admite nulo.
- Uso: `CourseSearch.of(q, tags, language).withAuthor(author)` y, si hay fecha, `.publishedSince(since)`.
- `matches(CourseMetadata)` se **sustituye** por `matches(CourseSummary)`, porque el autor y la fecha están
  en el resumen y no en los metadatos. No se deja la versión antigua: nadie puede olvidar los filtros
  nuevos. Los tests de hoy de `CourseSearchTest` solo cambian el ejemplo, que pasa a ser un
  `CourseSummary`.

**F. SQL de la fecha.** `v.published_at >= ?` con un `SqlParameter.Timestamp(Instant)` nuevo, que `bind`
convierte con `Timestamps.toColumn`. Como `SqlParameter` es sellado, el compilador obliga a cubrirlo en el
`switch` de `bind`. Orden de las condiciones: texto, etiquetas, idioma, autor, fecha.

**G. Documentos.** `api-cursos.md` y, además, §4.5 de `especificacion-cursos-v1.md` (marcado como
«añadido en `busqueda-autor-fecha.md`»), para que siga siendo cierto que allí está el detalle completo.

## Alcance

- Parámetros `author` y `published` en `GET /api/courses`, combinados con Y con los que ya hay.
- 400 `INVALID_PARAMETER` para un `published` no válido.
- Tests JUnit (`mvn test`), SQL probado a mano en un PostgreSQL 18 desechable y peticiones de Yaak.
- `api-cursos.md` y §4.5 de la especificación al día.

## Fuera de alcance

- «Ordenar por» y valoraciones (decisión 10 de `buscar-cursos.md`; §8 de la especificación).
- `GET /api/courses/tags` (decisión 3 de `buscar-cursos.md`).
- Filtrar por id de autor (lo necesitará «Mis cursos»).
- Quitar tildes al comparar (ni en `q` ni en `author`).
- Otras ventanas (semana) y rangos desde/hasta.
- Índices nuevos.
- La app (tarea 11 de `buscar-cursos.md`), incluido `ApiErrorCode`.
- Tests automáticos de repositorios y endpoints (pendientes del CI/CD, §8).

## Tareas

Cada paso: **tests primero** (en rojo, después en verde), `mvn test` (o `./gradus.sh test`) en verde y sin
avisos de compilación nuevos, y una entrada en «Progreso» con la fecha y el número de tests. Al terminar,
se espera la revisión antes del paso siguiente. Cada comportamiento descrito aquí o en un comentario lleva
su propio test, aunque ya funcione (la lista de tests es la mínima).

- [x] **10a · Dominio.** `domain/course/PublishedWithin` (nuevo) y `CourseSearch` (decisiones C, D y E);
  `InMemoryCourseRepository` pasa a llamar a `matches(summary)`. Tests en `PublishedWithinTest` (nuevo) y
  `CourseSearchTest`.
  - `PublishedWithin`:
    - `fromParameter`: `month` y `year` valen, también con espacios alrededor; `Month`, `week`, `any`,
      `""` y `null` dan vacío.
    - `since`: 2026-11-22T10:00Z → 2026-10-22T10:00Z (mes) y 2025-11-22T10:00Z (año);
      2026-03-31 → 2026-02-28; 2028-03-31 → 2028-02-29; 2028-02-29 menos un año → 2027-02-28.
  - `CourseSearch`:
    - `withAuthor` normaliza («  ANA   Ruiz » → «ana ruiz»); nulo o en blanco no filtra.
    - El autor es una subcadena sin distinguir mayúsculas («ANA» y «na ru» encuentran a «Ana Ruiz»; «Luis»
      no).
    - Sin quitar tildes: «angel» no encuentra a «Ángel».
    - `%` y `_` en el autor son literales.
    - Límite de D: «ana ruiz» no encuentra a «Ana␣␣Ruiz».
    - `publishedSince` trunca a microsegundos.
    - Un curso publicado justo en el límite entra; uno publicado un microsegundo antes, no.
    - Sin filtro de fecha entran todos; con `of(...)`, autor y fecha están vacíos.
    - Todos los filtros a la vez se combinan con Y (fallar uno basta para quedar fuera).
    - Los *withers* no modifican el original.
    - Los tests que ya había siguen pasando.

  *Acepta si:* los tests de `PublishedWithin` y `CourseSearch` cubren la lista anterior y `mvn test` pasa.

- [ ] **10b · Caso de uso y web** (cambia el contrato de la API). `SearchCoursesQuery` gana `author` y
  `published` (orden de los componentes: `text`, `tags`, `language`, `author`, `published`, `page`,
  `size`); `SearchCourses` recibe el `Clock`; `CourseCatalogController.search` lee los dos parámetros y
  los pasa tal como llegan; `App` le pasa el `clock`. Tests en `SearchCoursesTest`.
  - Los errores se reúnen con `Result.combine` anidado (página y tamaño → `PageRequest`; después, con
    `published`), sin tocar `Result`.
  - Tests:
    - Autor solo, y autor con una etiqueta.
    - Con un reloj fijo: un curso de hace 40 días entra en `year` y no en `month`; uno de hace dos años no
      entra en ninguno; uno publicado justo en el límite entra.
    - `published` ausente o en blanco = cualquiera.
    - `published=week` → `Rejected` con `INVALID_PARAMETER` en `published` y su mensaje.
    - `page`, `size` y `published` inválidos a la vez → tres errores, en ese orden.
    - La ventana se mide con la hora de cada búsqueda, no al crear el caso de uso: con un `Clock` de test
      que se puede adelantar (clase pequeña en los tests), el mismo `SearchCourses` deja fuera un curso
      tras adelantar el reloj.

  *Acepta si:* esos tests pasan, los que ya había siguen pasando y `App` compila con el `Clock`.

- [ ] **10c · SQL.** `SqlParameter.Timestamp`, las dos condiciones en `CourseSearchSql` (decisiones D y F)
  y el caso nuevo en `JdbcCourseRepository.bind`. Tests en `CourseSearchSqlTest`.
  - Tests:
    - Autor → `lower(u.name) LIKE ? ESCAPE '\'` con el patrón escapado («ana_r» → `%ana\_r%`).
    - Fecha → `v.published_at >= ?` con un `Timestamp`.
    - Los cinco filtros juntos, en orden y con sus parámetros en el mismo orden.
  - A mano, en un contenedor desechable `gradus-pg-scratch` (`postgres:18-alpine`, puerto 55432, sin
    volumen; se crea y se borra) con `jshell`, como en el paso 5d de `publicacion-cursos.md`:
    - `SELECT lower('ÁNGEL ÑÚÑEZ'), current_setting('lc_ctype')` y el proveedor de *locale* de la base.
      Comparar el `lc_ctype` con el de la base de desarrollo (consulta de solo lectura). Si `lower()` no
      sirve, **parar y preguntar** (decisión D).
    - Autor sin mayúsculas, con tildes y con `%` literal.
    - Límite de fecha incluido y un microsegundo antes, fuera (fechas insertadas a mano).
    - Autor + etiqueta + fecha.
    - Paginación con filtro de fecha y varios cursos a la misma hora, sin repeticiones ni saltos.

  *Acepta si:* los tests pasan y las pruebas a mano salen como dice la Specification.

- [ ] **10d · Documentos y Yaak.**
  - `api-cursos.md`: la ruta (`GET /api/courses?q=&tag=&language=&author=&published=&page=&size=`), dos
    puntos nuevos en «Búsqueda» y `published` en la fila de `INVALID_PARAMETER`.
  - `especificacion-cursos-v1.md` §4.5: `author` y `published` (decisión G).
  - Yaak, carpeta «Cursos · Consulta» (`fl_jjUAUPZHxR`, hoy 14 peticiones): las nuevas se crean **ya con su
    `sortPriority`** (15, 16…), porque con la CLI 0.7.1 `yaak request update` no llega a los YAML. Autor
    (en mayúsculas), `month`, `year`, autor + etiqueta + `year`, 400 por `published=week` y 400 con
    `page`, `size` y `published` inválidos a la vez.
  - Necesita permiso para arrancar el servidor contra la base de desarrollo (solo lecturas).
  - Por la API no se pueden publicar cursos antiguos (la fecha la pone el servidor): en Yaak, `month` y
    `year` incluyen todos los cursos. Las exclusiones quedan probadas en JUnit (10a, 10b) y en el 10c.

  *Acepta si:* las peticiones dan los códigos y resultados esperados y los dos documentos describen
  `author` y `published`.

## Progreso

- 2026-11-22: documento creado; decisiones A–G aprobadas. Punto de partida: 529 tests en verde.
- 2026-11-22: **10a hecho, pendiente de revisión.** `PublishedWithin` (nuevo) y `CourseSearch` con `author` y
  `publishedSince`, `withAuthor`, `publishedSince` y `matches(CourseSummary)`; `InMemoryCourseRepository` usa
  `search::matches`. Tests: 559 en verde (+30: `PublishedWithinTest` 11 nuevos y `CourseSearchTest` de 8 a 27),
  sin avisos nuevos. Tests escritos primero y vistos en rojo (21 fallos) contra esqueletos que compilaban.
- 2026-11-22: **10a revisado.** `mvn clean test`: 559 en verde, sin avisos. Cada punto de la lista tiene su
  test. Decisiones del paso, que **se mantienen**:
  - `withAuthor(null)` o en blanco **quita** un autor anterior: un *wither* fija el campo al valor dado, y
    «ninguno» también es un valor (igual que en `of(...)`, donde en blanco es vacío). La asimetría con
    `publishedSince(null)`, que lanza una excepción, es la del plan: el autor llega crudo de HTTP y la
    fecha la calcula el código.
  - Tests añadidos fuera de la lista, porque documentan promesas del Javadoc o de un comentario: el
    calendario en UTC con otra zona por defecto (con `ZoneId.systemDefault()` en Madrid daría una hora
    menos), la minúscula con `Locale.ROOT` en turco (como ya hace `TextNormalizerTest`) y el truncado
    antes de comparar. Cambian el estado global de la JVM, pero lo restauran en un `finally` y los tests
    no se ejecutan en paralelo (no hay `junit-platform.properties`).
  - `PublishedWithin.since` conserva los nanosegundos: truncar es cosa de `CourseSearch` (decisión C).
  - Para el 10b: `fromParameter` da vacío tanto para un valor en blanco como para uno inválido, así que
    el caso de uso debe mirar primero si `published` falta o está en blanco (sin filtro) y solo después
    tratar el vacío como 400 (decisión B).

## Pendiente para más adelante

- Columna normalizada del nombre (`users.search_name`), si el límite de la decisión D llega a molestar.
- Filtrar por id de autor, para «Mis cursos».

## Siguiente paso

Paso 10b (caso de uso y web), en un chat nuevo con Opus.
