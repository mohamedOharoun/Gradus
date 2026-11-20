# Gradus — Formato de curso v1: validación y almacenamiento en el backend

Especificación para implementar con Claude Code. La acompañan dos ficheros:

- `course-v1.schema.json`: JSON Schema (draft 2020-12) con la **estructura** del curso.
- `ejemplo-grafos.json`: curso de ejemplo **válido y sin avisos**. Es la base de los fixtures de test.

Ubicación sugerida en el repositorio:

| Fichero | Dónde |
|---|---|
| Esta especificación | `backend/docs/especificacion-cursos-v1.md` |
| `course-v1.schema.json` | `backend/src/main/resources/schemas/course-v1.schema.json` |
| `ejemplo-grafos.json` | `backend/src/test/resources/courses/valid/ejemplo-grafos.json` |

---

## 0. Contexto y alcance

Gradus es una app Android (Kotlin) de aprendizaje por dominio (*mastery learning*). Un curso es un grafo de módulos. Cada módulo tiene:

- prerrequisitos (otros módulos);
- una parte formativa: lecciones y ejercicios intercalados;
- una reserva de ejercicios de repaso, que sirve como **prueba de dominio** para desbloquear los módulos siguientes y después alimenta la repetición espaciada.

Los instructores escriben cada curso como un fichero JSON y lo publican en el backend. El backend:

1. valida el fichero;
2. si es válido, le asigna id y versión, extrae los metadatos de búsqueda y guarda el JSON **tal cual**, con los mismos bytes;
3. lo sirve a la app, que construye su propia representación en SQLite.

El backend no construye ninguna otra representación del contenido.

**Esta tarea:** validador del formato v1, persistencia de cursos y versiones, y API de publicación, consulta y búsqueda.

**Fuera de alcance:** ver §8.

---

## 1. El proyecto

### 1.1 Stack

- **Java 25** con Maven, sin framework de inyección de dependencias. Paquete raíz `es.ulpgc.pamn.dominio.backend`.
- **Javalin 7.2.4**: rutas en `config.routes`, puerto en `config.jetty.port`.
- **Jackson 2.22.4** (`com.fasterxml.jackson`) como mapper JSON de Javalin.
- **JUnit 6.1.3** (Jupiter) para los tests.
- **PostgreSQL 18** (`postgres:18-alpine`, contenedor `gradus-postgres`) con **JDBC directo** (driver 42.7.14, `PGSimpleDataSource`), sin ORM ni pool de conexiones.
- **Migraciones** en `backend/db/init/NNN_lo_que_hace.sql`. Las aplica `./gradus.sh` (orden `up` o `db`), que anota las ya aplicadas en la tabla `schema_migrations` y ejecuta cada una en su propia transacción.
- **Autenticación ya implementada**: token Bearer de sesión. `RequireSession.around(...)` protege una ruta y entrega al handler un `AuthenticationResult.Authenticated` con el `User`.

### 1.2 Convenciones del proyecto (respétalas)

**Código:**

- **Capas:**
  - `domain`: records inmutables que se validan en el constructor. Sin imports de Javalin, Jackson ni JDBC.
  - `application`: casos de uso como clases `final`, con dependencias por constructor (puertos, `Clock`, `Supplier<UUID>`). Devuelven `sealed interface`s de resultados. Los puertos de salida (`UserRepository`, `SessionRepository`) son interfaces aquí.
  - `infrastructure`: implementaciones (JDBC con SQL en *text blocks*). Un fallo inesperado se propaga como `IllegalStateException`.
  - `web`: controladores que traducen HTTP ↔ caso de uso con un `switch` sobre el resultado. Sin reglas de negocio.
  - `App`: raíz de composición; el único sitio que conoce y conecta las implementaciones.
- **Errores esperables como valores** (resultados sellados), no como excepciones.
- **Instantes truncados a microsegundos** antes de guardarlos (PostgreSQL guarda microsegundos), y columnas `TIMESTAMPTZ` a través de `Timestamps`.
- **Comprobar e insertar de forma atómica** cuando una regla depende del estado de la base de datos, como hace `UserRepository.add` con `ON CONFLICT`.
- **Errores de la API** fuera de la validación de cursos: `ErrorResponse` con `ErrorCode`. Se pueden añadir códigos; los existentes no se renombran ni se borran.
- **Comentarios y Javadoc en español**; identificadores en inglés.

**SQL:**

- Migraciones **repetibles sin efecto** (`IF NOT EXISTS`), con un comentario de cabecera en español.
- Las reglas del dominio se repiten como restricciones `CHECK` con nombre (`<tabla>_<regla>`), como en `003_create_sessions.sql`.
- Longitudes con `VARCHAR(n)`: PostgreSQL cuenta caracteres reales (*code points*), igual que `Validation.lengthOf` y que `maxLength` del JSON Schema.

**Proceso:**

- Cada tarea tiene su documento en `backend/docs/tareas/` (ver `inicio-sesion.md`): objetivo, decisiones, alcance, fuera de alcance, tareas con criterio «*Acepta si:*», progreso con fecha y número de tests en verde, pendiente y siguiente paso.
- **Tests primero** en dominio y casos de uso, con repositorios en memoria.
- `./gradus.sh test` (`mvn test`) **no necesita la base de datos**. El SQL se prueba a mano en un PostgreSQL 18 desechable, y los endpoints con peticiones de **Yaak** (CLI `yaak request create` y `yaak send`, workspace «Gradus · Backend»).
- **Sin dependencias nuevas** salvo aprobación expresa del usuario.

