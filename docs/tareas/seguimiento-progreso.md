# Seguimiento del progreso (inscripción, sin conexión y sincronización)

Que un usuario se **inscriba** en un curso, estudie (también sin conexión) y su progreso se
**sincronice** con el backend para no perderlo ni al cerrar la app ni al cambiar de móvil.

Este documento es el **índice común**: decisiones, contrato, reparto en partes y acuerdos. El detalle
de cada parte vive en su propio documento, para que cada orquestador lea solo lo suyo.

Estado: **partes A (backend), B (`sharedLogic`), C (app) y D (sin conexión) terminadas; contrato congelado** (2026-11-26).
Siguiente: tarea 14 (cierre). Rama: `dev/course-progress-tracking`.

## Mapa de documentos (qué lee cada uno)

| Documento | Qué tiene | Lo leen |
|---|---|---|
| `AGENTS.md` | Reglas del proyecto | Todos |
| Este documento: **Relevo general** y **Decisiones** | Lo común a todas las partes | Todos los orquestadores |
| `backend/docs/api-progreso.md` | **Contrato** de `POST /api/progress/sync` (la única frontera entre backend y app) | Orquestadores A y B |
| `backend/docs/tareas/progreso-backend.md` | Parte A: tareas 1–3, su Relevo y su progreso | Orquestador A y sus implementadores |
| `docs/tareas/progreso-logica.md` | Parte B: tareas 4–6 (**lo crea su orquestador** al empezar, con la plantilla de abajo) | Orquestador B |
| `docs/tareas/progreso-app.md` | Partes C y D: tareas 7–13 (Relevo, decisiones C-1 a C-6 y las tareas de D) | Orquestador C/D |
| `docs/tareas/progreso-app-historico.md` | Lo terminado de la parte C, con su detalle | Solo si hace falta saber por qué se hizo algo |
| `docs/arquitectura-progreso.md` | **Referencia viva:** piezas, flujo, invariantes, cómo extenderlo y qué cuidar al mergear | Orquestadores B, C y D; quien mergee o añada algo que use el progreso |

Los implementadores leen `AGENTS.md`, el documento de **su** parte (Relevo y su paso) y la lista de
ficheros que les dé el orquestador. Este índice, solo si el orquestador lo indica.

## Relevo general

Se **reescribe** cuando cambia algo que afecta a más de una parte; no se acumula.

- **Dónde estamos (2026-11-26):**
  - **Parte A terminada** (tareas 1–3): `POST /api/progress/sync` funciona contra PostgreSQL y la `006`
    está aplicada en la base de desarrollo. Contrato congelado: `backend/docs/api-progreso.md`.
  - **Parte B terminada** (tareas 4–6): modelo, puertos, almacén en memoria, gateway HTTP y
    `ProgressSync`. Decisiones B-1 a B-11 en `docs/tareas/progreso-logica.md`.
  - **Parte C terminada** (tareas 7–10): la app deja inscribirse (página de relleno desde Buscar), no
    deja estudiar sin inscripción, guarda el progreso en Room por usuario y lo sincroniza tras iniciar
    sesión, inscribirse y dominar. Probado de punta a punta por los usuarios (con `pm clear`, lo dominado
    vuelve del servidor). Detalle: `docs/tareas/progreso-app.md` y su histórico.
  - **Parte D terminada (2026-11-26):** tareas 11 (sesión cifrada y arranque según sesión), 13
    (sincronización con WorkManager 2.12.0) y 12 (copia local de cursos en `ProgressDatabase` v2 y precarga
    al inscribirse) terminadas, revisadas y probadas de punta a punta en el emulador por los usuarios.
    Detalle en `docs/tareas/progreso-app-historico.md` y referencia viva en `docs/arquitectura-progreso.md`.
- **Lo común que cambió con D (hecho):** `SessionStore` cifrado en disco sin cambiar el puerto (D-2);
  sincronización con WorkManager 2.12.0 y `ProgressSyncLauncher` borrado (D-4); `ProgressDatabase` en
  versión 2 con `course_copies` y precarga al inscribirse (D-3). Los puertos de la parte B y el contrato
  siguen intactos.
- **Siguiente:** commit de `12b-2` + cierre de D (por los usuarios) y **tarea 14 (cierre)**: congelar el
  esquema de Room (C-4), pasar la batería completa de tests (`sharedLogic`, `app` JVM + todos los
  instrumentados y `backend`), repaso final y actualizar `docs/especificacion.md` §4.4 y §8.
- **Dependencias entre partes:**
  - A, B, C y D: **terminadas**. Solo queda el cierre (tarea 14).
- **Cambios de contrato:** quien necesite cambiar `api-progreso.md` lo propone a los usuarios, lo
  apunta aquí («Contrato: cambio pendiente…») y los usuarios avisan al otro orquestador. Ningún
  orquestador cambia el contrato por su cuenta después de congelarlo.
