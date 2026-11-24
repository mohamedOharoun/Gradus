# Notas con cámara

Fotografiar los apuntes de papel y guardarlos junto al módulo del curso al que pertenecen, para
tenerlos a mano al estudiarlo o repasarlo. Es el requisito de la asignatura «al menos una API del
dispositivo» (`especificacion.md` §8: «Cámara para fotografiar apuntes y asociarlos a módulos») y
también cubre «base de datos local SQLite» (Room).

<<<<<<< HEAD
Estado: **hecho (2026-11-24)**. Decisiones S–Z aprobadas el 2026-11-24 (con las dependencias de T y V). No hay Figma de esta parte:
=======
Estado: **terminada** (2026-11-24). Decisiones S–Z aprobadas el 2026-11-24 (con las dependencias de T y V). No hay Figma de esta parte:
>>>>>>> 5031586c8855efd3e3619ec7062fa79a94b74330
los bocetos de abajo hacen de diseño (si no coinciden con un Figma posterior, manda el Figma).

## Lo que había antes de empezar

- Tocar un módulo del camino (desbloqueado o dominado) abría directamente su estudio.
- El progreso (`ProgressStore`) vivía en memoria; no había base de datos en el móvil.
- Ni CameraX ni Room estaban en `gradle/libs.versions.toml`. El Manifest solo pedía `INTERNET`.

Hoy: Room 2.8.5 (`gradus.db`) y CameraX 1.7.0; permiso `CAMERA` con `uses-feature camera.any`
opcional; la hoja del módulo da paso a «Mis notas», la cámara y el visor (ver «Progreso»).

## Cómo se usa (bocetos)

**1. En el camino, tocar un módulo abre su hoja** (en lugar de entrar directamente a estudiar):

```
┌──────────────────────────────────┐
│ ─────                            │  hoja inferior (bottom sheet)
│ BFS                    ✓ Dominado│
│ Recorrer un grafo por niveles.   │
│                                  │
│ [        Repasar módulo        ] │  (o «Estudiar» si aún no está dominado)
│ [ 📷  Mis notas · 3            ] │
└──────────────────────────────────┘
```

**2. Mis notas de un módulo:** cuadrícula de fotos, la más reciente primero.

```
← Notas · BFS
┌────────┐┌────────┐┌────────┐
│  foto  ││  foto  ││  foto  │
│ 24 nov ││ 22 nov ││ 20 nov │
└────────┘└────────┘└────────┘
[ 📷 Hacer foto ]  [ 🖼 De la galería ]

Sin notas:  «Aún no tienes notas de este módulo. Haz una foto de tus apuntes.»
```

**3. Hacer foto:** la cámara dentro de la app; después, revisar.

```
┌──────────────────────┐      ┌──────────────────────┐
│ ✕                    │      │ ✕                    │
│                      │      │      (la foto)       │
│   vista de cámara    │  →   │                      │
│                      │      │ Pie (opcional) [____]│
│         ( ◯ )        │      │ [Repetir]  [Guardar] │
└──────────────────────┘      └──────────────────────┘
```

**4. Ver una nota:** a pantalla completa, con zoom (pellizcar), su fecha y su pie; «Eliminar»
pide confirmación.

## Decisiones (aprobadas el 2026-11-24)

- **S · Por dónde se entra: la hoja del módulo en el camino** (boceto 1). Las notas se pueden ver
  también en módulos ya dominados sin tener que estudiarlos. Cuesta un toque más para estudiar.
  Alternativa: un icono 📷 en la barra del estudio (solo se llega estudiando). Se puede añadir
  después como atajo.
- **T · La cámara con CameraX, dentro de la app** (boceto 3). Es «usar la API de la cámara» de verdad
  y la interfaz es la nuestra (accesible, en claro y oscuro). **Dependencias nuevas:**
  `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`. Permiso `CAMERA` en el
  Manifest y pedido en tiempo de ejecución.
  Alternativa sin dependencias: pedir la foto a la app de cámara del móvil
  (`ActivityResultContracts.TakePicture`); necesita un `FileProvider` en el Manifest y la
  interfaz no es nuestra.
- **U · También «De la galería»** con el selector de fotos de Android (`PickVisualMedia`): no pide
  permisos ni dependencias. La foto se copia a la carpeta de la app, como las de la cámara.