### 1.3 Antes de empezar

1. Lee `backend/docs/tareas/inicio-sesion.md` para ver el formato de las tareas y el nivel de detalle esperado.
2. Crea `backend/docs/tareas/publicacion-cursos.md` a partir de esta especificación, con las tareas de §7. Pregunta al usuario en qué rama trabajar (las tareas anteriores usaron `develop-backend-gradus`).
3. **Esta tarea necesita una dependencia nueva**: una librería de JSON Schema que soporte draft 2020-12 **y funcione con Jackson 2.22** (por ejemplo, networknt/json-schema-validator en una versión basada en Jackson 2; compruébalo). Propón al usuario la librería, la versión y la licencia **antes** de añadirla. La alternativa, reescribir el schema a mano en Java, duplicaría la fuente de verdad, así que no es recomendable.
4. Comprueba el nombre del límite de tamaño de cuerpo en Javalin 7 (§4.7).
5. **Espera la aprobación del plan** antes de escribir código, como en las tareas anteriores.
6. No modifiques la app Android.

**Fuente de verdad.** La estructura la define el schema; la semántica (§3), este documento. Si encuentras una contradicción entre ambos, o una regla ambigua, **detente y pregunta**. No la resuelvas por tu cuenta.

---

## 2. El formato v1

### 2.1 Estructura general

```
curso
├── formatVersion        siempre 1
├── title, description, language, tags
├── defaults.mastery     { questions, minCorrect }
└── modules[]
    ├── id, title
    ├── requires[]       ids de los módulos prerrequisito
    ├── mastery?         sustituye a defaults.mastery en este módulo
    ├── steps[]          parte formativa: lecciones y ejercicios, en orden
    └── review[]         reserva: prueba de dominio y repasos posteriores
```

### 2.2 Campos del curso

| Campo | Tipo | Reglas |
|---|---|---|
| `formatVersion` | entero | Siempre `1`. Es la versión del **formato**, no del curso. |
| `title` | texto plano | 1–120 caracteres. |
| `description` | texto plano | 1–500 caracteres. |
| `language` | texto | ISO 639-1, opcionalmente con región: `es`, `en`, `es-ES`. |
| `tags` | lista de texto plano | 1–10 etiquetas de 1–32 caracteres, sin repetir. |
| `defaults.mastery` | objeto | Prueba de dominio por defecto (§2.6). |
| `modules` | lista | 1–100 módulos. |

### 2.3 Módulos

| Campo | Reglas |
|---|---|
| `id` | Id (§2.4). |
| `title` | Texto plano, 1–120 caracteres. |
| `requires` | 0–20 ids de módulos de este curso, sin repetir. Solo se guarda esta dirección; los sucesores se deducen. |
| `mastery` | Opcional. Si está, sustituye a `defaults.mastery` en este módulo. |
| `steps` | 1–50 pasos: lecciones o ejercicios, en el orden en que se presentan. |
| `review` | 1–100 ejercicios (sin lecciones). |

### 2.4 Ids

- Formato: minúsculas ASCII, dígitos y guiones; sin guiones al principio, al final ni seguidos. Hasta 64 caracteres. Patrón: `^[a-z0-9]+(-[a-z0-9]+)*$`.
- Los ids de **módulos, lecciones y ejercicios comparten un único espacio de nombres**: ningún id puede repetirse en todo el curso.
- Los ids de las opciones son locales a su ejercicio.
- Los ids deben ser **estables entre versiones** del curso, porque el progreso de los alumnos se refiere a ellos. El validador no puede imponerlo, pero al publicar una versión nueva avisa de los ids que desaparecen (§4.6).

### 2.5 Pasos y ejercicios

Todos los pasos y ejercicios llevan `type` e `id`.

**Lección** (`"type": "lesson"`): `text`, Markdown de hasta 10 000 caracteres.

**Ejercicios.** Campos comunes:

- `prompt`: enunciado, Markdown de hasta 2000 caracteres.
- `explanation`: Markdown de hasta 3000 caracteres, **obligatoria**. Se muestra tras responder; es la parte correctora del aprendizaje por dominio.

| `type` | Campos propios | Se considera correcto si… |
|---|---|---|
| `singleChoice` | `options` (2–6), `answer` (id de una opción) | se elige exactamente la opción `answer` |
| `multipleChoice` | `options` (3–6), `correct` (1–5 ids, sin repetir) | el conjunto elegido es exactamente `correct` |
| `matching` | `pairs` (2–8 pares `{ "left", "right" }`) | se reconstruyen todos los pares |

- Una opción es `{ "id", "text" }`, con `text` en Markdown de hasta 300 caracteres.
- Cada lado de un par es Markdown de hasta 200 caracteres.

Estas representaciones están elegidas para que ciertos errores **no se puedan escribir**:

- `singleChoice` con un único `answer`: «exactamente una correcta» se cumple por construcción, y con 2 o más opciones siempre hay al menos una incorrecta.
- `matching` como lista de pares: los conjuntos izquierdo y derecho son los elementos que aparecen en los pares, así que la relación ya es total y sobreyectiva. Si además no se repite ninguna izquierda ni ninguna derecha, es una **biyección**. Basta con comprobar duplicados.

### 2.6 Prueba de dominio

```json
"mastery": { "questions": 4, "minCorrect": 3 }
```