- **Abierto (común):**
  - Dependencias: Room 2.8.5 + KSP 2.3.14 (C-1), DataStore 1.3.0 (11b) y WorkManager 2.12.0 + `work-testing`
    2.12.0 (13b-1) ya están aprobados y añadidos. La tarea 12 no necesita dependencias nuevas.
  - Coordinación con otras ramas: la migración es la `006` (la `005` es de valoraciones), y la rama
    `dev/joel` toca `CourseViewModel`, `ScreenCourse`, `MainNavGraph`, `AppContainer`,
    `libs.versions.toml` y `build.gradle.kts`: tocarlos lo mínimo (decisión 5).

## Decisiones (aprobadas el 2026-11-24)

1. **Qué se guarda:** inscripciones y módulos dominados con su fecha. El registro de cada respuesta
   (§4.4) y los repasos SM-2 quedan para después; el diseño no lo impide. Se guarda el **hecho**
   «dominó X el día Y», no se recalcula (si el autor cambia `minCorrect`, lo dominado no cambia).
2. **Inscripción explícita** con un botón. **No hay inscripción automática.**
3. **Imposible empezar un módulo sin inscripción**, en tres capas:
   - flujo: la inscripción va antes de estudiar;
   - lógica: `studyAccess(enrolled)` → `Allowed | NotEnrolled`, que consultan `onModuleClick` y la
     entrada de `Route.Exercise`;
   - invariante: `markMastered` sobre un curso no inscrito lanza excepción (es un bug, no se ignora).
   En el servidor, los módulos dominados van **anidados** en su inscripción: no pueden existir sin ella.
4. **El camino se puede ver sin inscripción** (vista previa); tocar un módulo enseña un aviso.
5. **Agnóstico a la interfaz:** la lógica devuelve desenlaces sellados y la interfaz pone los textos.
   El botón y su página se deciden en la parte C, en ficheros propios.
6. **Por cuenta, no por móvil:** clave `(usuario, curso, módulo)`, sin la versión del curso (los ids
   de módulo son estables entre versiones; `especificacion-cursos-v1.md` §2.4).
7. **Sin «darse de baja».**
8. **Room en `app`**, con los puertos en `sharedLogic`. El puerto `ProgressStore` **no cambia**: la
   implementación lee el usuario de `SessionStore`.
9. **Un solo endpoint de sincronización** (`POST /api/progress/sync`, ver el contrato): fusionar es una
   unión, idempotente; una entrada mala se omite y se informa, sin bloquear la cola.
10. **Primero sincronizar con la app abierta**; WorkManager y la sesión persistente, después.
11. La `Session` se pasa como parámetro a cada llamada (no es estado global).

## Partes y tareas

La casilla ✅ la marca el revisor (el orquestador de la parte), no quien implementa.

| Parte | Tareas | Documento | Orquestador |
|---|---|---|---|
| **A · Backend** | 1 `SyncProgress` (dominio y caso de uso) · 2 SQL + JDBC · 3 ruta y contrato | `backend/docs/tareas/progreso-backend.md` | Propio |
| **B · `sharedLogic`** | 4 modelo, puertos y `studyAccess` · 5 `ProgressGateway` HTTP · 6 `ProgressSync` | `docs/tareas/progreso-logica.md` | Propio |
| **C · `app`** | 7 Room · 8 sincronizar con la app abierta · 9 la puerta en la interfaz · 10 botón de inscribirse | `docs/tareas/progreso-app.md` | Propio (C y D juntos) |
| **D · Sin conexión** | 11 sesión persistente · 12 copia local del curso · 13 WorkManager | `docs/tareas/progreso-app.md` | El de C |
| **Cierre** | 14 repaso final, `especificacion.md` §4.4 y §8 | Este documento | El último que termine |

Resumen de lo que pide cada tarea de B, C y D (el detalle lo escribe su orquestador en su documento):

- **4.** `Enrollment`, `EnrollmentStore` (+ en memoria), `studyAccess(enrolled)`; `markMastered` exige
  inscripción; el almacén en memoria pasa a ser por usuario. Tests: no inscrito no abre ni frontera ni
  dominado; inscrito sin conexión sí; por usuario; inscribirse dos veces; dominar sin inscripción
  lanza excepción y no deja filas.
- **5.** `ProgressGateway` + `HttpProgressGateway` (Ktor) + DTO + *mapper* + Fake; desenlaces
  `Synced | SessionExpired | Unreachable | ServerFailure`. Tests con `MockEngine` (200, 401, 400, sin
  conexión, 5xx, cuerpo ilegible). Es la **primera llamada con sesión** de la app.
