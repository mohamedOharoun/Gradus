# Arranca el backend de Gradus con un solo comando: base de datos, migraciones y servidor.
# Versión para PowerShell de gradus.sh (mismas órdenes y mismo comportamiento).
# Uso: .\gradus.ps1 [up|db|test|reset|help]   (sin argumentos = up)
# Funciona en PowerShell 7 (pwsh) y en Windows PowerShell 5.1.
param(
    [Parameter(Position = 0)]
    [string]$Command = 'up'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$BackendDir = $PSScriptRoot
$EnvFile = Join-Path $BackendDir '.env'
$MigrationsDir = Join-Path $BackendDir 'db/init'
$Container = 'gradus-postgres'
$RequiredJava = 25
# Nombre de las migraciones: 001_lo_que_hace.sql (tres cifras para que se ordenen bien).
$MigrationName = '^[0-9]{3}_[a-z0-9_]+\.sql$'

# Lo que se envía por tubería a docker (el SQL tiene tildes) va en UTF-8 sin BOM.
$OutputEncoding = New-Object System.Text.UTF8Encoding $false

Set-Location $BackendDir

function Write-Info([string]$Message) {
    Write-Host '==> ' -ForegroundColor Blue -NoNewline
    Write-Host $Message
}

function Stop-WithError([string]$Message) {
    Write-Host 'Error: ' -ForegroundColor Red -NoNewline
    Write-Host $Message
    exit 1
}

function Show-Usage {
    Write-Host @'
Uso: .\gradus.ps1 [orden]

  up      (por defecto) Base de datos + migraciones pendientes + servidor en primer plano.
  db      Solo base de datos + migraciones (para arrancar el servidor desde el IDE).
  test    Tests unitarios (mvn test). No necesita la base de datos.
  reset   Borra todos los datos locales y crea la base de datos de cero. Pide confirmación.
  help    Muestra esta ayuda.

La configuración sale de backend/.env (ver .env.example). Para probar con Yaak, deja el
servidor arrancado y envía las carpetas del workspace «Gradus · Backend».

Si Windows no deja ejecutar el script:
  powershell -ExecutionPolicy Bypass -File .\gradus.ps1 [orden]
'@
}

function Assert-Command([string]$Name, [string]$Hint) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        Stop-WithError "No se encuentra '$Name'. $Hint"
    }
}

# Los programas externos (docker, mvn) no lanzan excepciones al fallar: se mira su código de salida.
function Assert-LastExitCode([string]$Message) {
    if ($LASTEXITCODE -ne 0) { Stop-WithError $Message }
}

# Ejecuta un programa externo y devuelve su salida, también la de errores (stderr), como texto.
# En Windows PowerShell 5.1, con 'Stop', cada línea de stderr redirigida se convierte en un error
# que para el script, aunque sea solo un aviso (p. ej. el de Java 25 al llamar a mvn). Por eso aquí
# se baja a 'Continue'; si el programa falla de verdad, lo dice $LASTEXITCODE.
function Get-NativeOutput([scriptblock]$Command) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & $Command 2>&1 | ForEach-Object { "$_" } }
    finally { $ErrorActionPreference = $previous }
}

# Carga las variables de .env en el entorno del proceso sin mostrarlas:
# las usan docker compose y el servidor. Formato NOMBRE=valor; ignora vacías y comentarios.
function Import-EnvFile {
    if (-not (Test-Path $EnvFile)) {
        Stop-WithError "Falta backend/.env. Créalo con 'Copy-Item .env.example .env' y pon tu usuario y contraseña."
    }
    foreach ($line in Get-Content $EnvFile -Encoding UTF8) {
        $trimmed = $line.Trim()
        if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }
        $name, $value = $trimmed -split '=', 2
        if ($null -eq $value) { continue }
        # Quita comillas que rodeen el valor, como haría bash al hacer «source».
        $value = $value.Trim() -replace '^"(.*)"$', '$1' -replace "^'(.*)'$", '$1'
        Set-Item -Path "Env:$($name.Trim())" -Value $value
    }
}

# Maven compila con su propio Java (JAVA_HOME), que puede no ser el del PATH: se comprueba ese.
function Assert-MavenAndJava {
    Assert-Command 'mvn' 'Instala Maven.'
    $versionLine = Get-NativeOutput { mvn -v } | Where-Object { $_ -match '^Java version' } | Select-Object -First 1
    $version = if ($versionLine -match '^Java version:\s*([0-9.]+)') { $Matches[1] } else { $null }
    $major = if ($version) { [int]($version -split '\.')[0] } else { 0 }
    if ($major -lt $RequiredJava) {
        $shown = if ($version) { $version } else { 'desconocido' }
        Stop-WithError "Maven usa Java $shown; hace falta Java $RequiredJava o superior (revisa JAVA_HOME)."
    }
}