- **V · Base de datos: Room**, como dice la especificación. **Dependencias nuevas:** `room-runtime`,
  `room-ktx`, `room-compiler` y el plugin **KSP** (hay que tocar Gradle). Va en `app/` detrás de un
  puerto `NotesStore` de `sharedLogic` (como `SettingsStore`), así la lógica y los tests no saben
  de Room. Alternativa sin dependencias: `SQLiteOpenHelper` de Android (más código a mano).
- **W · Las fotos son ficheros** en la carpeta privada de la app (`files/notes/…jpg`); en la base de
  datos solo va su ruta, el curso, el módulo, la fecha y el pie (especificación §8). Se quedan en
  el móvil: no se suben al servidor y se borran al desinstalar la app. Son del móvil, no de la
  cuenta (como las etiquetas preferidas).
- **X · Si no se da permiso de cámara:** se explica para qué hace falta y «De la galería» sigue
  funcionando. Si se denegó para siempre, el botón lleva a los ajustes del sistema.
- **Y · Pie de foto opcional** (hasta 100 caracteres). Es lo que lee TalkBack («Nota del 24 de
  noviembre: Dijkstra paso a paso»); sin pie, solo la fecha.
- **Z · Eliminar pide confirmación** y borra el fichero y la fila. No hay papelera.

## Tareas

1. **`sharedLogic`** (test primero): `Note` (id, curso, módulo, fichero, pie, fecha), reglas del
   pie, el puerto `NotesStore` (las de un módulo como `Flow`, añadir, eliminar, cuántas hay por
   módulo) e `InMemoryNotesStore` para tests y previews. (Los textos de la fecha, «24 nov», van en
   la app con `java.time`: `sharedLogic` no tiene librería de fechas; tarea 4.)
2. **Room** (con las dependencias aprobadas): `NoteEntity`, `NoteDao`, `GradusDatabase`,
   `RoomNotesStore`; tests instrumentados (como `SharedPreferencesSettingsStoreTest`).
3. **Hoja del módulo** en el camino (boceto 1): Estudiar / Repasar y «Mis notas · N». Cambia qué
   hace tocar un módulo (hoy entra directamente a estudiar).
4. **Pantalla «Mis notas»** (boceto 2): cuadrícula, vacío, «De la galería» (copia la foto).
5. **Cámara** (boceto 3): permiso, vista previa y captura con CameraX, revisar (Repetir / Guardar)
   con el pie.
6. **Ver una nota** (boceto 4): pantalla completa con zoom, pie, fecha y eliminar.
7. **Cierre:** `especificacion.md` (quitar «pendiente de diseñar»), prueba en el emulador (tiene
   cámara virtual) y en claro / oscuro con TalkBack.

Cada pantalla: sin estado, con su ViewModel, previews en claro y oscuro y test de emulador.

## Fuera de alcance

Subir las notas al servidor o compartirlas, reconocer el texto de la foto (OCR), recortar o editar
la foto, notas de texto sin foto, guardar el progreso en Room (podría hacerse después aprovechando
la misma base de datos).

## Progreso

- 2026-11-24: documento creado con el diseño, las decisiones S–Z y las tareas. Aprobado el mismo día.

- 2026-11-24: **tarea 1 hecha** (`sharedLogic`, paquete `notes/`).
  - `Note` (id, curso, módulo, fichero, pie, `createdAt` en milisegundos desde 1970: no hay
    librería de fechas en `sharedLogic`).
  - `checkCaption` → `CaptionCheck` (`Valid(pie o null) | TooLong`): espacios de sobra fuera,
    vacío = sin pie, hasta `MAX_CAPTION_LENGTH = 100` caracteres (un emoji cuenta uno).
  - Puerto `NotesStore` (`notes` de un módulo como `Flow`, `counts` por módulo, `find`, `add`,
    `delete` que devuelve la nota para borrar su fichero), regla de orden `newestFirst` (fecha y,
    a igual fecha, id) e `InMemoryNotesStore`.
  - Tests: `NoteCaptionTest` (5) e `InMemoryNotesStoreTest` (13), en rojo antes (12 fallaban).
    sharedLogic: 749, todos pasan.

