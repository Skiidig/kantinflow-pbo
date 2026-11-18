param(
    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$libDir = Join-Path $root 'lib'

$dependencies = @(
    [pscustomobject]@{
        Nama     = 'SQLite JDBC'
        Versi    = '3.54.1.0'
        Jar      = 'sqlite-jdbc-3.54.1.0.jar'
        Entry    = 'org/sqlite/JDBC.class'
        Url      = 'https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.54.1.0/sqlite-jdbc-3.54.1.0.jar'
    },
    [pscustomobject]@{
        Nama     = 'FlatLaf'
        Versi    = '3.8'
        Jar      = 'flatlaf-3.8.jar'
        Entry    = 'com/formdev/flatlaf/FlatLightLaf.class'
        Url      = 'https://repo1.maven.org/maven2/com/formdev/flatlaf/3.8/flatlaf-3.8.jar'
    },
    [pscustomobject]@{
        Nama     = 'FlatLaf Extras'
        Versi    = '3.8'
        Jar      = 'flatlaf-extras-3.8.jar'
        Entry    = 'com/formdev/flatlaf/extras/FlatSVGIcon.class'
        Url      = 'https://repo1.maven.org/maven2/com/formdev/flatlaf-extras/3.8/flatlaf-extras-3.8.jar'
    },
    [pscustomobject]@{
        Nama     = 'JSVG'
        Versi    = '2.2.0'
        Jar      = 'jsvg-2.2.0.jar'
        Entry    = 'com/github/weisj/jsvg/SVGDocument.class'
        Url      = 'https://repo1.maven.org/maven2/com/github/weisj/jsvg/2.2.0/jsvg-2.2.0.jar'
    },
    [pscustomobject]@{
        Nama     = 'FlatLaf Inter Fonts'
        Versi    = '3.19'
        Jar      = 'flatlaf-fonts-inter-3.19.jar'
        Entry    = 'com/formdev/flatlaf/fonts/inter/FlatInterFont.class'
        Url      = 'https://repo1.maven.org/maven2/com/formdev/flatlaf-fonts-inter/3.19/flatlaf-fonts-inter-3.19.jar'
    },
    [pscustomobject]@{
        Nama     = 'Swing Toast Notifications'
        Versi    = '1.0.4'
        Jar      = 'swing-toast-notifications-1.0.4.jar'
        Entry    = 'raven/toast/Notifications.class'
        Url      = 'https://raw.githubusercontent.com/DJ-Raven/swing-toast-notifications/main/library/swing-toast-notifications-1.0.4.jar'
    },
    [pscustomobject]@{
        Nama     = 'XChart'
        Versi    = '3.8.8'
        Jar      = 'xchart-3.8.8.jar'
        Entry    = 'org/knowm/xchart/XChartPanel.class'
        Url      = 'https://repo1.maven.org/maven2/org/knowm/xchart/xchart/3.8.8/xchart-3.8.8.jar'
    },
    [pscustomobject]@{
        Nama     = 'Apache PDFBox'
        Versi    = '3.0.8'
        Jar      = 'pdfbox-3.0.8.jar'
        Entry    = 'org/apache/pdfbox/pdmodel/PDDocument.class'
        Url      = 'https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox/3.0.8/pdfbox-3.0.8.jar'
    },
    [pscustomobject]@{
        Nama     = 'PDFBox IO'
        Versi    = '3.0.8'
        Jar      = 'pdfbox-io-3.0.8.jar'
        Entry    = 'org/apache/pdfbox/io/RandomAccessRead.class'
        Url      = 'https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox-io/3.0.8/pdfbox-io-3.0.8.jar'
    },
    [pscustomobject]@{
        Nama     = 'FontBox'
        Versi    = '3.0.8'
        Jar      = 'fontbox-3.0.8.jar'
        Entry    = 'org/apache/fontbox/ttf/TrueTypeFont.class'
        Url      = 'https://repo1.maven.org/maven2/org/apache/pdfbox/fontbox/3.0.8/fontbox-3.0.8.jar'
    },
    [pscustomobject]@{
        Nama     = 'Commons Logging'
        Versi    = '1.4.0'
        Jar      = 'commons-logging-1.4.0.jar'
        Entry    = 'org/apache/commons/logging/Log.class'
        Url      = 'https://repo1.maven.org/maven2/commons-logging/commons-logging/1.4.0/commons-logging-1.4.0.jar'
    }
)

Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue

function Test-JarEntry {
    param(
        [string]$Path,
        [string]$Entry
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $false
    }

    try {
        $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)
        try {
            return ($null -ne $archive.GetEntry($Entry))
        } finally {
            $archive.Dispose()
        }
    } catch {
        return $false
    }
}

