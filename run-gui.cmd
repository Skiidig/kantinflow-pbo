@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"
set "SQLITE_JAR=%ROOT%lib\sqlite-jdbc-3.54.1.0.jar"
set "FLATLAF_JAR=%ROOT%lib\flatlaf-3.8.jar"
set "OUT=%ROOT%out"

pushd "%ROOT%" >nul 2>&1
if errorlevel 1 (
    echo [GAGAL] Tidak dapat membuka folder proyek:
    echo         %ROOT%
    call :berhenti
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo [GAGAL] Java tidak ditemukan di PATH.
    echo         Pasang JDK 25, lalu pastikan java dan javac ada di PATH.
    echo         Cek dengan: java -version  dan  javac -version
    goto :gagal
)

where javac >nul 2>&1
if errorlevel 1 (
    echo [GAGAL] javac tidak ditemukan di PATH.
    echo         Anda mungkin hanya memasang JRE. Pasang JDK, lalu tambahkan bin JDK ke PATH.
    echo         Cek dengan: javac -version
    goto :gagal
)

where powershell >nul 2>&1
if errorlevel 1 (
    echo [GAGAL] Windows PowerShell tidak ditemukan.
    echo         PowerShell diperlukan untuk memeriksa dependensi Java.
    goto :gagal
)

if not exist "%ROOT%setup.ps1" (
    echo [GAGAL] setup.ps1 tidak ditemukan di folder proyek.
    goto :gagal
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT%setup.ps1" -CheckOnly >nul 2>&1
if errorlevel 1 (
    echo [GAGAL] Dependensi Java belum siap.
    echo         File yang diharapkan:
    echo         %SQLITE_JAR%
    echo         %FLATLAF_JAR%
    echo.
    echo Jalankan setup.ps1 terlebih dahulu, misalnya:
    echo   powershell -ExecutionPolicy Bypass -File "%~dp0setup.ps1"
    goto :gagal
)

echo [OK] Java, javac, SQLite JDBC, dan FlatLaf tersedia.
if defined KANTINFLOW_DB (
    echo      Database uji: %KANTINFLOW_DB%
) else (
    echo      Database: data\kantinflow.db
)

if exist "%OUT%" (
    rd /s /q "%OUT%" >nul 2>&1
)
mkdir "%OUT%" >nul 2>&1
if errorlevel 1 (
    echo [GAGAL] Tidak dapat membuat folder hasil kompilasi:
    echo         %OUT%
    goto :gagal
)

set "SOURCES="
for /r "%ROOT%src\main\java" %%F in (*.java) do (
    if /I not "%%~dpF"=="%ROOT%src\main\java\kantinflow\fx\" (
        set "SOURCES=!SOURCES! "%%F""
    )
)
if not defined SOURCES (
    echo [GAGAL] Tidak ada file .java di:
    echo         %ROOT%src\main\java
    goto :gagal
)

echo [INFO] Mengompilasi sumber Swing...
javac -encoding UTF-8 -cp "%ROOT%lib\*" -d "%OUT%" !SOURCES!
if errorlevel 1 (
    echo.
    echo [GAGAL] Kompilasi Java gagal. Baca pesan javac di atas.
    goto :gagal
)

echo [OK] Kompilasi berhasil. Membuka GUI KantinFlow...
java -cp "%OUT%;%ROOT%lib\*" kantinflow.Main gui
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo [GAGAL] GUI berhenti dengan kode %EXIT_CODE%.
    echo         Baca pesan kesalahan di atas.
    goto :gagal_kode
)

popd
exit /b 0

:gagal
call :berhenti
popd
exit /b 1

:gagal_kode
call :berhenti
popd
exit /b %EXIT_CODE%

:berhenti
if not defined KANTINFLOW_NO_PAUSE (
    echo.
    pause
)
exit /b 0
