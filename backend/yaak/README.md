# Pruebas manuales con Yaak

Espacio de trabajo de [Yaak](https://yaak.app) con las peticiones de prueba del backend.

## Contenido

- `gradus-backend.yaak.json`: espacio de trabajo «Gradus · Backend» en el formato de exportación de Yaak.
  - Variable global `baseUrl` = `http://localhost:7070`.
  - Carpeta «Usuarios · Registro» con un caso por cada respuesta de `POST /api/users`:
    201, 409, 400 (validación), 400 (campos ausentes), 400 (JSON roto) y 400 (campo desconocido).

## Uso

1. Levanta la base de datos y el servidor desde `backend/`:
   ```bash
   docker compose up -d --wait
   set -a; source .env; set +a
   mvn compile exec:java
   ```
2. En Yaak: **Importar** → elige `backend/yaak/gradus-backend.yaak.json`.
3. Abre el espacio «Gradus · Backend» y envía las peticiones. La descripción de cada una dice qué debe responder.

## Versionarlo con el proyecto (opcional)

Yaak puede sincronizar un espacio de trabajo con una carpeta (un YAML por petición), y así los cambios
que hagáis en Yaak quedan en git. Para ello, en los ajustes del espacio de trabajo elige como carpeta de
sincronización `backend/yaak/workspace/`.

Cuando lo activéis, esa carpeta pasa a ser la fuente de verdad y `gradus-backend.yaak.json` sobra.

**Nunca guardéis contraseñas reales ni tokens en estas peticiones:** se suben al repositorio.
