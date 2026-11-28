# Barra de navegación (pantallas con sesión)

Primera pieza de las pantallas de dentro de la app (especificación §5, «Con sesión»: *barra
inferior: Inicio, Buscar, Mis cursos, Ajustes*). Las pantallas de cada pestaña se harán en sus
propios documentos; aquí solo la barra, el armazón que la comparte y la navegación entre pestañas.

Reparto: el inicio de sesión real (tarea 5 de `registro-cuentas-reales.md`) y el backend de cursos
los hace el compañero. Esta tarea es solo de interfaz en `app/`.

## Objetivo

Un componente común, `GradusNavigationBar`, que aparezca abajo en todas las pantallas con sesión,
marque la pestaña actual y permita cambiar de pestaña, conservando lo que había en cada una.

## Diseño

Figma «Dominio · Pantallas principales», componente **«Barra de navegación»** (390 × 80), usado en
2 · Inicio (`3:127`), 3 · Buscar, 5 · Mis cursos y 6 · Ajustes.

- Cuatro destinos con icono y texto siempre visibles: **Inicio** (casa), **Buscar** (lupa),
  **Mis cursos** (libro) y **Ajustes** (deslizadores).
- Seleccionado: píldora lavanda detrás del icono; icono y texto en morado y en negrita.
- No seleccionado: icono y texto en gris; fondo blanco.
- Encaja con `NavigationBar` + `NavigationBarItem` de Material 3, así que no hace falta dibujarla
  a mano. Colores por rol del tema (sin `Color(0x…)`): fondo `surface`, píldora
  `primaryContainer`, seleccionado `onPrimaryContainer`, resto `onSurfaceVariant`. El modo oscuro
  sale solo del `EsquemaOscuro` (el Figma no tiene modo oscuro).
- Confirmado con una captura (2026-11-17): los iconos son siempre de contorno, también el de la
  pestaña seleccionada (no cambian a la versión rellena).

## Idea de diseño (código)

```
GradusNavHost
├── Welcome, Register, Login            (sin barra)
└── navigation<Route.Main>              (grafo anidado: todo lo que lleva barra)
    ├── Route.Home        ┐
    ├── Route.Search      │  MainScaffold = Scaffold(bottomBar = GradusNavigationBar)
    ├── Route.MyCourses   │  La barra solo aparece si el destino actual es una pestaña.
    └── Route.Settings    ┘
```

- **`TopLevelDestination`** (enum en `ui/navigation/`): cada pestaña con su ruta, texto e icono.
  La barra se pinta recorriendo `TopLevelDestination.entries`: añadir una pestaña es añadir una
  línea (enfoque declarativo, guiado por datos).
- **`GradusNavigationBar`** (en `ui/components/`): sin estado (*state hoisting*). Recibe
  `selected: TopLevelDestination` y avisa con `onSelect: (TopLevelDestination) -> Unit`. No
  conoce el `NavController`: así tiene previews y no depende de la navegación.
- **Navegación entre pestañas**: solo en `GradusNavHost` (o en un `mainGraph()` aparte, ver
  tarea 4). Al pulsar una pestaña: `popUpTo` del inicio del grafo con `saveState`,
  `launchSingleTop` y `restoreState`. Así cada pestaña recuerda su estado, pulsar la pestaña
  actual no apila nada y «atrás» desde cualquier pestaña vuelve a Inicio y, desde Inicio, sale de
  la app (patrón recomendado por Android para la navegación inferior).
- **Patrones**: componente sin estado + objeto de configuración (`TopLevelDestination`), grafo de
  navegación anidado y el NavHost como único sitio que decide a dónde se va.

## Alcance

- Cuatro iconos de Material Symbols en `res/drawable` (como los que ya hay).
- `TopLevelDestination`, rutas nuevas (`Main`, `Search`, `MyCourses`, `Settings`) y grafo anidado.
- `GradusNavigationBar` con previews en claro y oscuro, con cada pestaña seleccionada.
- Armazón `MainScaffold` que coloca la barra y deja el resto de la pantalla al contenido.
- Pantallas provisionales `ScreenSearch`, `ScreenMyCourses` y `ScreenSettings` (con
  `PlaceholderScreen`); `ScreenHome` sigue siendo la provisional.
- Al registrarse e iniciar sesión se llega a `Route.Main` (Inicio con la barra).

## Fuera de alcance