function Start-Database {
    Assert-Command 'docker' 'Instala Docker y arranca su servicio.'
    $null = Get-NativeOutput { docker compose version }
    Assert-LastExitCode "Falta el plugin 'docker compose' (Compose v2)."
    Write-Info "Arrancando PostgreSQL ($Container)…"
    # --wait espera a que el healthcheck diga que acepta conexiones.
    docker compose up -d --wait postgres
    Assert-LastExitCode 'No se ha podido arrancar PostgreSQL. ¿Está Docker Desktop abierto?'
}

# psql dentro del contenedor. Usuario y base de datos salen de .env (los mismos que usa el
# contenedor); la contraseña no hace falta dentro del contenedor y nunca se imprime.
# client_min_messages=warning oculta avisos como «already exists, skipping»; los errores sí salen.
$PsqlOptions = @('-X', '-q', '-v', 'ON_ERROR_STOP=1')

# Consulta de una línea (-c): no lee la entrada estándar, así que va sin -i.
function Invoke-PsqlQuery([string]$Query) {
    docker exec -e 'PGOPTIONS=-c client_min_messages=warning' $Container `
        psql @PsqlOptions -U $env:GRADUS_DB_USER -d $env:GRADUS_DB_NAME -At -c $Query
}

# Script SQL enviado por la entrada estándar (-f -). Ojo: en PowerShell, lo que llega por tubería
# a una función NO pasa solo a los programas de dentro; hay que enviárselo a docker explícitamente.
function Invoke-PsqlScript([string]$Sql) {
    $Sql | docker exec -i -e 'PGOPTIONS=-c client_min_messages=warning' $Container `
        psql @PsqlOptions -U $env:GRADUS_DB_USER -d $env:GRADUS_DB_NAME --single-transaction -f -
}

# Aplica, en orden, las migraciones que no figuran en schema_migrations. Cada una va en una
# transacción junto con su anotación: si falla, no queda ni a medias ni anotada.
function Invoke-Migrations {
    Write-Info 'Buscando migraciones pendientes…'
    Invoke-PsqlScript 'CREATE TABLE IF NOT EXISTS schema_migrations (filename TEXT PRIMARY KEY, applied_at TIMESTAMPTZ NOT NULL DEFAULT now());'
    Assert-LastExitCode 'No se ha podido crear la tabla schema_migrations.'

    $applied = @(Invoke-PsqlQuery 'SELECT filename FROM schema_migrations')
    Assert-LastExitCode 'No se han podido leer las migraciones aplicadas.'

    $pending = 0
    foreach ($file in Get-ChildItem -Path $MigrationsDir -Filter '*.sql' | Sort-Object Name) {
        $name = $file.Name
        if ($name -cnotmatch $MigrationName) {
            Stop-WithError "Nombre de migración no válido: $name (formato: 001_lo_que_hace.sql)."
        }
        if ($applied -contains $name) { continue }

        Write-Info "  Aplicando $name"
        $sql = (Get-Content -Raw -Encoding UTF8 $file.FullName) +
            "`nINSERT INTO schema_migrations (filename) VALUES ('$name');`n"
        Invoke-PsqlScript $sql
        Assert-LastExitCode "Ha fallado la migración $name (no se ha aplicado nada de ella)."
        $pending++
    }

    if ($pending -eq 0) {
        Write-Info 'No hay migraciones pendientes.'
    } else {
        Write-Info "$pending migración(es) aplicada(s)."
    }
}

function Start-Server {
    Assert-MavenAndJava
    $port = if ($env:GRADUS_HTTP_PORT) { $env:GRADUS_HTTP_PORT } else { '7070' }
    Write-Info "Servidor en http://localhost:$port (Ctrl+C para pararlo)."
    mvn -q compile exec:java
    exit $LASTEXITCODE
}

function Reset-Database {
    Write-Host 'Esto borra TODOS los datos de la base de datos local (volumen gradus-pgdata).'
    $answer = Read-Host "Escribe 'borrar' para continuar"
    if ($answer -ne 'borrar') { Stop-WithError 'Cancelado: no se ha borrado nada.' }
    docker compose down -v
    Assert-LastExitCode 'No se ha podido borrar la base de datos.'
    Start-Database
    Invoke-Migrations
}

switch ($Command) {
    'up'    { Import-EnvFile; Start-Database; Invoke-Migrations; Start-Server }
    'db'    { Import-EnvFile; Start-Database; Invoke-Migrations }
    'test'  { Assert-MavenAndJava; mvn test; exit $LASTEXITCODE }
    'reset' { Import-EnvFile; Reset-Database }
    { $_ -in 'help', '-h', '--help' } { Show-Usage }
    default { Show-Usage; exit 1 }
}