- `questions`: cuántos ejercicios de `review` se sortean para la prueba (1–20).
- `minCorrect`: aciertos mínimos para dar el módulo por dominado (1–20).
- Se guarda como número de aciertos y no como porcentaje: con pocas preguntas, un porcentaje obliga a fijar una regla de redondeo.
- **Prueba efectiva** de un módulo: `module.mastery` si existe; si no, `defaults.mastery`.

### 2.7 Texto

- **Texto plano** (`title`, `description`, etiquetas): una sola línea, sin espacios al principio ni al final.
- **Markdown** (lecciones, enunciados, explicaciones, opciones, pares): CommonMark, con al menos un carácter que no sea espacio. La app no renderiza HTML embebido.
- Los patrones del schema terminan en `(?![\s\S])` en lugar de `$`, porque en Java y Python `$` también acepta un salto de línea final. El motor de expresiones regulares debe soportar *lookahead*; el de Java lo soporta.

### 2.8 Lo que no va en el fichero

El autor **no** escribe id del curso, versión, autor ni fechas: los asigna el servidor (§4). El schema rechaza cualquier campo no definido (`additionalProperties: false`), así que un fichero con `"author"` o `"id"` en la raíz no pasa la validación. Esto también caza erratas en los nombres de campo.

---

## 3. Validación

### 3.1 Arquitectura y principios

El validador se divide en dos piezas, siguiendo las capas del proyecto:

- **Lector** (`infrastructure`, capas 1–2): bytes → curso tipado, o lista de problemas.
  - Decodifica, parsea con Jackson a `JsonNode`, valida contra el schema y, si todo pasa, **convierte el `JsonNode` en los records del dominio a mano**.
  - No pongas anotaciones de Jackson en el dominio. Tras pasar el schema la forma está garantizada, así que el mapeo manual es directo.
  - Usa un `ObjectMapper` propio para cursos, sin tocar el que Javalin usa para el resto de la API.
- **Reglas** (`domain`, capas 3–5): funciones puras sobre los records del dominio (por ejemplo `CourseDocument`, `Module`, una `sealed interface Step` con `Lesson` y los tres ejercicios).
  - Sin dependencias de Jackson, Javalin ni JDBC.
  - Las rutas JSON Pointer se calculan con los índices de las listas, que conservan el orden del documento: `/modules/1/review/3`.
  - Así las reglas se prueban sin JSON y podrán portarse más adelante al módulo Kotlin compartido de la app (`sharedLogic`).

**Leer versiones guardadas.** Para leer el contenido de una versión ya publicada (por ejemplo, en `compareVersions`, §4.6) se usa **solo el lector**, no las reglas. Así, si las reglas de las capas 3–5 se endurecen en el futuro, las versiones antiguas se siguen pudiendo leer. Es el mismo problema que `inicio-sesion.md` apunta como pendiente para `UserName` y `Email`.

Principios comunes:

- **Determinista.** El mismo fichero produce siempre el mismo informe, con los problemas en orden de documento.
- **Exhaustivo dentro de cada capa.** Recoge todos los problemas de una capa antes de parar.
- **Tipos propios.** Cada problema es un `Issue(code, severity, path, message)`, con su propio enum de códigos (por ejemplo `CourseIssueCode`). No reutilices `ErrorCode` ni `ValidationError`: esos describen campos de una petición que la app traduce; estos describen posiciones dentro de un documento y los leen autores y LLMs.
- **Mensajes accionables.** Cada problema lleva:
  - un **código** estable (§3.3);
  - una **ruta** en formato JSON Pointer (RFC 6901);
  - un **mensaje en español** que nombra los ids y valores implicados, dice qué se esperaba y, si ayuda, lista las alternativas válidas. Un instructor debe poder pegar el informe en un LLM y pedirle que corrija el curso.

### 3.2 Capas

1. **Sintaxis**: UTF-8 válido, JSON válido, sin claves duplicadas.
2. **Versión y estructura**: `formatVersion` soportada; después, el schema.
3. **Identidad y referencias**: ids únicos, referencias existentes, opciones y pares coherentes.
4. **Grafo**: sin ciclos; aristas redundantes.
5. **Dominio y pedagogía**: pruebas de dominio realizables y razonables.

Reglas de ejecución:

- Cada capa supone que las anteriores no tienen errores. Si una capa produce errores, las siguientes no se ejecutan.
- Excepción: las capas 4 y 5 son independientes entre sí. Si la capa 3 pasa, se ejecutan ambas.
- Los avisos nunca detienen nada.

Detalles de implementación de la capa 1:

- Si el cuerpo empieza por un BOM UTF-8, se elimina antes de nada.
- Decodifica con un `CharsetDecoder` de UTF-8 configurado con `CodingErrorAction.REPORT`: una secuencia inválida es `INVALID_JSON`, no un carácter de sustitución silencioso. Esto además garantiza que guardar el texto y volver a codificarlo reproduce los mismos bytes (§4.1).
- Activa `JsonParser.Feature.STRICT_DUPLICATE_DETECTION` en el mapper de cursos.

### 3.3 Catálogo de reglas