- 2026-11-24: **tarea 2 hecha** (Room).
  - Versiones consultadas en los repositorios oficiales: **Room 2.8.5** y **KSP 2.3.14** (desde 2.3,
    KSP no depende de la versión de Kotlin; funciona con el Kotlin integrado de AGP 9.5).
  - Gradle: `room` y `ksp` en `libs.versions.toml` (versiones, librerías `androidx-room-*` y el
    plugin `ksp`); `apply false` en el `build.gradle.kts` raíz; en `app/build.gradle.kts`, el plugin,
    `room-runtime`, `room-ktx` y `ksp(room-compiler)`.
  - `app/.../data/notes/`: `NoteEntity` (tabla `notes`, índice por curso y módulo, y la traducción
    a/desde `Note`), `NoteDao` (consultas comprobadas al compilar; mismo orden que `newestFirst`;
    `takeOut` busca y borra en una transacción), `GradusDatabase` (versión 1, `exportSchema = false`)
    y `RoomNotesStore` (adaptador del puerto).
  - `AppContainer`: base de datos `gradus.db` y `notesStore`.
  - Test instrumentado `RoomNotesStoreTest` (11, base de datos en memoria): las mismas reglas que
    `InMemoryNotesStoreTest`, más que todos los campos vuelven iguales y una nota sin pie. En rojo
    antes (9 fallaban); ahora pasan los 11.

- 2026-11-24: **tarea 3 hecha** (hoja del módulo en el camino).
  - `CourseViewModel`: tocar un módulo de la frontera o dominado abre su hoja (`sheet`), ya no entra
    directamente a estudiar; `onStudy` / `onOpenNotes` / `onSheetDismiss` / `onNotesOpened`;
    `noteCounts` por módulo, escuchando `NotesStore.counts` junto al progreso (`combine`). Recibe
    `NotesStore` (por defecto, en memoria). Los bloqueados siguen igual (aviso de qué falta).
  - `screens/course/`: `ModuleSheet` (`ModalBottomSheet`) con `ModuleSheetContent` aparte (para
    probarlo y previsualizarlo), `ModuleSheetActions`, `ModuleSheetTexts` (Estudiar/Repasar, Por
    estudiar/Dominado, «Mis notas · 3», y para TalkBack «Mis notas, 3 fotos»). Icono nuevo
    `ic_photo_camera` (Symbols Rounded).
  - Navegación: `Route.ModuleNotes(courseId, moduleId)` y `ModuleNotesDestination.kt` con una
    pantalla provisional (la tarea 4 la sustituye). En `MainNavGraph.kt` solo cambian las líneas del
    camino (nuestras): el `NotesStore`, el evento `notesToOpen` y las acciones de la hoja; y una
    línea para el destino.
  - Tests: `CourseViewModelTest` (12; no había tests de este ViewModel; 7 fallaban antes),
    `ModuleSheetTextsTest` (4) y `ModuleSheetTest` (5, emulador). Todos pasan.
  - Emulador con el backend (se entra al curso desde Buscar: tras el merge, Inicio ya no tiene el
    botón provisional): la hoja se abre en «Representar un grafo» con «Por estudiar»; «Estudiar»
    abre la práctica; «Mis notas» abre la pantalla provisional y «atrás» vuelve al camino; el
    módulo bloqueado sigue avisando de qué falta.
  - Visto: la hoja de Material trae «Close sheet» y «Drag handle» en el idioma del sistema (el
    emulador está en inglés). Es de Material; se verá con el idioma (fase 3b de Ajustes).

