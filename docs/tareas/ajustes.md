# Ajustes

Pestaña **Ajustes** (Figma «6 · Ajustes»; captura del usuario del 2026-11-22). Desde el 2026-11-22
la hacemos nosotros (antes era del compañero). La parte de backend que haga falta también es nuestra.

Estado: **fases 1 y 2 hechas (2026-11-23)**; fase 3 sin empezar. Decisiones A–E aprobadas el 2026-11-22.

## El diseño (Figma)

```
Ajustes
CUENTA
┌───────────────────────────────────────────┐
│ (MO)  Nombre                       Editar │
│       nombre@ejemplo.com                  │
│───────────────────────────────────────────│
│ 🔒 Contraseña                  Cambiar  › │
└───────────────────────────────────────────┘
APARIENCIA E IDIOMA
┌───────────────────────────────────────────┐
│ ☾ Modo oscuro                       ( o ) │
│ 🌐 Idioma                      Español  › │
└───────────────────────────────────────────┘
TEMAS QUE TE INTERESAN
┌───────────────────────────────────────────┐
│ # Etiquetas preferidas             Editar │
│ Verás antes los cursos con estas etiquetas│
│ en Inicio y en Buscar.                    │
│ (#grafos) (#kotlin) (#algoritmos)         │
│ (#bases de datos) (+ Añadir)              │
└───────────────────────────────────────────┘
SESIÓN
┌───────────────────────────────────────────┐
│ ⇤ Cerrar sesión                           │
│ 🗑 Eliminar cuenta                (rojo)  │
└───────────────────────────────────────────┘
```

## Qué se puede hacer hoy

| En el Figma | Qué hace falta | Fase |
|---|---|---|
| Perfil: iniciales, nombre y correo | Ya están en la sesión (`Session.account`) | **1** |
| «Editar» el perfil | Ruta nueva en el backend | 2 |
| Contraseña · «Cambiar» | Ruta nueva en el backend | 2 |
| Modo oscuro | Guardarlo en el móvil y aplicarlo al tema | **1** |
| Idioma | Pasar todos los textos a `strings.xml` | 3 |
| Etiquetas preferidas | Guardarlas; ordenar Buscar las toca (es del compañero) | 3 |
| Cerrar sesión | `DELETE /api/sessions/current` (ya existe) | **1** |
| Eliminar cuenta | Ruta nueva en el backend | 2 |

## Fase 1 (sin cambios en el backend)

### Tareas

1. ✅ **Hecha (2026-11-22).** **Cerrar sesión en `sharedLogic`** (test primero).
   - `AccountGateway.logOut(session): LogOutOutcome` (`LoggedOut | Unreachable | ServerFailure`).
     Un 401 cuenta como `LoggedOut`: la sesión ya no valía en el servidor.
   - `HttpAccountGateway`: `DELETE /api/sessions/current` con `Authorization: Bearer <token>`.
   - `FakeAccountGateway`: responde `LoggedOut` y recuerda la sesión que le pidieron cerrar.
   - ✅ Criterio: tests con `MockEngine` (204, 401, 5xx, sin conexión, la cabecera con el token).
2. ✅ **Hecha (2026-11-22).** **Preferencias y textos** (`sharedLogic`, test primero).
   - `settings/AppSettings.kt`: el modo oscuro elegido (`null` = el del sistema) e `isDark(systemDark)`.
   - `settings/SettingsStore.kt`: el puerto (`settings: Flow<AppSettings>`, `setDarkMode`) e
     `InMemorySettingsStore` para tests y previews.
   - `account/Initials.kt`: `initialsOf(name)` y `Account.initials`. Primera letra de la primera y
     de la última palabra: «Mohamed Oharoun» → «MO»; «Ana» → «A»; sin nombre → «?».
   - ✅ Criterio: tests de las iniciales (tildes, espacios de sobra, un solo nombre) y del store.
3. **Guardar el modo oscuro en el móvil** (`app`).
   - `data/SharedPreferencesSettingsStore`: `SettingsStore` con `SharedPreferences` (es de
     Android: **no es una dependencia nueva**). Decisión D.
   - `MainActivity`: `GradusTheme(darkTheme = elegido ?: sistema)`. `Theme.kt` no cambia (decisión E).
   - ✅ Criterio: en el emulador, el modo oscuro se aplica al momento y se recuerda al reabrir la app.
4. **Piezas de la pantalla** (`app/ui/components`): `SettingsSection` (título en versalitas y
   tarjeta), `SettingsRow` (icono, texto, valor y ›), `SettingsSwitchRow` y `ProfileRow`
   (avatar con iniciales). Iconos nuevos (Material Symbols Rounded, como los del compañero):
   luna, globo, almohadilla, salir, papelera, chevron y más. Previews y test de emulador
   (48 dp, TalkBack: el interruptor se lee como interruptor, con su estado).
5. **`ScreenSettings` sin estado** con las cuatro secciones del Figma, previews en claro y
   oscuro y test de emulador.