| Código | Capa | Gravedad | Condición | Ruta |
|---|---|---|---|---|
| `INVALID_JSON` | 1 | error | No es UTF-8 válido o no es JSON válido. | `""`; el mensaje incluye línea y columna si el parser las da |
| `DUPLICATE_KEY` | 1 | error | Un objeto repite una clave. Muchos parsers se quedan en silencio con la última; aquí se rechaza. | objeto afectado, si el parser lo permite; si no, `""` |
| `UNSUPPORTED_FORMAT_VERSION` | 2 | error | `formatVersion` existe, es un entero y es distinto de 1. Se comprueba antes del schema para dar un mensaje claro; si aparece, no se ejecuta el schema. | `/formatVersion` |
| `SCHEMA_VIOLATION` | 2 | error | Incumple el schema. Una entrada por violación. | ubicación en la instancia |
| `DUPLICATE_ID` | 3 | error | Un id de módulo, lección o ejercicio se repite en el curso. | segunda aparición; el mensaje cita la primera |
| `DUPLICATE_OPTION_ID` | 3 | error | Dos opciones del mismo ejercicio comparten id. | segunda opción |
| `DUPLICATE_OPTION_TEXT` | 3 | error | Dos opciones del mismo ejercicio tienen el mismo texto normalizado (§3.4). | segunda opción |
| `UNKNOWN_OPTION` | 3 | error | `answer`, o un elemento de `correct`, no es id de ninguna opción del ejercicio. | ese campo o elemento |
| `ALL_OPTIONS_CORRECT` | 3 | error | En `multipleChoice`, `correct` incluye todas las opciones: marcarlo todo siempre acertaría. | `…/correct` |
| `DUPLICATE_MATCHING_SIDE` | 3 | error | En `matching`, dos izquierdas o dos derechas tienen el mismo texto normalizado: deja de ser biyección. | segundo par repetido |
| `UNKNOWN_PREREQUISITE` | 3 | error | Un id de `requires` no es un módulo del curso. | `/modules/i/requires/j` |
| `SELF_PREREQUISITE` | 3 | error | Un módulo se requiere a sí mismo. | `/modules/i/requires/j` |
| `DUPLICATE_TAG` | 3 | aviso | Dos etiquetas coinciden tras normalizar (§3.4). | segunda etiqueta |
| `CYCLE` | 4 | error | Los prerrequisitos forman un ciclo. El mensaje muestra el ciclo concreto (§3.5). | `/modules/i` del módulo donde empieza el ciclo informado |
| `REDUNDANT_PREREQUISITE` | 4 | aviso | El prerrequisito p de m ya se alcanza a través de otro prerrequisito de m (§3.5). | `/modules/i/requires/j` |
| `MASTERY_MIN_EXCEEDS_QUESTIONS` | 5 | error | `minCorrect` > `questions`. | el objeto `mastery` (una vez por objeto) |
| `MASTERY_POOL_TOO_SMALL` | 5 | error | `questions` de la prueba efectiva > número de ejercicios de `review`. | `/modules/i/review` |
| `MASTERY_POOL_SMALL_FOR_RETRY` | 5 | aviso | `review` tiene menos de 2 · `questions` ejercicios: un reintento repetirá preguntas. | `/modules/i/review` |
| `MASTERY_REQUIRES_PERFECT` | 5 | aviso | `minCorrect` = `questions`: un solo despiste suspende. | el objeto `mastery` (una vez por objeto) |
| `MASTERY_GUESSABLE` | 5 | aviso | La probabilidad de aprobar adivinando supera el 5 % (§3.6). | `/modules/i/review` |
| `MODULE_WITHOUT_LESSON` | 5 | aviso | `steps` no contiene ninguna lección. | `/modules/i/steps` |
| `MODULE_WITHOUT_EXERCISE` | 5 | aviso | `steps` no contiene ningún ejercicio formativo. | `/modules/i/steps` |
| `MATCHING_TOO_MANY_PAIRS` | 5 | aviso | Más de 6 pares: incómodo en la pantalla de un móvil. | `…/pairs` |

Notas sobre la capa 5:

- Las reglas sobre el propio objeto `mastery` (`MASTERY_MIN_EXCEEDS_QUESTIONS`, `MASTERY_REQUIRES_PERFECT`) se informan **una vez por objeto**: en `/defaults/mastery` o en `/modules/i/mastery`.
- Las que dependen de la reserva de un módulo se informan **una vez por módulo**, con el id del módulo en el mensaje.
- Si la prueba efectiva de un módulo tiene un error, se omiten los avisos de dominio de ese módulo.

Fuera del validador también aparecen en el informe `PAYLOAD_TOO_LARGE` (error, §4.7) y `REMOVED_IDS` (aviso al publicar una versión, §4.6).

### 3.4 Normalización de texto

Para comparar textos (opciones, lados de un par, etiquetas, búsqueda):

1. Normalización Unicode **NFC**. Así, «é» precompuesta y «e» seguida de U+0301 (acento combinante) son iguales.
2. Recortar espacios en blanco al principio y al final.
3. Sustituir cada secuencia de espacios en blanco por un único espacio.
4. Pasar a minúsculas con `Locale.ROOT`.

La normalización **solo se usa para comparar**. Los textos se guardan y se sirven tal como los escribió el autor. Ponla en el dominio, como función pura, porque la usan tanto las reglas como la búsqueda.

### 3.5 Grafo de prerrequisitos

**Detección de ciclos (algoritmo de Kahn).** Repetir: tomar un módulo cuyos prerrequisitos ya se hayan tomado todos. Si al final quedan módulos sin tomar, hay al menos un ciclo entre ellos.

**Extracción de un ciclo concreto.** Cada módulo restante tiene al menos un prerrequisito también restante; si no, se habría tomado. Por tanto:

1. Empezar por el primer módulo restante, en orden de documento.
2. Seguir siempre su primer prerrequisito restante, en el orden de `requires`.
3. Como el número de módulos es finito, en algún momento se repite uno. El tramo entre sus dos apariciones es un ciclo.

Mensaje de ejemplo: «Los prerrequisitos forman un ciclo: representacion requiere camino, que requiere bfs, que requiere representacion.» Basta con informar un ciclo: el autor corrige y vuelve a validar.

**Aristas redundantes** (solo si no hay ciclos). El prerrequisito p de m es redundante si p es alcanzable siguiendo `requires` desde algún otro prerrequisito q de m. Con 100 módulos como máximo, un recorrido por cada prerrequisito es suficiente.

### 3.6 Probabilidad de aprobar adivinando

Para cada módulo cuya prueba efectiva no tenga errores, con q = `questions` y k = `minCorrect`:

1. Probabilidad de acertar al azar cada ejercicio de `review`:
   - `singleChoice` con n opciones: 1/n.
   - `multipleChoice` con n opciones: 1/(2ⁿ − 1), porque la app exige marcar al menos una (§5).
   - `matching` con m pares: 1/m!, porque la app exige completar todas las parejas (§5).
2. p = el **máximo** de esos valores en la reserva.
3. Cota = P(X ≥ k) con X ~ Binomial(q, p):

   P(X ≥ k) = Σ desde i = k hasta q de C(q, i) · pⁱ · (1 − p)^(q − i)

4. Si la cota es mayor que 0,05: aviso `MASTERY_GUESSABLE`. El mensaje incluye la cota en porcentaje con un decimal y sugiere aumentar `questions` o usar ejercicios con más opciones.

**Por qué usar la p máxima es válido.** Las preguntas se sortean y cada una se adivina de forma independiente con su propia probabilidad, que es como mucho la máxima. Si alguna fuese más fácil de adivinar, la probabilidad de aprobar solo podría aumentar. Por tanto, la binomial con la p máxima es una **cota superior**.

Valores exactos para los tests:

| q | k | p | Cota |
|---|---|---|---|
| 5 | 4 | 1/4 | 0,015625 |
| 5 | 4 | 1/2 | 0,1875 |
| 10 | 8 | 1/2 | 0,0546875 |
| 4 | 3 | 1/5 | 0,0272 |

### 3.7 Informe

```json
{
  "valid": false,
  "errors": [
    {
      "code": "UNKNOWN_PREREQUISITE",
      "path": "/modules/1/requires/0",
      "message": "El módulo «bfs» requiere «colas», que no es un módulo de este curso. Módulos existentes: representacion, bfs."
    }
  ],
  "warnings": []
}
```

- `valid` es `true` si y solo si `errors` está vacío.
- Los avisos no impiden publicar.

---

## 4. Backend

### 4.1 Migración y modelo de datos

Nueva migración `backend/db/init/004_create_courses.sql`. La aplica `./gradus.sh` al arrancar (`up` o `db`), también sobre bases ya creadas:

```sql
-- Cursos publicados y sus versiones. Ver backend/docs/tareas/publicacion-cursos.md.
-- El id, las fechas y el número de versión los genera la aplicación (Java), no la base de datos.
-- Se puede ejecutar varias veces sin efecto (IF NOT EXISTS).
CREATE TABLE IF NOT EXISTS courses (
    id             UUID        PRIMARY KEY,
    -- Sin ON DELETE CASCADE: borrar a un autor no debe borrar cursos que otros estudian.
    author_id      UUID        NOT NULL REFERENCES users (id),
    created_at     TIMESTAMPTZ NOT NULL,
    latest_version INTEGER     NOT NULL,
    CONSTRAINT courses_latest_version_positive CHECK (latest_version >= 1)
);

-- Para comprobar la clave ajena al borrar un usuario y, más adelante, listar los cursos de un autor.
CREATE INDEX IF NOT EXISTS courses_author_id_idx ON courses (author_id);

-- Cada versión es inmutable. Los metadatos se derivan del JSON al publicar; el JSON es la fuente de verdad.
CREATE TABLE IF NOT EXISTS course_versions (
    course_id      UUID         NOT NULL REFERENCES courses (id),
    version        INTEGER      NOT NULL,
    published_at   TIMESTAMPTZ  NOT NULL,
    format_version INTEGER      NOT NULL,
    title          VARCHAR(120) NOT NULL,
    description    VARCHAR(500) NOT NULL,
    language       VARCHAR(5)   NOT NULL,
    -- Etiquetas tal como las escribió el autor, y normalizadas para buscar (ver §3.4 de la especificación).
    tags           TEXT[]       NOT NULL,
    search_tags    TEXT[]       NOT NULL,
    -- Título y descripción normalizados, para buscar con LIKE.
    search_text    TEXT         NOT NULL,
    module_count   INTEGER      NOT NULL,
    -- SHA-256 en hexadecimal de los bytes recibidos.
    sha256         VARCHAR(64)  NOT NULL,
    size_bytes     INTEGER      NOT NULL,
    -- El JSON recibido, sin modificar. TEXT y no JSONB: JSONB reordena claves y quita espacios.
    content        TEXT         NOT NULL,
    PRIMARY KEY (course_id, version),
    CONSTRAINT course_versions_version_positive CHECK (version >= 1),
    CONSTRAINT course_versions_format_version_positive CHECK (format_version >= 1),
    CONSTRAINT course_versions_language_format CHECK (language ~ '^[a-z]{2}(-[A-Z]{2})?$'),
    CONSTRAINT course_versions_tag_count CHECK (cardinality(tags) BETWEEN 1 AND 10
                                                AND cardinality(search_tags) = cardinality(tags)),
    CONSTRAINT course_versions_module_count CHECK (module_count BETWEEN 1 AND 100),
    CONSTRAINT course_versions_sha256_is_hex CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT course_versions_size CHECK (size_bytes BETWEEN 1 AND 2097152)
);

-- Búsqueda por etiquetas con search_tags @> ARRAY[...] («contiene todas»).
CREATE INDEX IF NOT EXISTS course_versions_search_tags_idx ON course_versions USING GIN (search_tags);
```

