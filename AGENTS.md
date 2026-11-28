# AGENTS.md — Gradus

Instrucciones para el agente de IA (Claude Code). Se leen al principio de cada petición.
Si una petición contradice este fichero, pregunta antes de actuar.

## 1. El proyecto

Gradus es una app Android nativa para aprender por dominio (*mastery learning*). Cada curso es un
grafo de conceptos: un concepto se desbloquea cuando se dominan sus prerrequisitos. Incluye
repetición espaciada, notas con la cámara y, más adelante, un catálogo de cursos en un servidor.

Es el proyecto de la asignatura PAMN (ULPGC). Lo hacemos dos estudiantes que **estamos aprendiendo
Kotlin y Android**. Requisitos de la asignatura: Kotlin nativo, al menos una API del dispositivo
(cámara), base de datos local SQLite y algún tipo de backend.

## 2. Cómo trabajamos

Usamos programación en pareja con IA (AIXP) y documentos de tarea ligeros (ODD):

- **Nosotros dirigimos y decidimos; tú implementas.** Nada se aplica sin que lo entendamos.
- Ciclo de cada petición: **entender → proponer → esperar nuestro OK → implementar → comprobar → explicar**.
- **Pasos pequeños.** Una cosa cada vez. Si un cambio toca más de dos ficheros o más de unas
  100 líneas, propón primero un plan dividido en pasos y espera a que elijamos.
- **Si algo es ambiguo, haz una sola pregunta concreta** en vez de suponer.
- **Tareas pequeñas** (un componente, un arreglo): sin documentos, directamente.
- **Funcionalidades grandes** (varias sesiones, varias pantallas): crea o actualiza
  `docs/tareas/<funcionalidad>.md` con: objetivo, alcance, fuera de alcance, lista de tareas con
  criterios de aceptación, progreso y siguiente paso. Léelo antes de continuar una funcionalidad.

## 3. Modo aprendizaje

- Explica en español sencillo **qué** haces y **por qué**.
- La primera vez que aparezca un concepto nuevo de Kotlin o Compose (por ejemplo `remember`,
  `Modifier`, `data class`, corrutinas), explícalo en dos o tres frases con un ejemplo.
- Muestra solo el código que cambia, salvo que pidamos el fichero completo.
- Si decimos «lo hago yo» o «dame una pista», da pistas y no la solución.
- Menciona una alternativa solo cuando cambie de verdad la decisión.

## 4. Stack

- **App nativa de Android hecha con Kotlin Multiplatform (KMP):** la app es nativa, pero la lógica
  vive en un módulo compartido para poder llevarla a iOS si más adelante hacemos esa versión.
- Kotlin 2.4, Android Gradle Plugin 9.5, Java 11 como destino del bytecode. `minSdk` 29
  (Android 10), `targetSdk` 37. El `minSdk` sale de `gradle/libs.versions.toml` y es el mismo en
  todos los módulos: si no coincide, `app` no puede usar `sharedLogic`.
- Interfaz de `app`: Jetpack Compose + Material 3 (Compose BOM 2026.02.01) y Navigation Compose.
  **Nada de vistas XML.** `sharedUI` usa Compose Multiplatform (todavía sin usar).
- Previstas, cuando toque: Room (SQLite), WorkManager y CameraX.
- Servidor: `backend/`, proyecto independiente en Java 25 con Maven, Javalin y PostgreSQL en Docker.
- **No añadas dependencias sin preguntar.** Si se aprueban, van en `gradle/libs.versions.toml`
  (en el backend, en `backend/pom.xml`). En `sharedLogic` solo valen librerías multiplataforma.
- Si no estás seguro de que una API o versión exista, dilo en vez de inventarla.

## 5. Estructura

Paquete raíz: `es.ulpgc.pamn.dominio` (no se renombra aunque la app se llame Gradus). El código de
ejemplo de la plantilla KMP usa `es.ulpgc.pamn.gradus`.

```
app/                    App Android que usamos (depende de sharedLogic)
└── src/main/java/es/ulpgc/pamn/dominio/
    ├── MainActivity.kt
    ├── data/           Datos de ejemplo; más adelante, base de datos y red
    └── ui/
        ├── theme/      Color.kt, Type.kt, Theme.kt
        ├── components/ Piezas reutilizables
        ├── navigation/ Rutas y GradusNavHost
        └── screens/    Una pantalla por fichero (y su ViewModel)
sharedLogic/            KMP: lógica compartible con iOS
├── src/commonMain/kotlin/es/ulpgc/pamn/dominio/model/
│                       Datos y reglas en Kotlin puro (validaciones, formularios, modelo)
├── src/commonTest/     Tests de la lógica (kotlin.test)
└── src/androidMain/, src/iosMain/   Solo lo que dependa de la plataforma
sharedUI/, androidApp/, iosApp/
                        Plantilla KMP: UI compartida y apps de arranque (todavía sin usar)
backend/                Servidor (Java, Maven); ver backend/docs/tareas/
docs/                   Especificación y documentos de tareas
```