6. **`SettingsViewModel` y conexión**: lee la sesión y el modo oscuro; «Cerrar sesión» llama al
   servidor, borra la sesión del móvil y vuelve a Bienvenida sin poder volver atrás.
   - ✅ Criterio, en el emulador: el perfil es el de la cuenta; el modo oscuro cambia toda la app;
     cerrar sesión lleva a Bienvenida y «atrás» no vuelve a Ajustes.

### Decisiones (aprobadas el 2026-11-22)

- **A · Lo que aún no funciona** («Editar», «Cambiar» contraseña, Idioma, Etiquetas, Eliminar
  cuenta). Propuesta: se ve todo como en el Figma, pero eso **desactivado** y con «Próximamente»
  en lugar de su valor («Cambiar», «Español»…), para que nadie lo pulse esperando algo. TalkBack
  lo lee «no disponible». Alternativa: esconderlo hasta que funcione (la pantalla quedaría
  distinta del Figma).
- **B · Modo oscuro.** Mientras no se toque, sigue al del sistema (el interruptor enseña cómo está
  ahora). Al tocarlo, se fija a claro u oscuro y se recuerda. Sin opción «del sistema» en la
  pantalla (el Figma tiene un interruptor).
- **C · Cerrar sesión sin pedir confirmación** (se vuelve a entrar en un momento). Si el servidor
  no responde, la sesión se borra igual del móvil y se sale: el usuario quiere salir, y la sesión
  del servidor caduca sola. Eliminar cuenta (fase 2) sí pedirá confirmación.
- **D · `SharedPreferences`** para guardar el modo oscuro: viene con Android, sin dependencias.
  La alternativa moderna (DataStore) es una dependencia nueva y habría que aprobarla.
- **E · El tema.** `AGENTS.md` pide no tocar el tema sin permiso: solo se cambia **qué** se le pasa
  a `GradusTheme` en `MainActivity`; colores y tipografía (`Theme.kt`, `Color.kt`) no cambian.

## Fase 2 (backend nuestro): perfil, contraseña y eliminar la cuenta

Estado: **hecha (2026-11-23)**. Decisiones F–J aprobadas el 2026-11-23.

### Lo que ya hay en el backend

- `users` (nombre, correo único, hash PBKDF2) y `sessions` con `ON DELETE CASCADE` (al borrar un
  usuario se borran sus sesiones).
- `courses.author_id` **sin** `ON DELETE CASCADE` (a propósito: «borrar a un autor no debe borrar
  cursos que otros estudian»). Hoy, borrar a un autor de cursos daría un error de la base de datos.
- `RequireSession` protege rutas con el token; `UserName` y `RawPassword` ya validan nombre y
  contraseña (las mismas reglas que el registro).

### Rutas nuevas (todas con `Authorization: Bearer <token>`)

| Ruta | Cuerpo | Respuestas |
|---|---|---|
| `PATCH /api/users/me` | `{name}` | `200` usuario · `400` nombre no válido |
| `PUT /api/users/me/password` | `{currentPassword, newPassword}` | `204` · `400` reglas de la nueva · `403` `WRONG_PASSWORD` |
| `DELETE /api/users/me` | `{password}` | `204` · `403` `WRONG_PASSWORD` · `409` `AUTHOR_OF_COURSES` |

Códigos nuevos en `ErrorCode` (solo se añaden): `WRONG_PASSWORD`, `AUTHOR_OF_COURSES`.

### Decisiones (aprobadas el 2026-11-23)

- **F · Qué se edita del perfil: solo el nombre.** Cambiar el correo exigiría comprobar que es
  tuyo (mandar un correo), y eso no existe. El correo se ve, pero no se edita.
- **G · Cambiar la contraseña pide la actual** (si alguien coge el móvil desbloqueado no puede
  cambiarla) y **cierra las sesiones de los demás dispositivos**; la de este móvil sigue abierta.
- **H · Eliminar la cuenta pide la contraseña y una confirmación** («Esta acción no se puede
  deshacer»). Borra el usuario y sus sesiones. Al terminar, la app vuelve a Bienvenida.
- **I · Si la cuenta es autora de cursos publicados, no se puede eliminar** (`409`, y la app
  explica por qué). Así nadie se queda sin un curso que está estudiando. Alternativa: «anonimizar»
  (borrar nombre, correo y contraseña pero dejar al usuario como «Autor eliminado»); es más
  trabajo y cambia la tabla.
- **J · `DELETE` con cuerpo** (`{password}`): Javalin y Ktor lo admiten. Alternativa:
  `POST /api/users/me/deletion`, más «seguro» con proxies raros, pero menos REST.

### Tareas

Backend (`backend/`, JUnit, test primero; ver `backend/docs/tareas/`):

1. **Cambiar el nombre.** `User.withName`, caso de uso `RenameUser`, `UserRepository.update`,
   `PATCH /api/users/me`.
   - ✅ Criterio: tests del caso de uso (nombre válido, demasiado corto/largo, espacios) y de la
     petición; el nombre se guarda normalizado como en el registro.
2. **Cambiar la contraseña.** `ChangePassword` (comprueba la actual, valida la nueva con
   `RawPassword`, guarda el hash nuevo), `SessionRepository.removeAllOfUserExcept(user, sesión)`,
   `PUT /api/users/me/password`.
   - ✅ Criterio: actual incorrecta → `WRONG_PASSWORD` y nada cambia; nueva débil → `400` con sus
     códigos; bien → las otras sesiones se cierran y esta no.