- 2026-11-24: **tarea 4 hecha** (pantalla «Mis notas» y «De la galería»).
  - `data/notes/PhotoFiles.kt`: puerto `PhotoFiles` (`import(uri) → ruta | null`, `delete(ruta)`) y
    `AndroidPhotoFiles` (copia con el `ContentResolver` a `files/notes/<uuid>.jpg` en `Dispatchers.IO`;
    si falla no deja una copia a medias; `delete` no sale de la carpeta de notas). En `AppContainer`.
  - `screens/notes/`: `NoteTexts` («24 nov», «20 dic 2025», y para TalkBack «Nota del 24 de
    noviembre: pie»; meses en una lista propia), `ModuleNotesViewModel` (escucha las notas; añade la
    elegida sin pie; `adding` / `addFailed`; ignora otra mientras copia), `ScreenModuleNotes`
    (cuadrícula adaptable, vacío, «Hacer foto» desactivado hasta la tarea 5, «De la galería») y previews.
  - `components/NotePhoto.kt`: lee la foto reducida con `BitmapFactory` (sin librerías de imágenes) y
    fuera del hilo de la pantalla; recuadro gris mientras carga o si no existe.
  - `ModuleNotesDestination.kt`: sustituye la pantalla provisional; el selector de fotos
    (`PickVisualMedia`, sin permisos); el título sale del curso en la caché.
  - Tests: `NoteTextsTest` (8) y `ModuleNotesViewModelTest` (9) en el ordenador, en rojo antes;
    `AndroidPhotoFilesTest` (5, emulador, en rojo antes) y `ScreenModuleNotesTest` (9, emulador).
  - Emulador con el backend: «Mis notas» vacía → «De la galería» → selector del sistema («Dominio
    will only have access to the photos you select») → la foto se copia (mismo tamaño que la original)
    y sale con «24 nov»; la hoja del módulo pasa a «Mis notas · 1». La nota se deja para la tarea 6.
  - Visto: el selector dice «Dominio»: es el `app_name` de `strings.xml`, que nunca se cambió a
    Gradus. No se toca aquí.

- 2026-11-24: **tarea 5 hecha** (la cámara).
  - CameraX **1.7.0** (consultada en el repositorio de Google): `camera-core`, `camera-camera2`,
    `camera-lifecycle` y `camera-view` en `libs.versions.toml` y `app/build.gradle.kts`. Existe
    `camera-compose` estable (vista previa hecha en Compose), pero no estaba aprobada: se usa
    `camera-view` (`PreviewView` dentro de `AndroidView`).
  - Manifest: permiso `CAMERA` y `uses-feature camera.any` con `required="false"` (sin cámara, solo
    «De la galería»; «Hacer foto» desactivado).
  - `PhotoFiles`: `newCapture` (fichero temporal en `cache/captures/`), `keepCapture` (lo pasa a
    `files/notes/`) y `discardCapture` (solo dentro de `captures`).
  - `screens/camera/`: `CameraViewModel` (máquina de estados `NeedsPermission → Ready → Capturing →
    Reviewing → Closed`, sin CameraX: se prueba en el ordenador), `ScreenCamera` sin estado (la vista
    previa entra por un hueco), previews y `CameraXPreview.kt` (`LifecycleCameraController`, solo
    encendido con permiso y mientras se dispara).
  - `TakeNoteDestination.kt`: permiso (`RequestPermission`; denegado para siempre si
    `shouldShowRequestPermissionRationale` es false → «Abrir ajustes»; al volver se mira otra vez),
    disparar, «atrás» tira la foto sin guardar. `Route.TakeNote` y una línea en `MainNavGraph.kt`.
  - `NotePhoto` gira la foto según su EXIF (muchas cámaras la guardan «tumbada»).
  - Tests: `CameraViewModelTest` (23, en rojo antes), 5 más en `AndroidPhotoFilesTest` (10 en total,
    en rojo antes), `ScreenCameraTest` (12, emulador). Todos pasan.
  - Emulador (cámara virtual): «Hacer foto» → diálogo del sistema → «Don't allow» → nuestra
    explicación → «Permitir la cámara» → «While using the app» → vista previa → foto → revisar →
    «Repetir» (la temporal se borra) → otra foto con pie «Grafo de la clase» → «Guardar» → «Mis notas»
    con la nota («Nota del 24 de noviembre: Grafo de la clase»); sin fotos temporales al acabar.
    El caso «denegado para siempre» queda cubierto por los tests (no se probó a mano).
  - Visto: el diálogo del sistema también dice «Dominio» (`app_name`).