Reglas:

- **`content` es `TEXT`, nunca `JSONB`.** `jsonb` guarda una representación binaria: reordena claves, elimina espacios y, ante claves duplicadas, se queda con la última. Los bytes servidos ya no serían los recibidos y el `sha256` dejaría de corresponder. (El tipo `json` sí guarda una copia exacta del texto, pero no aporta nada: el JSON ya está validado.)
- **Los bytes se conservan.** El texto se decodificó de UTF-8 estricto (§3.2) y se vuelve a codificar en UTF-8 al servirlo. Para UTF-8 válido, decodificar y volver a codificar es exactamente reversible, así que los bytes servidos son los del `sha256`. Requiere que la base de datos use UTF-8; compruébalo una vez con `SHOW server_encoding;`.
- `sha256` es el hash SHA-256 de los bytes recibidos (sin el BOM, si lo había), en hexadecimal en minúsculas.
- Las versiones son **inmutables**: nunca se actualizan ni se borran.
- Los metadatos se **derivan del JSON al publicar** y nunca se editan por separado: el JSON es la fuente de verdad y las columnas son copias.

### 4.2 Publicación: transacciones y concurrencia

- Los repositorios actuales abren una conexión por operación. Publicar exige varias sentencias atómicas, así que el repositorio de cursos usa **una sola conexión con `setAutoCommit(false)`** para toda la publicación: *commit* al final, *rollback* ante cualquier error.
- **Curso nuevo:** insertar en `courses` (con `latest_version = 1`) y en `course_versions`, en la misma transacción.
- **Versión nueva:** en la misma transacción, primero
  ```sql
  SELECT author_id, latest_version FROM courses WHERE id = ? FOR UPDATE
  ```
  El bloqueo de fila serializa dos publicaciones simultáneas del mismo curso: la segunda espera y ve el `latest_version` ya actualizado. Sin él, ambas calcularían el mismo número de versión y una fallaría con una violación de clave primaria.
  Después: comprobar el autor, comparar el `sha256` con el de la última versión, insertar la versión `latest_version + 1` y actualizar `latest_version`.
- Como las comprobaciones necesitan la fila bloqueada, el puerto devuelve un resultado sellado, igual que `UserRepository.add` comprueba e inserta de forma atómica. Por ejemplo:
  ```java
  AppendVersionResult appendVersion(UUID courseId, UUID requesterId, CourseVersionDraft draft);
  // Appended(CourseVersion) | CourseNotFound | NotAuthor | IdenticalToLatest
  ```
- Los instantes se truncan a microsegundos, como en el resto del proyecto.

### 4.3 Endpoints

| Método y ruta | Sesión | Qué hace | Respuestas |
|---|---|---|---|
| `POST /api/courses/validate` | sí | Valida sin guardar. Permite a los autores iterar, también con un LLM. | 200 con el informe, sea válido o no · 401 · 413 |
| `POST /api/courses` | sí | Publica un curso nuevo (versión 1). | 201 · 400 · 401 · 413 · 422 |
| `POST /api/courses/{id}/versions` | sí | Publica una versión nueva de un curso existente. | 201 · 400 · 401 · 403 · 404 · 409 · 413 · 422 |
| `GET /api/courses` | no | Busca entre las últimas versiones (§4.5). | 200 · 400 |
| `GET /api/courses/{id}` | no | Última versión, con contenido. | 200 · 404 |
| `GET /api/courses/{id}/versions/{version}` | no | Una versión concreta, con contenido. | 200 · 404 |

Conexión en `App`:

```java
config.routes.post("/api/courses/validate", requireSession.around(courses::validate));
config.routes.post("/api/courses", requireSession.around(courses::publish));
config.routes.post("/api/courses/{id}/versions", requireSession.around(courses::publishVersion));
config.routes.get("/api/courses", courses::search);
config.routes.get("/api/courses/{id}", courses::latest);
config.routes.get("/api/courses/{id}/versions/{version}", courses::version);
```

- El cuerpo de las publicaciones se lee con `ctx.bodyAsBytes()` y se entrega al lector. **No** se usa `ctx.bodyAsClass`.
- El autor es `authenticated.user().id()`. Nunca se lee del cuerpo.
- Un `{id}` que no es un UUID, o una `{version}` que no es un entero positivo, se responde como 404: no existe tal recurso.

### 4.4 Cuerpos de respuesta

**Problemas con el documento** (400, 413, 422): el informe de §3.7.

- 400: la capa 1 falla (no es UTF-8 o no es JSON válido).
- 413: el cuerpo supera 2 MiB (§4.7).
- 422: JSON válido que no supera las capas 2–5.