3. **Eliminar la cuenta.** `DeleteAccount` (contraseña, ¿autor de cursos?),
   `UserRepository.removeUnlessAuthor(id)`, `DELETE /api/users/me`. (Al hacerla se cambió el plan:
   `CourseRepository` es del compañero y no se toca; la comprobación va en la misma sentencia DELETE.)
   - ✅ Criterio: contraseña mala → `403` y nada se borra; autor → `409` y nada se borra; bien →
     usuario y sesiones borrados, y el token deja de valer (`401`).
4. **Base de datos (JDBC) y prueba con `curl`** contra el PostgreSQL de Docker. Sin tablas nuevas.

App (test primero en `sharedLogic`):

5. **`AccountGateway`**: `rename`, `changePassword`, `deleteAccount` con resultados sellados
   (`Renamed | Rejected(errores) | Unauthorized | Unreachable | ServerFailure`, etc.). Tests con
   `MockEngine`. (Actualizar la sesión guardada en el móvil con el nombre nuevo va en la tarea 6,
   en el ViewModel: el gateway no toca el `SessionStore`, como en `logOut`.)
6. **Editar perfil**: pantalla con el campo de nombre (reutiliza `FormField` y los mensajes del
   registro), su ViewModel y la fila «Editar» de Ajustes activada.
7. **Cambiar contraseña**: actual, nueva y repetirla, con las comprobaciones en vivo del registro
   (`PasswordChecks`). Fila «Contraseña · Cambiar» activada.
8. **Eliminar cuenta**: diálogo de confirmación con la contraseña; mensaje si es autor;
   al terminar, a Bienvenida sin poder volver atrás. Fila «Eliminar cuenta» activada y en rojo.

Cada pantalla: previews en claro y oscuro, test de emulador y prueba con el backend local.

### Fuera de alcance

Cambiar el correo, recuperar la contraseña olvidada, foto de perfil, «anonimizar» al autor,
deshacer el borrado.

## Fase 3: etiquetas preferidas e idioma

Estado: **en curso**. Decisiones K–R aprobadas el 2026-11-23.

### Lo que hay hoy

- **Textos:** unos 290 textos escritos en el código, en 64 ficheros de `app`. 11 de esos ficheros son
  del compañero (Buscar y filtros). `strings.xml` solo tiene `app_name`. Varias funciones puras de
  textos tienen tests en el ordenador (`CourseTextsTest`, `CourseDetailTextsTest`, y del compañero
  `SearchTextsTest` y `FilterTextsTest`): con `strings.xml` necesitan el `Context` de Android.
- **Idioma por app:** Android 13+ trae `LocaleManager` (sin dependencias). En Android 10–12 (minSdk
  29) solo se puede con AppCompat, que no usa `app` (cambiaría `MainActivity` y el tema).
  Cualquiera de los dos necesita `android:localeConfig` en `AndroidManifest.xml`.
- **Etiquetas:** Buscar (del compañero) ordena por fecha de publicación en el servidor. Inicio es
  todavía provisional. Las etiquetas de los cursos van en minúsculas y sin «#».

### Decisiones (aprobadas el 2026-11-23)

- **K · Primero las etiquetas, después el idioma.** Son independientes; las etiquetas son pequeñas
  y se ven enseguida. El idioma es una migración larga.
- **L · Etiquetas guardadas en el móvil** (`SettingsStore`, como el modo oscuro). Sin backend.
  Alternativa: guardarlas en el servidor para que te sigan a otro móvil (tabla y rutas nuevas).