- **6.** `ProgressSync`: sube lo pendiente, fusiona la respuesta y marca lo subido. Tests: sin nada
  pendiente también llama y trae lo del servidor (B-10, 2026-11-25); fallo de red deja lo pendiente;
  401 no reintenta; lo dominado en otro móvil aparece; lo omitido por el servidor sale de la cola.
- **7.** Room por usuario (inscripciones, dominados, pendientes); `AppContainer` con `Context`.
- **8.** Sincronizar tras iniciar sesión, tras dominar y al volver la conexión.
- **9.** El camino usa `studyAccess`; `Route.Exercise` vuelve atrás sin inscripción.
- **10.** `EnrollmentViewModel` + `EnrollButton` en ficheros propios; aquí se decide dónde va.
- **11–13.** Sesión guardada cifrada; copia local del curso al inscribirse; WorkManager.

## Alcance y fuera de alcance

- **Dentro:** lo de la tabla de partes.
- **Fuera:** darse de baja; registro de respuestas y SM-2; Inicio real («cursos inscritos con su
  porcentaje»); reanudar a mitad de un módulo; actualizar la copia local con versiones nuevas;
  comprobar el SHA-256.

## Acuerdos de proceso (para todas las partes)

- **Un orquestador por parte** (o uno nuevo cuando el chat se alargue). Al empezar lee `AGENTS.md`,
  el Relevo general, las Decisiones y el documento de su parte; al terminar la sesión **reescribe el
  Relevo de su parte** (y el general, si cambia algo común).
- **El orquestador** planifica los pasos (puede juntar pasos triviales de la misma naturaleza; el
  criterio es el riesgo, no el tamaño), indica **modelo y esfuerzo** de cada paso (el mínimo que
  mantiene la calidad), escribe el mensaje para quien implementa, revisa, marca la casilla y propone
  el **mensaje de commit** con el estilo del repositorio: `add:`/`fix:`/`test:`/`docs:` + descripción
  en inglés + `(progress task N)`. Los commits los hacen los usuarios.
- **Mensaje al implementador** (plantilla; se adapta):
  1. Rol y paso: «Eres quien implementa el paso X del seguimiento del progreso de Gradus».
  2. Qué leer: `AGENTS.md`, el Relevo de la parte y el paso X; **la lista de ficheros** que se tocan
     y, si hace falta, uno de ejemplo. Nada más salvo que lo necesite.
  3. **Primero, proponer:** ficheros, forma del código y lista de tests; **esperar el OK**. Que el paso
     esté detallado en el documento **no** significa que esté aprobado.
  4. Tras el OK: tests primero (rojo → verde → refactor); tests nuevos en ficheros nuevos y temáticos.
  5. Al terminar: los comandos del paso; en «Progreso», lo necesario y «pendiente de revisión»; **sin
     marcar la casilla**; fragmentos clave del código y decisiones fuera del plan; sin commits.
- **Los tests también documentan:** cada comportamiento que aparezca en el código, un comentario o el
  progreso tiene su test; las listas de los criterios son el mínimo.
- **En «Progreso»**, lo que alguien necesita para continuar o entender una decisión; lo que ya cuentan
  el diff, los tests o el commit no se repite. Lo terminado y largo pasa a un histórico de la parte.
- Sin permiso, nada se escribe en la base de desarrollo (cuentas, cursos, progreso).
- Nombres de test en Kotlin sin comas ni paréntesis (no compilan para iOS).
- Gradle: `--rerun` cuando haga falta que los tests se ejecuten de verdad.
- En el emulador, solo las clases de test del paso; la batería completa, al cierre (tarea 14).

## Progreso

- 2026-11-24: definición hecha y aprobada (decisiones 1–11). Documento partido en índice + documentos
  por parte para que cada orquestador cargue solo lo suyo. Creados `backend/docs/api-progreso.md`
  (borrador) y `backend/docs/tareas/progreso-backend.md`.
- 2026-11-25: **parte A terminada y contrato congelado** (tarea 3 de `progreso-backend.md`).
- 2026-11-25: **parte B terminada** (tareas 4-6 de `progreso-logica.md`). Creada la referencia viva
  `docs/arquitectura-progreso.md`. Lo que necesita la parte C, en el Relevo general.
- 2026-11-26: **parte C terminada** (tareas 7–10 de `progreso-app.md`; lo terminado, en
  `progreso-app-historico.md`). Lo que necesita la parte D, en el Relevo general y en el de `progreso-app.md`.
- 2026-11-26: **parte D terminada** (tareas 11, 13 y 12: sesión cifrada, WorkManager 2.12.0 y copia local
  de cursos en Room v2; detalle en `progreso-app-historico.md`). Probada en el emulador por los usuarios.
  Siguiente: tarea 14 (cierre).
