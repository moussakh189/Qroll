# =============================================================================
# QRoll - Windows Installer Builder
# Run this script ONCE on your dev PC to produce installer\QRoll-1.1.exe
# Requirements: JDK 17+, Maven (bundled path below)
# =============================================================================

$ErrorActionPreference = "Stop"

$MVN   = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
$JAVA  = "java"       # must be JDK 17+ on PATH; jpackage is bundled with it
$VERSION = "1.1"
$APP_NAME = "QRoll"

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  QRoll Installer Builder v$VERSION" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

# ── Step 1: Build fat JAR ─────────────────────────────────────────────────────
Write-Host "[1/3] Building fat JAR..." -ForegroundColor Yellow
& $MVN package -DskipTests -q
if ($LASTEXITCODE -ne 0) { Write-Error "Maven build failed!"; exit 1 }
Write-Host "      Done -> target\qroll-$VERSION.jar" -ForegroundColor Green

# ── Step 2: Clean previous installer output ───────────────────────────────────
Write-Host "[2/3] Preparing output folder..." -ForegroundColor Yellow
if (Test-Path "installer") { Remove-Item -Recurse -Force "installer" }
New-Item -ItemType Directory -Path "installer" | Out-Null

# ── Step 3: Run jpackage ──────────────────────────────────────────────────────
Write-Host "[3/3] Running jpackage (this may take ~1 min)..." -ForegroundColor Yellow

jpackage `
  --input          target `
  --main-jar       "qroll-$VERSION.jar" `
  --main-class     com.qroll.gui.App `
  --name           $APP_NAME `
  --app-version    $VERSION `
  --vendor         "QRoll" `
  --description    "QR-based Attendance System for University" `
  --type           exe `
  --win-menu `
  --win-shortcut `
  --win-dir-chooser `
  --win-menu-group "QRoll" `
  --dest           installer `
  --java-options   "--add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" `
  --java-options   "--add-opens javafx.base/com.sun.javafx.runtime=ALL-UNNAMED"

if ($LASTEXITCODE -ne 0) { Write-Error "jpackage failed!"; exit 1 }

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Green
Write-Host "  SUCCESS!  Installer ready:" -ForegroundColor Green
Write-Host "  installer\$APP_NAME-$VERSION.exe" -ForegroundColor White
Write-Host "=====================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Give this .exe to IT. No Java needed on teacher PCs." -ForegroundColor Cyan
Write-Host "Database will be stored at: %APPDATA%\QRoll\qroll.db" -ForegroundColor Cyan
Write-Host ""