## 6. Convenciones de código

- **Nombres en inglés** (clases, funciones, variables), salvo términos técnicos estándar.
- Clases pequeñas: ningún archivo debería ser largo, la separación de responsabilidades es prioritaria, así como la reutilización del código.
- Patrones de diseño: es importante el uso de patrones de diseño, e informar de su uso. Asimismo, patrones de diseño como el builder, factory methods, así como tener un enfoque de programación funcional.
- Pantallas: `ScreenX` en `ui/screens/`. Componentes reutilizables en `ui/components/`.
- Todo composable acepta `modifier: Modifier = Modifier` y recibe datos y eventos por parámetro
  (*state hoisting*): las pantallas no deciden la navegación, avisan con lambdas como `onVolver`.
- Cada pantalla y componente tiene `@Preview` en claro y en oscuro.
- **Colores y textos de estilo siempre desde el tema:** `MaterialTheme.colorScheme` y
  `MaterialTheme.typography`. Nunca `Color(0x...)` dentro de una pantalla. Un color nuevo va en
  `Color.kt` con el nombre de su variable en Figma.
- **La lógica no va en la interfaz.** Validaciones, motor de dominio, grafo y repasos van en
  `sharedLogic/src/commonMain/` en Kotlin puro, para poder probarlos y reutilizarlos en iOS.
  En `commonMain` no se puede importar nada de Android ni de Java (`android.*`, `java.*`): solo
  la librería estándar de Kotlin y librerías multiplataforma.
- Accesibilidad: `contentDescription` en iconos con significado y zonas táctiles de 48 dp como mínimo.
- Textos visibles: de momento en el código. Cuando añadamos idiomas, pasarán a `strings.xml`.

## 7. Tests

- **Lógica en Kotlin puro: test primero.** Escribe el test que falla, después el código mínimo
  que lo hace pasar y luego limpia (rojo → verde → refactor). Van en
  `sharedLogic/src/commonTest/` y usan `kotlin.test` (no JUnit). Se ejecutan con
  `./gradlew :sharedLogic:testAndroidHostTest`.
- **Backend:** tests en `backend/src/test/` con JUnit Jupiter; se ejecutan con `mvn test`.
- **Interfaz:** preview y prueba en el emulador. Indica siempre qué pasos seguir para comprobarlo.
- **Los tests también documentan.** La lista de tests del criterio de una tarea es la mínima, no
  la única: cada comportamiento que aparezca en el código, en un comentario o en el progreso
  (desempates, casos límite, respuestas que llegan tarde…) tiene su propio test, aunque ya funcione.

## 8. Fuentes de verdad

- **Diseño:** Figma «Dominio · Pantallas principales»
  (https://www.figma.com/design/knk7ucKW8xHfpa4LcqacUO). Cuando trabajemos una pantalla,
  adjuntaremos una captura. Si el código y el diseño no coinciden, pregunta.
- **Especificación:** `docs/especificacion.md`. Si una petición la contradice, avísanos.

## 9. Lo que no debes hacer

- Hacer commits, push o cambiar de rama: eso lo hacemos nosotros.
- Borrar, mover o renombrar ficheros o paquetes sin permiso.
- Modificar ficheros de Gradle, `AndroidManifest.xml` o el tema sin que lo pidamos.
- Tocar `local.properties`, `.idea/` o carpetas `build/`.
- Escribir claves, contraseñas o tokens en el código.
- Cambiar código que no está relacionado con la petición, aunque se pueda mejorar: menciónalo y ya.

## 10. Cuándo algo está terminado

- Compila sin errores ni avisos nuevos.
- Las previews se ven bien en claro y en oscuro.
- Los tests pasan.
- Si es interfaz, lo hemos probado en el emulador.
- Está explicado y, si tiene documento de tarea, la tarea está marcada como hecha.

## 11. Formato de tus respuestas

1. **Qué voy a hacer**, en una a tres frases.
2. **El cambio**, solo el código que cambia, indicando el fichero.
3. **Por qué**, con los conceptos nuevos explicados.
4. **Cómo comprobarlo**, con pasos concretos.
5. **Siguiente paso sugerido**, sin empezarlo.