**Otros errores**: el `ErrorResponse` existente, añadiendo a `ErrorCode`:

| Código | Estado | Cuándo | `field` |
|---|---|---|---|
| `COURSE_NOT_FOUND` | 404 | No existe el curso o la versión. | `id` o `version` |
| `NOT_COURSE_AUTHOR` | 403 | Quien publica la versión no es el autor del curso. | `id` |
| `IDENTICAL_TO_LATEST` | 409 | El contenido es idéntico a la última versión (mismo `sha256`). No se crea versión. | `body` |
| `INVALID_PARAMETER` | 400 | Un parámetro de búsqueda no es válido (§4.5). | nombre del parámetro |

El 401 lo responde `RequireSession`, como en el resto de la API.

**Metadatos de una versión** (elementos de la búsqueda y respuesta 201):

```json
{
  "id": "0b8f…",
  "version": 2,
  "author": { "id": "5c1d…", "name": "Ana" },
  "publishedAt": "2026-11-18T08:35:00Z",
  "formatVersion": 1,
  "title": "Introducción a los grafos",
  "description": "…",
  "language": "es",
  "tags": ["grafos", "algoritmos"],
  "moduleCount": 2,
  "sha256": "9c1e…"
}
```

- El nombre del autor sale de un `JOIN` con `users`, para que la app no necesite otra petición por resultado.
- **201**: `{ "course": { …metadatos… }, "warnings": [ … ] }`.
- **GET con contenido**: los metadatos más `"content": { …el JSON del autor… }`. El contenido se inserta tal como está guardado, sin parsearlo ni volver a serializarlo (en Jackson 2, por ejemplo, con `@JsonRawValue` sobre un `String`).

### 4.5 Búsqueda

`GET /api/courses?q=&tag=&language=&page=&size=`

