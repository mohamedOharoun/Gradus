Para sincronizar en tu Yaak local:
New Workspace > Open Folder > backend/yaak/workspace

## Ficheros de prueba (carpeta «Entregas»)

Las entregas suben fotos de `backend/yaak/fixtures/` (`foto-entrega.jpg` y `no-es-jpeg.png`). Yaak usa
la ruta del fichero tal cual, sin resolverla respecto al workspace, así que una vez por ordenador:

1. Crea un entorno **privado** (no compartido; así no se sube al repositorio).
2. Añade la variable `fixturesDir` con la ruta absoluta de esa carpeta, con `/` también en Windows
   (por ejemplo `C:/Users/<tu usuario>/…/Gradus_PAMN/backend/yaak/fixtures`).
3. Selecciona ese entorno antes de enviar las peticiones.
