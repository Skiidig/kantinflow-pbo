param(
    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'
$version = '3.54.1.0'
$jarName = "sqlite-jdbc-$version.jar"
$root = $PSScriptRoot
$libDir = Join-Path $root 'lib'
$jarPath = Join-Path $libDir $jarName
$downloadUrl = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/$version/$jarName"

Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue

function Test-SqliteJar {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $false
    }

    try {
        $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)
        try {
            return ($null -ne $archive.GetEntry('org/sqlite/JDBC.class'))
        } finally {
            $archive.Dispose()
        }
    } catch {
        return $false
    }
}

if (Test-SqliteJar $jarPath) {
    Write-Host "[OK] Driver SQLite sudah tersedia dan valid:" -ForegroundColor Green
    Write-Host "     $jarPath"
    Write-Host "     Versi $version"
    exit 0
}

if ($CheckOnly) {
    Write-Host "[GAGAL] Driver SQLite tidak ditemukan atau tidak valid:" -ForegroundColor Red
    Write-Host "        $jarPath"
    Write-Host "Jalankan setup.ps1 terlebih dahulu untuk mengunduhnya."
    exit 1
}

New-Item -ItemType Directory -Force -Path $libDir | Out-Null
$tempPath = Join-Path $libDir (".$jarName.download-" + [Guid]::NewGuid().ToString('N') + '.tmp')

try {
    Write-Host "Mengunduh driver SQLite $version dari Maven Central..."
    Write-Host "  $downloadUrl"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $downloadUrl -OutFile $tempPath -UseBasicParsing

    if (-not (Test-SqliteJar $tempPath)) {
        throw "File unduhan bukan driver SQLite yang valid."
    }

    Move-Item -LiteralPath $tempPath -Destination $jarPath -Force

    if (-not (Test-SqliteJar $jarPath)) {
        throw "Driver hasil unduhan tidak valid setelah dipindahkan."
    }

    Write-Host "[OK] Driver SQLite berhasil disiapkan:" -ForegroundColor Green
    Write-Host "     $jarPath"
    exit 0
} catch {
    Write-Host "[GAGAL] Gagal menyiapkan driver SQLite." -ForegroundColor Red
    Write-Host "        $($_.Exception.Message)"
    Write-Host "        File sementara tidak dipakai sebagai driver."
    Write-Host "        Periksa koneksi internet, lalu jalankan setup.ps1 lagi."
    exit 1
} finally {
    if (Test-Path -LiteralPath $tempPath) {
        Remove-Item -LiteralPath $tempPath -Force -ErrorAction SilentlyContinue
    }
}