- 2026-11-24: **tarea 6 hecha** (ver una nota y eliminarla).
  - `NoteViewerViewModel` (`Loading | Showing(nota, confirmingDelete, deleting) | Gone`): eliminar
    pide confirmación; al confirmar borra la fila y su foto, una sola vez; una nota que no existe → `Gone`.
  - `NoteTexts.noteFullDate` («24 de noviembre», con el año si es otro); `noteDescription` la reutiliza.
  - `components/ZoomablePhoto.kt`: pellizcar (1× a 5×) y arrastrar con `transformable`; doble toque
    amplía o vuelve; `graphicsLayer` mueve lo ya dibujado sin volver a leer la foto.
  - `ScreenNoteViewer` (fecha como título, papelera de 48 dp, foto con zoom, pie; diálogo «¿Eliminar
    esta nota?») y previews. `NoteViewerDestination.kt`, `Route.NoteViewer`, una línea en `MainNavGraph.kt`.
  - «Mis notas»: cada foto es un botón («Ver la nota» para TalkBack) que abre el visor
    (`onlyWhenResumed`: dos toques rápidos no la abren dos veces).
  - Tests: `NoteViewerViewModelTest` (9) y uno más en `NoteTextsTest`, en rojo antes;
    `ScreenNoteViewerTest` (7, emulador) y uno más en `ScreenModuleNotesTest` (10). Todos pasan.
  - Emulador: nota desde la galería → tocarla abre el visor («24 de noviembre») → doble toque amplía →
    «Eliminar nota» → diálogo → «Cancelar» la conserva → «Eliminar» vuelve a «Mis notas» vacía y la
    foto ya no está en `files/notes`. El pellizco no se puede hacer con `adb`: queda para probarlo a mano.

<<<<<<< HEAD
- 2026-11-24: **tarea 7 hecha (cierre)**; **notas con cámara terminadas**.
  - `especificacion.md`: la hoja del módulo, las notas con cámara descritas (§5), «pendiente de
    diseñar» solo los repasos, y en §8 la cámara (CameraX) y Room (`gradus.db`).
    `resumen-proyecto.md` puesto al día (Ajustes, hoja, notas, la API del dispositivo y los límites).
  - Tests: sharedLogic 749, app en el ordenador 320 y backend 616, todos bien. En el emulador, la
    primera ejecución completa dio 3 fallos (uno de `ScreenCameraTest` y dos de `FilterSheetTest`, del
    compañero) por el emulador saturado (carga 14 tras dos horas, «System has crashed»). Tras
    reiniciarlo en frío: `FilterSheetTest` 61/61, y los de la cámara y lo relacionado 55/55
    (`RoomNotesStoreTest`, `AndroidPhotoFilesTest`, `ModuleSheetTest`, `ScreenModuleNotesTest`,
    `ScreenCameraTest`, `ScreenNoteViewerTest`). A petición del usuario, no se repitió la batería entera.
  - Vuelta en el emulador, en claro y en oscuro: hoja del módulo → «Mis notas» → permiso → cámara →
    foto con pie → guardar → ver → eliminar (cancelado) → «Repetir» sin dejar temporales; la hoja dice
    «Mis notas · 1».
  - TalkBack: se activó, pero `adb` no puede hacer sus gestos (el foco se queda en «Volver»). Lo que
    lee cada elemento está comprobado en los tests de pantalla (descripciones y roles). Queda una pasada
    a mano si se quiere. Se desactivó al acabar.

## Siguiente paso

Hecho. Ideas para después: guardar también el progreso en esta base de datos Room; un atajo 📷 en
la barra del estudio (alternativa de la decisión S); cambiar `app_name` («Dominio») a Gradus.
=======
- 2026-11-24: **tarea 7 hecha** (cierre). **Tarea completada.**
  - `especificacion.md` ya describe las notas con cámara (§6 y tabla del §8); en «Pendiente de
    diseñar» solo quedan los repasos.
  - Comprobado por el equipo: compila; pasan los tests de `sharedLogic`, `testDebugUnitTest` y los
    del emulador (`connectedDebugAndroidTest`); vuelta completa en el emulador en claro y oscuro;
    a mano, el pellizco del visor y «denegado para siempre → Abrir ajustes»; TalkBack lee las notas
    («Nota del 24 de noviembre: …») y la hoja («Mis notas, 3 fotos»).

## Pendiente para más adelante

- `app_name` sigue siendo «Dominio»: es lo que muestran el diálogo del permiso y el selector de
  fotos. Cambiarlo a «Gradus» es otra tarea (afecta a `strings.xml`).
- Guardar el progreso en Room, aprovechando la misma base de datos (fuera de alcance aquí).

## Siguiente paso

Ninguno en esta funcionalidad.
>>>>>>> 5031586c8855efd3e3619ec7062fa79a94b74330
