@echo off
setlocal EnableExtensions

set "ROOT=%~dp0"
set "MVN=mvn"

if defined KANTINFLOW_MAVEN (
    set "MVN=%KANTINFLOW_MAVEN%"
)

pushd "%ROOT%" >nul 2>&1
if errorlevel 1 (
    echo [GAGAL] Tidak dapat membuka folder proyek:
    echo         %ROOT%
    goto :gagal
)

where java >nul 2>&1
if errorlevel 1 (
    echo [GAGAL] Java tidak ditemukan di PATH.
    echo         Pasang JDK 25 terlebih dahulu.
    goto :gagal
)

if not exist "%ROOT%pom.xml" (
    echo [GAGAL] pom.xml tidak ditemukan.
    goto :gagal
)

if defined KANTINFLOW_MAVEN (
    if not exist "%MVN%" (
        echo [GAGAL] KANTINFLOW_MAVEN tidak menunjuk ke mvn.cmd yang ada:
        echo         %MVN%
        goto :gagal
    )
) else (
    where mvn >nul 2>&1
    if errorlevel 1 (
        echo [GAGAL] Maven tidak ditemukan.
        echo         Pasang Maven 3.9+ dan pastikan perintah mvn ada di PATH.
        echo         Atau set KANTINFLOW_MAVEN ke path mvn.cmd.
        goto :gagal
    )
)

echo [INFO] Menjalankan KantinFlow JavaFX melalui Maven...
if defined KANTINFLOW_MAVEN (
    call "%MVN%" -DskipTests javafx:run
) else (
    call mvn -DskipTests javafx:run
)
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
    echo.
    echo [GAGAL] KantinFlow JavaFX berhenti dengan kode %EXIT_CODE%.
    echo         Baca pesan Maven/JavaFX di atas.
    goto :gagal_kode
)

popd
exit /b 0

:gagal
if not defined KANTINFLOW_NO_PAUSE pause
popd
exit /b 1

:gagal_kode
if not defined KANTINFLOW_NO_PAUSE pause
popd
exit /b %EXIT_CODE%