- El contenido de Inicio, Buscar, Mis cursos y Ajustes (cada una tendrá su documento).
- Sesión real y cerrar sesión (compañero, tareas 5 y 6 de `registro-cuentas-reales.md`).
- Pantallas de detalle sin barra (curso, concepto, pregunta): se añadirán fuera de las pestañas.
- Insignias o contadores en la barra (por ejemplo, repasos pendientes).
- Lógica en `sharedLogic`: no hay reglas de negocio que probar; la selección de pestaña la da la
  propia navegación. Si apareciera alguna regla, irá allí con test primero.

## Tareas

- [x] **1. Iconos.** `ic_home`, `ic_search`, `ic_book` (o el nombre del símbolo que encaje) e
  `ic_tune` en `res/drawable`, de Material Symbols, como vectores XML.
  *Ficheros:* 4 XML nuevos.
  *Acepta si:* se ven en la preview del paso 3 y se parecen a los del Figma.
- [x] **2. Destinos y rutas.** `TopLevelDestination` (ruta, texto, icono) y rutas `Main`,
  `Search`, `MyCourses`, `Settings` en `Routes.kt`; pantallas provisionales de las tres pestañas nuevas.
  *Ficheros:* `ui/navigation/TopLevelDestination.kt`, `Routes.kt`, 3 pantallas en `ui/screens/`.
  *Acepta si:* compila; cada pantalla nueva tiene preview en claro y oscuro.
- [x] **3. Componente `GradusNavigationBar`.** Sin estado, con `modifier`, `selected` y `onSelect`;
  colores del tema; `contentDescription` no hace falta en el icono porque el texto ya lo dice
  (TalkBack lee «Inicio, pestaña, seleccionada, 1 de 4»).
  *Ficheros:* `ui/components/GradusNavigationBar.kt`.
  *Acepta si:* previews en claro y oscuro con cada pestaña seleccionada; se parece al Figma; zonas
  táctiles de 48 dp como mínimo.
- [x] **4. Armazón y navegación.** `MainScaffold` (barra abajo, contenido con su `innerPadding`) y
  grafo anidado `navigation<Route.Main>` con las cuatro pestañas. Para chocar menos con los cambios
  del compañero en `GradusNavHost`, el grafo va en su propio fichero
  (`ui/navigation/MainNavGraph.kt`, función `NavGraphBuilder.mainGraph(...)`). `enterHome()` pasa a
  ir a `Route.Main`.
  *Ficheros:* `MainNavGraph.kt`, `MainScaffold.kt`, `GradusNavHost.kt` (pocas líneas).
  *Acepta si:* ver «Cómo comprobarlo». Cuidado con el doble margen: las pantallas de pestaña no
  deben sumar `safeDrawingPadding` abajo además del `innerPadding` del `Scaffold`.
- [x] **5. Prueba en el emulador y documentos.** Recorrer «Cómo comprobarlo» en claro y en oscuro;
  marcar tareas y anotar el progreso.

## Cómo comprobarlo

1. Registrarse (o «Entrar» en el inicio de sesión provisional) → Inicio con la barra y «Inicio»
   seleccionado.
2. Pulsar cada pestaña → cambia la pantalla y la marca; pulsar otra vez la actual no hace nada.
3. Desde Buscar, «atrás» → Inicio; desde Inicio, «atrás» → sale de la app (no vuelve al registro).
4. Girar el móvil → sigue en la misma pestaña.
5. Con TalkBack: cada pestaña se lee con su nombre y si está seleccionada.
6. Modo claro y oscuro del móvil: la barra usa los colores del tema y se lee bien en los dos.
7. La barra no queda tapada por la barra de gestos del sistema (edge-to-edge).

## Coordinación con el compañero

- Los dos tocaremos `GradusNavHost.kt`: por eso el grafo de pestañas va en `MainNavGraph.kt` y en
  `GradusNavHost` solo cambian un par de líneas. Avisarle antes de fusionar.
- Su inicio de sesión real seguirá llamando a `enterHome()`; desde esta tarea, eso lleva a la
  barra. No necesita cambiar nada más.
- Sus pantallas de cursos (catálogo, detalle) podrán colgar de Buscar o Mis cursos sin tocar la barra.

## Trabajar con agentes

Plantilla de petición:

> Lee `AGENTS.md` y `docs/tareas/barra-navegacion.md`. Haz **solo la tarea N**. Solo puedes tocar
> los ficheros que indica. Al terminar, compila con `./gradlew :app:assembleDebug`, marca la tarea,
> añade una línea en «Progreso» y explícanos el cambio con el formato de `AGENTS.md` §11.

Orden: 1 y 2 son independientes; 3 necesita 1 y 2; 4 necesita 3; 5 al final.

## Progreso