function Unduh-Dengan-Java {
    param(
        [string]$Uri,
        [string]$Destination
    )

    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($null -eq $java) {
        throw "Java tidak ditemukan untuk jalur unduhan cadangan."
    }

    $sourceDir = Join-Path ([System.IO.Path]::GetTempPath()) ("kantinflow-unduh-" + [Guid]::NewGuid().ToString('N'))
    $sourcePath = Join-Path $sourceDir 'UnduhDriver.java'
    New-Item -ItemType Directory -Force -Path $sourceDir | Out-Null
    try {
        $source = @'
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class UnduhDriver {
    public static void main(String[] args) throws Exception {
        try (InputStream in = URI.create(args[0]).toURL().openStream();
                OutputStream out = Files.newOutputStream(Path.of(args[1]))) {
            in.transferTo(out);
        }
    }
}
'@
        Set-Content -LiteralPath $sourcePath -Value $source -Encoding ASCII
        & $java.Source $sourcePath $Uri $Destination
        if ($LASTEXITCODE -ne 0) {
            throw "Unduhan Java berhenti dengan kode $LASTEXITCODE."
        }
    } finally {
        Remove-Item -LiteralPath $sourceDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

New-Item -ItemType Directory -Force -Path $libDir | Out-Null

$missing = @()
foreach ($dep in $dependencies) {
    $target = Join-Path $libDir $dep.Jar
    if (Test-JarEntry -Path $target -Entry $dep.Entry) {
        Write-Host "[OK] $($dep.Nama) $($dep.Versi) sudah tersedia dan valid:" -ForegroundColor Green
        Write-Host "     $target"
    } else {
        $missing += $dep
    }
}

if ($missing.Count -eq 0) {
    exit 0
}

if ($CheckOnly) {
    Write-Host "[GAGAL] Dependensi berikut belum tersedia atau tidak valid:" -ForegroundColor Red
    foreach ($dep in $missing) {
        Write-Host "        $($dep.Jar) (butuh $($dep.Entry))"
    }
    Write-Host "Jalankan setup.ps1 terlebih dahulu untuk mengunduhnya."
    exit 1
}

foreach ($dep in $missing) {
    $target = Join-Path $libDir $dep.Jar
    $tempPath = Join-Path $libDir (".$($dep.Jar).download-" + [Guid]::NewGuid().ToString('N') + '.tmp')

    try {
        Write-Host "Mengunduh $($dep.Nama) $($dep.Versi) dari Maven Central..."
        Write-Host "  $($dep.Url)"
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        try {
            Invoke-WebRequest -Uri $dep.Url -OutFile $tempPath -UseBasicParsing
        } catch {
            Write-Host "Jalur HTTPS PowerShell gagal; mencoba jalur Java..." -ForegroundColor Yellow
        }

        if (-not (Test-JarEntry -Path $tempPath -Entry $dep.Entry)) {
            if (Test-Path -LiteralPath $tempPath) {
                Remove-Item -LiteralPath $tempPath -Force -ErrorAction SilentlyContinue
            }
            Unduh-Dengan-Java -Uri $dep.Url -Destination $tempPath
        }

        if (-not (Test-JarEntry -Path $tempPath -Entry $dep.Entry)) {
            throw "File unduhan bukan JAR $($dep.Nama) yang valid."
        }

        Move-Item -LiteralPath $tempPath -Destination $target -Force

        if (-not (Test-JarEntry -Path $target -Entry $dep.Entry)) {
            throw "JAR $($dep.Nama) tidak valid setelah dipindahkan."
        }

        Write-Host "[OK] $($dep.Nama) berhasil disiapkan:" -ForegroundColor Green
        Write-Host "     $target"
    } catch {
        Write-Host "[GAGAL] Gagal menyiapkan $($dep.Nama)." -ForegroundColor Red
        Write-Host "        $($_.Exception.Message)"
        Write-Host "        File sementara tidak dipakai sebagai dependensi."
        Write-Host "        Periksa koneksi internet, lalu jalankan setup.ps1 lagi."
        exit 1
    } finally {
        if (Test-Path -LiteralPath $tempPath) {
            Remove-Item -LiteralPath $tempPath -Force -ErrorAction SilentlyContinue
        }
    }
}

exit 0