- **`q`**: subcadena en título o descripción.
  - Se normaliza (§3.4) y se busca en `search_text` con `LIKE ? ESCAPE '\'`.
  - **Escapa `%`, `_` y `\` en `q` antes de construir el patrón.** Si no, buscar «100%» se convierte en un comodín.
- **`tag`**: repetible; deben cumplirse **todas** (Y lógico). Se normalizan y se comprueba `search_tags @> ?`, que es exactamente «contiene todas» para arrays (`connection.createArrayOf("text", …)`).
- **`language`**: coincidencia exacta.
- Solo últimas versiones: `JOIN course_versions v ON v.course_id = c.id AND v.version = c.latest_version`.
- **Orden: `published_at DESC, course_id`.** El segundo criterio desempata: sin un orden total, dos cursos con la misma fecha podrían repetirse o saltarse entre páginas.
- **`page`** desde 0; **`size`** 20 por defecto, entre 1 y 100. Fuera de rango: 400 `INVALID_PARAMETER`.
- Respuesta: `{ "items": [ …metadatos… ], "page": 0, "size": 20, "total": 37 }`.

### 4.6 Versiones

- `POST /api/courses` crea el curso con `version = 1`.
- `POST /api/courses/{id}/versions` crea la versión `latest_version + 1`, solo si el usuario actual es el autor (§4.2).
- Al publicar una versión nueva, además del informe normal, se comparan los ids con la versión anterior. Si desaparecen ids de módulos, lecciones o ejercicios, se añade el aviso `REMOVED_IDS` con la lista: el progreso de los alumnos sobre ellos se perderá.
  - Implementarlo como función pura del dominio: `compareVersions(anterior, nueva) → avisos`. La versión anterior se lee de `content` con el lector (§3.1).

### 4.7 Límite de tamaño

- El límite es **2 MiB** (2 097 152 bytes).
- Javalin tiene su propio límite de cuerpo: en Javalin 6 era `config.http.maxRequestSize`, con 1 MB por defecto. **Comprueba el nombre y el valor por defecto en Javalin 7.2.4.** Si no se ajusta, Javalin rechazará los cursos de más de 1 MB con su propia respuesta, antes de llegar al controlador.
- Fija ese límite en 2 MiB y traduce el rechazo de Javalin al informe con `PAYLOAD_TOO_LARGE`, para que el cliente reciba siempre el mismo formato.

---

## 5. Contrato con la app (no se implementa aquí)

Se documenta porque la cota de §3.6 depende de ello. La app debe:

- **Barajar** las opciones y la columna derecha de los `matching` en cada intento. El orden del fichero no significa nada.
- En `multipleChoice`, **exigir al menos una opción marcada**.
- En `matching`, **exigir completar todas las parejas**.
- Sortear la prueba de dominio **sin reemplazo**, priorizando los ejercicios que no salieron en el intento anterior.

---

## 6. Pruebas

Siguiendo la práctica del proyecto: tests automáticos sin base de datos (`./gradus.sh test`), SQL probado a mano en un PostgreSQL 18 desechable y endpoints con Yaak.

**Tests automáticos (JUnit, tests primero):**

- **Reglas por código.** Construye los records directamente y comprueba cada código de las capas 3–5.
- **Normalización.** `"  Matriz   de ADYACENCIA "` equivale a `"matriz de adyacencia"`; «é» precompuesta equivale a «e» + U+0301.
- **Cota binomial.** Los cuatro valores de §3.6, con tolerancia 1e-12.
- **Ciclos.** El mensaje contiene el ciclo concreto. Un módulo que se requiere a sí mismo da `SELF_PREREQUISITE` y no `CYCLE` (la capa 3 falla y la 4 no se ejecuta).
- **`compareVersions`**: detecta ids eliminados y no avisa si solo se añaden.
- **Fixtures.** `ejemplo-grafos.json` es válido y **no produce ningún aviso**. Para cada código de error y de aviso de §3.3, un fixture que sea una mutación mínima del ejemplo y produzca **exactamente** ese código, con la ruta esperada. Test parametrizado sobre la carpeta de fixtures.
- **Schema y ejemplo sincronizados.** El ejemplo valida contra el mismo fichero de schema que usa producción.
- **Capa 1.** Claves duplicadas (`"answer": "a", "answer": "b"`) dan `DUPLICATE_KEY`; bytes UTF-8 inválidos dan `INVALID_JSON`; un BOM inicial se acepta.
- **Patrones del schema.** Un `title` terminado en salto de línea se rechaza.
- **Casos de uso**, con un repositorio en memoria: cada caso de cada resultado sellado (publicado, rechazado, curso inexistente, no autor, idéntico a la última versión).
- **Escapado de `q`** (§4.5), como función pura.

**SQL, a mano en PostgreSQL 18 desechable:**

- Aplicar el `004` dos veces seguidas sin efecto la segunda.
- Insertar curso y versiones; probar cada restricción `CHECK`.
- **Concurrencia:** en dos sesiones de `psql`, `BEGIN` y el `SELECT … FOR UPDATE` del mismo curso. La segunda debe quedarse esperando hasta el `COMMIT` de la primera, y entonces ver el `latest_version` nuevo.
- Búsqueda: `%` literal, varias etiquetas (todas obligatorias), paginación con fechas iguales sin repeticiones ni saltos.
- Borrar un usuario con cursos debe fallar por la clave ajena.

**Endpoints, con Yaak** (carpetas nuevas en el workspace «Gradus · Backend», reutilizando el token del inicio de sesión):

- Al menos una petición por código de estado de §4.3.
- Publicar dos veces el mismo contenido como versión da 409; un usuario distinto del autor da 403; sin sesión, 401.
- **Bytes conservados:** el SHA-256 del `content` devuelto por el GET coincide con el `sha256` de la publicación.
- Un curso de más de 1 MB y menos de 2 MiB se acepta; uno de más de 2 MiB da 413 con el informe.

---

## 7. Tareas

Para `backend/docs/tareas/publicacion-cursos.md`. Cada tarea termina con sus tests en verde; anota en «Progreso» la fecha y el número de tests, como en las tareas anteriores, y espera la aprobación del usuario antes de seguir.

- [ ] **1. Dominio del curso** (paquete `domain/course`). Records (`CourseDocument`, `Module`, `Mastery`, `sealed interface Step` con `Lesson`, `SingleChoice`, `MultipleChoice`, `Matching`), normalización de texto, `Issue` y `CourseIssueCode`. Tests primero.
  *Acepta si:* los records rechazan en el constructor lo que el schema ya prohíbe (por ejemplo, listas vacías), y los tests de normalización pasan.
- [ ] **2. Reglas de validación** (capas 3–5) y `compareVersions`, como funciones puras.
  *Acepta si:* hay al menos un test por código de las capas 3–5, los cuatro valores de la cota binomial coinciden y el mensaje de `CYCLE` muestra el ciclo concreto.
- [ ] **3. Lector** (capas 1–2): BOM, UTF-8 estricto, Jackson con detección de claves duplicadas, schema y mapeo a records. Informe completo. Requiere aprobar la dependencia de JSON Schema (§1.3).
  *Acepta si:* el ejemplo es válido y sin avisos, hay un fixture por cada código de §3.3 con su ruta, y un `title` con salto de línea final se rechaza.
- [ ] **4. Casos de uso**: `ValidateCourse`, `PublishCourse`, `PublishCourseVersion`, `FindCourse` y `SearchCourses`, con el puerto `CourseRepository` y un repositorio en memoria para los tests.
  *Acepta si:* los tests cubren cada caso de cada resultado sellado, y `REMOVED_IDS` aparece al publicar una versión que quita ids.
- [ ] **5. Infraestructura**: `004_create_courses.sql` y `JdbcCourseRepository` con transacción y `FOR UPDATE`.
  *Acepta si:* el SQL se prueba a mano según §6 (incluida la concurrencia con dos sesiones de `psql`) y `./gradus.sh db` aplica el `004` sobre la base existente.
- [ ] **6. Web**: `CourseController`, DTOs, rutas en `App`, códigos nuevos en `ErrorCode`, límite de 2 MiB y carpetas de Yaak «Cursos · Publicación» y «Cursos · Consulta».
  *Acepta si:* desde Yaak se obtienen todos los códigos de §4.3 y el SHA-256 del contenido devuelto coincide con el publicado.

---

## 8. Fuera de alcance (no implementar)

- Progreso de los alumnos: se especificará aparte.
- Imágenes y cursos empaquetados como `.zip`.
- Otros tipos de ejercicio.
- Editar o borrar cursos o versiones.
- Valoraciones, opiniones y chat.
- Pool de conexiones y errores 500 en JSON: ya están apuntados como pendientes del proyecto.
- Tests automáticos de repositorios y endpoints: pendientes hasta decidir el CI/CD.
- Portar las reglas a la app Android: se decidirá después. Mantenerlas en el dominio, sin dependencias externas (§3.1), es lo que deja esa puerta abierta.