- 2026-11-17: documento creado tras mirar el Figma (componente «Barra de navegación»). Aprobado.
- 2026-11-17: tarea 1 hecha. Material Symbols **Rounded** de contorno (como el resto de iconos de
  la app): `home`, `search`, `book_2` (el más parecido al libro del Figma; se descartaron `book` y
  `menu_book`) y `tune`, en `ic_home`, `ic_search`, `ic_book` e `ic_tune`. El SVG original dibuja en
  y ∈ [-960, 0]; cada XML lo coloca con `<group android:translateY="960">`.
- 2026-11-17: tarea 2 hecha. Rutas `Main`, `Search`, `MyCourses` y `Settings` en `Routes.kt`;
  `TopLevelDestination` (ruta, texto, icono); `ScreenSearch`, `ScreenMyCourses` y
  `ScreenSettings` provisionales con preview en claro y oscuro. `:app:assembleDebug` sin avisos.
  Todavía no se navega a ellas (tarea 4).

- 2026-11-17: tarea 3 hecha. `GradusNavigationBar(selected, onSelect, modifier)` sobre
  `NavigationBar` de Material 3 (80 dp, deja sitio a la barra de gestos). Textos con
  `labelSmall` («Texto/Navegación» del Figma), en negrita el seleccionado. Colores: fondo
  `surface`, píldora `primaryContainer`, icono seleccionado `onPrimaryContainer`, texto
  seleccionado `primary` (con `onPrimaryContainer`, en oscuro casi no se distinguiría de las otras
  pestañas) y el resto `onSurfaceVariant`. Preview con cada pestaña seleccionada, en claro y oscuro.
  Probada en el emulador poniéndola un momento en `ScreenHome` (ya deshecho): se parece al Figma
  en claro, se lee bien en oscuro, al pulsar cambia la selección y cada pestaña mide ≈ 97 × 80 dp.
  Sin avisos al compilar.

- 2026-11-18: tarea 4 hecha. `ui/components/MainScaffold.kt` (`Scaffold` con la barra abajo; da
  a la pantalla un `Modifier` con `padding(innerPadding).consumeWindowInsets(innerPadding)` para que
  su `safeDrawingPadding` no sume otra vez el margen). `ui/navigation/MainNavGraph.kt` con
  `NavGraphBuilder.mainGraph(navController)`: `navigation<Route.Main>` que empieza en `Home` y una
  función `tab<T>()` por pestaña que envuelve su pantalla en `MainScaffold`; `navigateToTab()` con
  `popUpTo<Route.Home> { saveState }`, `launchSingleTop` y `restoreState`. **Decisión de
  implementación:** cada pestaña pinta su propia barra (y no un único `Scaffold` alrededor de todo
  el `NavHost`) para que en `GradusNavHost.kt` solo cambien 5 líneas (`mainGraph(...)` y
  `enterHome()` → `Route.Main`) y choque menos con el trabajo del compañero; para que la barra no
  parpadee, el grafo `Main` va **sin animación** (cambiar de pestaña es instantáneo).
  Probado en el emulador (API 37): tras «Entrar» → Inicio con la barra; cada pestaña cambia pantalla
  y marca; pulsar la actual no hace nada; atrás desde Ajustes o Buscar → Inicio; atrás desde Inicio
  → sale de la app; al girar sigue en Buscar; sin doble margen arriba ni abajo; la barra queda por
  encima de la barra de gestos. Sin avisos al compilar.

- 2026-11-18: tarea 5 hecha. Test instrumentado nuevo
  `app/src/androidTest/.../ui/components/GradusNavigationBarTest.kt` (2 tests, en verde dos veces
  seguidas; el primer intento falló una vez con «No compose hierarchies found», un fallo de arranque
  del emulador que no se repitió): cada pestaña tiene rol `Tab` (TalkBack dice «pestaña»), su nombre
  como texto, solo la actual está seleccionada, mide ≥ 48 × 48 dp y al pulsarla avisa con su
  destino. Se ejecuta con `./gradlew :app:connectedDebugAndroidTest` (necesita el emulador
  abierto). Repaso visual en claro y oscuro con la navegación completa. Aviso: si el emulador se
  abre desde una instantánea de *Quick Boot* antigua, la app instalada puede ser vieja; arrancar
  siempre con Run ▶ (configuración `app`). **Funcionalidad completada.**

## Siguiente paso

Elegir la primera pantalla de pestaña (Inicio, Buscar, Mis cursos o Ajustes) y crear su documento
de tarea. Pendiente de avisar al compañero (ver «Coordinación con el compañero»).