- **M · Cómo se eligen:** se escriben y se añaden; se guardan como en los cursos (minúsculas, sin
  «#» ni espacios alrededor, sin repetir), como mucho 10. Cada una se quita con su ✕.
- **N · Usarlas para ordenar Buscar lo hace el compañero.** Nosotros le damos `preferredTags` (un
  `Flow`) y una función pura en `sharedLogic` que pone primero los cursos con alguna etiqueta
  preferida sin cambiar el orden del resto. Hasta entonces, la frase de Ajustes «Verás antes los
  cursos…» sería una promesa: propuesta, decir «Pronto verás antes…» hasta que Buscar lo use.
- **O · Idioma: español e inglés**, elegible en Ajustes (Español, English, «El del sistema»).
  - Con `LocaleManager` (Android 13+, sin dependencias). En Android 10–12 la fila dice que sigue
    al idioma del sistema y no se puede tocar. Necesita `res/xml/locales_config.xml` y una línea
    en `AndroidManifest.xml` (**hay que tocar el Manifest**).
  - Alternativa: AppCompat para todas las versiones (dependencia, `AppCompatActivity` y tema).
- **P · Los textos pasan a `strings.xml`** (`values/` en español y `values-en/` en inglés), por zonas.
  Las funciones puras de textos devuelven un `UiText` (el id del texto y sus datos) en vez de un
  `String`, para seguir probándolas en el ordenador sin Android.
- **Q · Los ficheros del compañero** (Buscar y filtros) los pasa él con el mismo patrón, o yo si
  él lo aprueba, para no provocar conflictos.
- **R · El contenido de los cursos no se traduce** (títulos, lecciones, preguntas): lo escribe su
  autor en su idioma.

### Tareas

Etiquetas (3a):

1. **`sharedLogic`** (test primero): `PreferredTags` (normalizar, sin repetir, máximo 10),
   `AppSettings.preferredTags`, `SettingsStore.setPreferredTags`, y `prioritizing(preferidas)` para
   ordenar una lista de cursos.
2. **Guardarlas en el móvil:** `SharedPreferencesSettingsStore` (una por línea en un solo texto:
   un `StringSet` no guarda el orden).
3. **Pantalla «Etiquetas preferidas»** (añadir y quitar) y la sección de Ajustes con sus chips
   (reutilizando `TagLabel`). Fila activa.
4. **Entrega al compañero:** nota en `docs/tareas/` con cómo usar `preferredTags` y `prioritizing`.

Idioma (3b):

5. **Base:** `UiText` (+ cómo pintarlo en Compose), `locales_config.xml`, la línea del Manifest y la
   fila «Idioma» con `LocaleManager`.
6. **Textos de la cuenta:** Bienvenida, Registro, Inicio de sesión y componentes de formulario.
7. **Textos de Ajustes** y sus pantallas (perfil, contraseña, eliminar, etiquetas).
8. **Textos del curso:** camino, práctica, ficha y Mis cursos (con `CourseTexts` y
   `CourseDetailTexts` → `UiText`).
9. **Buscar y filtros:** el compañero (o yo, con su OK).
10. **Comprobación final** en el emulador, en inglés y en español, con TalkBack.

### Fuera de alcance

Más idiomas, traducir los cursos, sincronizar las etiquetas entre dispositivos (si se elige L),
recomendaciones automáticas.

## Progreso

- 2026-11-22: documento creado; fase 1 aprobada para empezar. Decisiones A–E aprobadas.

- 2026-11-22: **tarea 1 hecha**.
  - `account/LogOutOutcome.kt` (nuevo): `LoggedOut | Unreachable | ServerFailure`.
  - `AccountGateway.logOut(session)`; `HttpAccountGateway`: `DELETE /api/sessions/current` con
    `bearerAuth(token)`; 204 y 401 → `LoggedOut`, otro código → `ServerFailure`.
  - `FakeAccountGateway`: `logOutAnswer`, constructor `FakeAccountGateway(LogOutOutcome)` y
    `closedSessions`.
  - Tests: `HttpAccountGatewayLogOutTest` (7) y 3 más en `AccountGatewaysTest`; 555 en total.
  - Comprobado contra el backend local con `curl`: cerrar → 204; cerrar otra vez → 401.

- 2026-11-22: **tarea 2 hecha**.
  - `account/Initials.kt`, `settings/AppSettings.kt` y `settings/SettingsStore.kt` (nuevos).
  - Tests: `InitialsTest` (8: dos palabras, tres, una, tildes y minúsculas, espacios, guion, sin
    nombre y `Account.initials`) e `InMemorySettingsStoreTest` (7). 570 en total.

- 2026-11-22: **tarea 3, código hecho** (falta comprobarla en el emulador).
  - `SettingsStore.settings` pasa a `StateFlow` (siempre tiene valor: la app arranca con el tema
    elegido, sin un fotograma con el del sistema).
  - `data/SharedPreferencesSettingsStore.kt` (nuevo): fichero «settings», clave `dark_mode`; sin
    clave → `null` (sigue al sistema).
  - `AppContainer.init(context)` y `settingsStore`; `MainActivity` aplica
    `settings.isDark(isSystemInDarkTheme())` a `GradusTheme`. `Theme.kt` sin cambios.
  - Test instrumentado `SharedPreferencesSettingsStoreTest` (4), con su propio fichero de prueba.
  - Compila; 570 tests de sharedLogic y los de `app/src/test` pasan.
  - Emulador: 149 tests instrumentados pasan (4 nuevos). Con el fichero escrito por `adb run-as`
    y la app reabierta: sin elegir sigue al sistema (claro y oscuro); `true` → oscuro con el
    sistema en claro; `false` → claro con el sistema en oscuro.
  - **Encontrado y arreglado:** los iconos de las barras del sistema (hora, batería, barra de
    gestos) seguían al sistema y no a lo elegido. `MainActivity` vuelve a llamar a
    `enableEdgeToEdge` con `SystemBarStyle.auto(...) { dark }` dentro de `DisposableEffect(dark)`.
    Comprobado en el emulador en los cuatro casos: iconos claros sobre oscuro y oscuros sobre claro.
  - «Al momento» (sin reabrir) se comprobará con el interruptor (tareas 5 y 6).
  - **Tarea 3 hecha.**

- 2026-11-23: **tarea 4 hecha**.
  - Iconos (Material Symbols Rounded, del repositorio de Google, mismo formato que `ic_tune`):
    `ic_dark_mode`, `ic_language`, `ic_tag`, `ic_logout`, `ic_delete`, `ic_chevron_right`, `ic_add`.
  - `components/`: `SettingsSection` (+ `SettingsDivider`), `SettingsRowLayout` (esqueleto común,
    `internal`), `SettingsRow`, `SettingsSwitchRow`, `ProfileRow`. Previews en claro y oscuro.
  - Decisión A en la práctica: `enabled = false` → atenuada, «Próximamente» (`COMING_SOON`) en vez
    del valor, no se puede pulsar; TalkBack la anuncia «desactivada».
  - Test de emulador `SettingsRowsTest` (16): un solo elemento por fila, ≥ 48 dp, iconos no leídos,
    desactivada, interruptor con rol y estado, toque en cualquier parte de la fila, iniciales no
    leídas, título de sección como encabezado. Pasan los 16.
  - Visto en el emulador (montaje temporal en `MainActivity`, ya deshecho) en claro y oscuro.
  - Pendiente menor: el candado (`ic_lock`, Material Icons relleno) no es del mismo estilo que el
    resto (contorno). Resuelto en la tarea 5 con `ic_lock_outline`.

- 2026-11-23: **tarea 5 hecha**.
  - `res/drawable/ic_lock_outline.xml` (Symbols Rounded «lock»). `ic_lock` no cambia (es el del camino).
  - `screens/settings/SettingsScreenState.kt`: `SettingsScreenState(name, email, darkMode)` y
    `SettingsActions(onDarkModeChange, onLogOut)`; lo demás no tiene acción (decisión A).
  - `screens/settings/SettingsContent.kt`: una función por sección (`AccountSection`,
    `AppearanceSection`, `InterestsSection`, `SessionSection`).
  - `ScreenSettings(state, actions)` sin estado y con desplazamiento; previews en
    `settings/ScreenSettingsPreviews.kt` (claro, oscuro y nombre largo).
  - El `ScreenSettings(modifier)` provisional sigue para la navegación: lo quita la tarea 6.
  - Etiquetas preferidas: sin chips (aún no se guardan), solo la fila desactivada y la explicación.
    `ic_tag` se usa; `ic_add` queda para la fase 3.
  - Test de emulador `ScreenSettingsTest` (11). Pasan los 11.
  - Vista en el emulador (montaje temporal en `MainActivity`, deshecho) en claro y oscuro.

- 2026-11-23: **tarea 6 hecha; fase 1 terminada**.
  - `screens/settings/SettingsState.kt`: `Loading | Ready(account, loggingOut) | LoggedOut`.
  - `screens/settings/SettingsViewModel.kt`: lee la sesión (sin sesión → `LoggedOut`); expone
    `appSettings` del store; `onDarkModeChange` lo guarda; `onLogOut` avisa al servidor, borra la
    sesión aunque el servidor falle (decisión C) e ignora los toques de más.
  - `navigation/SettingsDestination.kt` (nuevo): `SettingsRoute` (ViewModel + pantalla) y, con
    `LoggedOut`, a Bienvenida borrando el grafo Main. En `MainNavGraph.kt` solo cambia la línea de
    Ajustes (y sobra el import de `ScreenSettings`).
  - Se quita el `ScreenSettings` provisional. Queda un `ScreenSettings(modifier)` `internal` con
    datos de ejemplo **solo para la preview de `MainScaffold.kt`**, que es del compañero y no se
    toca. Si el compañero cambia esa preview, se puede borrar.
  - Tests: `SettingsViewModelTest` (12, en el ordenador; vistos en rojo antes de implementar).
  - Emulador, con el backend local y la cuenta de prueba: Ajustes enseña la cuenta real; el
    interruptor cambia toda la app al momento (y las barras del sistema) y se guarda; al reabrir
    sigue oscuro; «Cerrar sesión» lleva a Bienvenida y «atrás» sale de la app.
  - Totales: sharedLogic 570, app (ordenador) 165, app (emulador) 176. Todo pasa.

- 2026-11-23: **fase 2, tarea 1 hecha** (cambiar el nombre, backend).
  - `User.withName`; `UserRepository.update` (nombre y hash; el correo no cambia).
  - `RenameUser` + `RenameResult` (`Renamed | Rejected`), con las reglas de `UserName`.
  - `RenameUserRequest` y `UserController.rename`; ruta `PATCH /api/users/me` (con sesión).
  - Adelantado para que compile: `JdbcUserRepository.update` (`UPDATE users SET name, password_hash`)
    e `InMemoryUserRepository.update`. Se probará contra PostgreSQL en la tarea 4.
  - Tests: `RenameUserTest` (10) y `UserTest.renamingChangesOnlyTheName`, vistos en rojo antes.
    Backend: 540 tests, todos pasan.

- 2026-11-23: **fase 2, tarea 2 hecha** (cambiar la contraseña, backend).
  - `ChangePassword` + `ChangePasswordCommand` (sin contraseñas en `toString`) +
    `ChangePasswordResult` (`Changed | Rejected | WrongPassword`). Orden: validar las dos (la nueva
    con las reglas de `RawPassword`) → comprobar la actual → guardar el hash y cerrar las otras
    sesiones. Los errores se reetiquetan con su campo: `currentPassword` / `newPassword`.
  - `User.withPasswordHash`; `SessionRepository.removeOthersOf(userId, tokenHash)` (en memoria y
    JDBC: `DELETE FROM sessions WHERE user_id = ? AND token_hash <> ?`).
  - `ErrorCode.WRONG_PASSWORD` (y `ErrorCodeTest`, que fija el contrato con la app).
  - `ChangePasswordRequest` y `UserController.changePassword`; ruta `PUT /api/users/me/password`:
    `204`, `400` o `403` con `WRONG_PASSWORD` en el campo `currentPassword`.
  - Tests: `ChangePasswordTest` (11, vistos en rojo), `ChangePasswordRequestTest` (2) y
    `UserTest.changingThePasswordChangesOnlyItsHash`. Backend: 554 tests, todos pasan.

- 2026-11-23: **fase 2, tarea 3 hecha** (eliminar la cuenta, backend).
  - `DeleteAccount` + `DeleteAccountCommand` + `DeleteAccountResult`
    (`Deleted | Rejected | WrongPassword | AuthorOfCourses`). Orden: contraseña presente → es la
    suya → borrar salvo que sea autor. A quien no sabe la contraseña no se le dice si tiene cursos.
  - `UserRepository.removeUnlessAuthor(id)`: en JDBC, una sola sentencia
    `DELETE FROM users WHERE id = ? AND NOT EXISTS (SELECT 1 FROM courses WHERE author_id = users.id)`;
    las sesiones caen por `ON DELETE CASCADE`. `CourseRepository` (del compañero) no se toca.
  - `ErrorCode.AUTHOR_OF_COURSES`; `DeleteAccountRequest` y `UserController.deleteAccount`; ruta
    `DELETE /api/users/me`: `204`, `400`, `403` `WRONG_PASSWORD` (campo `password`) o `409`
    `AUTHOR_OF_COURSES` (campo `account`).
  - Tests: `DeleteAccountTest` (10, vistos en rojo; incluye que el token deja de valer y que el
    correo queda libre) y `DeleteAccountRequestTest` (2). Backend: 566 tests, todos pasan.

- 2026-11-23: **fase 2, tarea 4 hecha** (prueba real del backend; parte de backend terminada).
  - Backend reiniciado con `./gradus.sh up` (sin migraciones pendientes: no hay tablas nuevas).
  - Script de prueba HTTP (Python, en el scratchpad de la sesión; hace lo mismo que `curl`) con una
    cuenta nueva y desechable, contraseña aleatoria que no se imprime: **23 de 23 bien**.
    - Nombre: `200` sin espacios y guardado en PostgreSQL; `400` `TOO_SHORT` / `REQUIRED`; `401` sin token.
    - Contraseña: `403` con la actual mala (y la otra sesión sigue); `400` con una débil; `204` bien →
      la otra sesión da `401`, esta sigue; la vieja ya no entra y la nueva sí.
    - Eliminar: `403` / `400`; la cuenta de prueba de la app (autora de 2 cursos) → `409` y sigue
      existiendo; la desechable → `204`, su token da `401` y ya no puede entrar.
  - Comprobado en la base de datos: no queda ninguna cuenta de prueba ni sesiones huérfanas
    (`ON DELETE CASCADE` funciona); la autora sigue con sus 2 cursos.
  - Incidencia: los tres primeros intentos del script fallaron a mitad (la consola de Windows no
    escribía «─»; `python -I` ignora `PYTHONIOENCODING`; una edición fallida se ejecutó igual) y
    dejaron cada vez una cuenta desechable, que se borró a mano con `DELETE … AND NOT EXISTS (cursos)`.
    El script final borra la cuenta en un `finally` si algo falla.

- 2026-11-23: **fase 2, tarea 5 hecha** (`AccountGateway` en `sharedLogic`).
  - `RenameOutcome` (`Renamed | Rejected | SessionExpired | Unreachable | ServerFailure`),
    `ChangePasswordOutcome` (+ `WrongPassword`), `DeleteAccountOutcome` (+ `WrongPassword`,
    `AuthorOfCourses`). `401` → `SessionExpired`: la sesión ya no vale y hay que volver a entrar.
  - `PasswordChange(currentPassword, newPassword)` (sin contraseñas en `toString`).
  - `AccountGateway.rename / changePassword / deleteAccount`; `HttpAccountGateway`: `PATCH`, `PUT` y
    `DELETE` con cuerpo, todas con `bearerAuth` (función común `withSessionAndJson`).
  - DTOs `RenameRequestDto`, `ChangePasswordRequestDto`, `DeleteAccountRequestDto` (estos dos sin
    contraseñas en `toString`).
  - `ApiErrorCode`: `WRONG_PASSWORD`, `AUTHOR_OF_COURSES` (y el test que fija el contrato).
  - `FakeAccountGateway`: respuestas por defecto (renombra como el servidor, cambia, borra),
    constructores por desenlace y lo recibido (`receivedNames`, `receivedPasswordChanges`, `deletions`).
  - Tests: `HttpAccountGatewayRenameTest` (8), `…ChangePasswordTest` (9), `…DeleteAccountTest` (10),
    vistos en rojo (21 fallaban con el esqueleto); 6 más del falso. sharedLogic: 603, todos pasan.

- 2026-11-23: **fase 2, tarea 6 hecha** («Editar perfil»).
  - `sharedLogic`: `model/ProfileForm` (nombre con las reglas del registro; sin cambios no se envía;
    los espacios alrededor no cuentan) y `account/ProfileApplyingOutcome` (`RenameOutcome` → formulario;
    `SessionExpired` no lo toca). Tests: `ProfileFormTest` (15) y `ProfileApplyingOutcomeTest` (6), en rojo antes.
  - App: `screens/profile/` → `EditProfileState` (`Loading | Editing(email, form) | LoggedOut`),
    `EditProfileActions`, `EditProfileViewModel` (al guardar, guarda en el móvil la sesión con el
    nombre nuevo; `SessionExpired` → borra la sesión y sale), `ScreenEditProfile` (nombre editable,
    correo fijo con «El correo no se puede cambiar», «Guardar» desactivado sin cambios) y sus previews.
  - Navegación: `Route.EditProfile`, `navigation/EditProfileDestination.kt` (al guardar vuelve a
    Ajustes); en `MainNavGraph.kt` solo una línea; `leaveToWelcome` pasa a `internal`.
  - Ajustes: la fila del perfil ya está activa («Editar»); `SettingsViewModel.onResume()` vuelve a
    leer la sesión al volver (`LifecycleResumeEffect`), así enseña el nombre nuevo.
  - Tests: `EditProfileViewModelTest` (13; en rojo antes, y pilló un fallo real: un segundo toque
    mientras se enviaba volvía a enviar), 2 más en `SettingsViewModelTest`, `ScreenEditProfileTest`
    (9, emulador) y `ScreenSettingsTest` actualizado (12). sharedLogic 624; todo pasa.
  - Emulador con el backend y la cuenta de prueba: Ajustes → Editar → «Prueba Editada» → Guardar →
    vuelve a Ajustes con el nombre e iniciales («PE») nuevos y está en PostgreSQL. Después se le
    devolvió el nombre «Prueba Curso» desde la propia app.

- 2026-11-23: **fase 2, tarea 7 hecha** («Cambiar contraseña»).
  - `sharedLogic`: `model/PasswordForm` (actual, nueva y repetición; requisitos y coincidencia en
    vivo; sin contraseñas en `toString`), `PasswordChangeField`, `PasswordProblem`
    (`WrongCurrent | InField`), y `account/PasswordApplyingOutcome` (`toChange()` y la traducción de
    la respuesta; cualquier regla de la nueva → `WEAK_PASSWORD`, como en el registro).
    `FieldError` y `FieldErrorMessages` (compartidos con el compañero) no se tocan: «la actual no es
    correcta» va aparte (`isWrongCurrent`). Tests: `PasswordFormTest` (15) y
    `PasswordApplyingOutcomeTest` (8), en rojo antes.
  - App: `screens/password/` → `ChangePasswordState` (`Editing | LoggedOut`), `ChangePasswordActions`,
    `ChangePasswordViewModel` (la sesión del móvil no cambia; `SessionExpired` → la borra y sale),
    `ScreenChangePassword` (reutiliza `PasswordField`, `PasswordRequirementList` y `PasswordMatchItem`;
    al terminar, «Contraseña cambiada» con el aviso de la decisión G y «Volver a Ajustes») y previews.
  - Navegación: `Route.ChangePassword`, `navigation/ChangePasswordDestination.kt`, una línea en
    `MainNavGraph.kt`. Ajustes: la fila «Contraseña · Cambiar» ya está activa.
  - Tests: `ChangePasswordViewModelTest` (11, en rojo antes), `ScreenChangePasswordTest` (10, emulador;
    uno se arregló porque el test no guardaba el estado como la app) y `ScreenSettingsTest` (13).
  - Emulador con el backend y la cuenta de prueba: actual equivocada → «La contraseña actual no es
    correcta»; bien → «Contraseña cambiada»; el servidor rechaza la vieja (401) y acepta la nueva;
    una sesión abierta con `curl` («otro dispositivo») pasó de 200 a 401 y la del móvil siguió
    valiendo. Después se le devolvió la contraseña de siempre desde la propia app (comprobado: entra).

- 2026-11-23: **fase 2, tarea 8 hecha** («Eliminar cuenta»); **fase 2 terminada**.
  - `sharedLogic`: `model/DeleteAccountForm` + `DeleteProblem` (`WRONG_PASSWORD` se corrige
    escribiendo; `AUTHOR_OF_COURSES` no, y ya no se puede enviar) y `account/DeleteApplyingOutcome`
    (un 400 es un fallo: la app nunca envía la contraseña vacía). Tests: `DeleteAccountFormTest` (9)
    y `DeleteApplyingOutcomeTest` (6), en rojo antes. sharedLogic: 663.
  - App: un **diálogo** encima de Ajustes, no una pantalla. `screens/settings/DeleteAccountState`
    (`Closed | Open(form) | LoggedOut`), `DeleteAccountActions`, `DeleteAccountViewModel` (aparte del
    de Ajustes; borrada la cuenta o caducada la sesión → borra la sesión del móvil y sale; no se
    puede cancelar mientras se envía) y `DeleteAccountDialog` (aviso «No se puede deshacer»,
    contraseña, botón rojo; si es autora, «No se puede eliminar» y solo «Entendido»), con previews.
  - `SettingsRoute` abre el diálogo y, con `LoggedOut`, va a Bienvenida (`leaveToWelcome`). La fila
    «Eliminar cuenta» ya está activa y en rojo.
  - Tests: `DeleteAccountViewModelTest` (11, en rojo antes), `DeleteAccountDialogTest` (9, emulador)
    y `ScreenSettingsTest` (14).
  - Emulador con el backend: la cuenta de prueba (autora de 2 cursos) → «No se puede eliminar» y
    sigue existiendo; una cuenta desechable (creada para la prueba, contraseña aleatoria nunca
    impresa) → contraseña mala: «La contraseña no es correcta»; buena: Bienvenida, «atrás» sale de
    la app y la cuenta ya no está en PostgreSQL. Base de datos sin restos de pruebas.

- 2026-11-23: **fase 3, tarea 1 hecha** (etiquetas preferidas en `sharedLogic`).
  - `settings/PreferredTags.kt`: `PreferredTags` (`add` → `TagAddition`: `Added | Empty |
    AlreadyAdded | Full`; `remove`; factory `of(guardadas)` que vuelve a aplicar las reglas; `MAX = 10`).
    Se normalizan con `tagKey()` (la misma función con la que Buscar compara etiquetas; solo se
    llama, no se cambia) y sin «#» delante.
  - `prioritizing(preferidas)`: los cursos con alguna etiqueta preferida primero; orden estable.
  - `AppSettings.preferredTags` y `SettingsStore.setPreferredTags` (también en
    `InMemorySettingsStore`). `SharedPreferencesSettingsStore` de momento solo las tiene en
    memoria (la tarea 2 las guarda en el fichero).
  - Tests: `PreferredTagsTest` (11), `PrioritizingTest` (5) y 3 más en `InMemorySettingsStoreTest`,
    vistos en rojo (13 fallaban). sharedLogic: 682, todos pasan.

- 2026-11-23: **fase 3, tarea 2 hecha** (etiquetas guardadas en el móvil).
  - `SharedPreferencesSettingsStore`: clave `preferred_tags`, una etiqueta por línea en un solo
    texto (`putStringSet` no guarda el orden). Al leer, `PreferredTags.of` vuelve a aplicar las reglas.
  - Test instrumentado `SharedPreferencesSettingsStoreTest`: 6 nuevos (empieza vacío, se ve al
    momento, sobrevive a cerrar la app con su orden, quitar todas se recuerda, no pisa el modo
    oscuro, lo raro del fichero se limpia). En rojo antes (3 fallaban); ahora pasan los 10.

- 2026-11-23: **fase 3, tarea 3 hecha** (pantalla «Etiquetas preferidas» y sección de Ajustes).
  - `screens/tags/`: `PreferredTagsUiState` (etiquetas, borrador, aviso), `PreferredTagsActions`,
    `PreferredTagsViewModel` (cada cambio se guarda al momento; las reglas son de `PreferredTags`),
    `PreferredTagsTexts` (avisos, «3 de 10», «#grafos», «Quitar la etiqueta grafos»),
    `ScreenPreferredTags` (campo, «Añadir», chips con ✕ reutilizando `RemovableFilterChip` del
    compañero, sin tocarlo) y previews.
  - Navegación: `Route.PreferredTags`, `navigation/PreferredTagsDestination.kt`, una línea en
    `MainNavGraph.kt`.
  - Ajustes: la fila «Etiquetas preferidas · Editar» activa; las elegidas como `TagLabel` («#grafos»)
    o «Aún no has elegido ninguna.»; la frase dice «Pronto verás antes…» (decisión N).
  - Tests: `PreferredTagsViewModelTest` (9, en rojo antes), `PreferredTagsTextsTest` (5; sin rojo:
    sin las funciones no compilaba), `ScreenPreferredTagsTest` (9, emulador; la zona de 48 dp se
    comprueba tocando a 22 dp del centro, como en `FilterChipsTest`) y `ScreenSettingsTest` (17).
  - Emulador: «#Grafos» → «#grafos»; «GRAFOS» → «Ya tienes esa etiqueta»; quitar «kotlin» baja de
    2 a 1; Ajustes enseña «#grafos»; al cerrar y abrir la app siguen ahí. (Un primer intento escribió
    «%23Grafos» porque `adb input text` no traduce `%23`; se repitió con «#».) Al acabar se quitaron.
  - Nota: se guardan en el móvil (decisión L), no por cuenta: otra cuenta en el mismo móvil las vería.

- 2026-11-24: **arreglo tras el merge de `dev/backend-course-search`.** El compañero renombró
  `tagKey()` a `searchKey()` en `course/search/CourseQuery.kt` (ahora también compara el creador;
  la regla es la misma) y `PreferredTags.kt` no compilaba. Se cambian las dos llamadas y el import a
  `searchKey()`; su código no se toca.

## Siguiente paso

Fase 3, tarea 4: nota para el compañero con cómo usar `preferredTags` y `prioritizing` en Buscar.
