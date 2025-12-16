# PseudoPad Installer Script (Launch4j + Inno Setup)
# Requires: Maven, Java 21, Inno Setup 6

Write-Host "1. Building project and creating executable..."
mvn clean package

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed."
    exit 1
}

Write-Host "2. Creating installer with Inno Setup..."

# Try to find ISCC.exe (Inno Setup Compiler)
$isccPath = "ISCC.exe"
if (-not (Get-Command $isccPath -ErrorAction SilentlyContinue)) {
    $possiblePaths = @(
        "C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
        "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
        "C:\Program Files\Inno Setup 6\ISCC.exe",
        "${env:ProgramFiles}\Inno Setup 6\ISCC.exe"
    )
    foreach ($path in $possiblePaths) {
        if (Test-Path $path) {
            $isccPath = $path
            break
        }
    }
}

if (-not (Get-Command $isccPath -ErrorAction SilentlyContinue) -and -not (Test-Path $isccPath)) {
    Write-Host "Error: Inno Setup Compiler (ISCC.exe) not found."
    Write-Host "Please install Inno Setup 6 from: https://jrsoftware.org/isdl.php"
    Write-Host "The .exe file has been created in 'target/PseudoPad.exe', but the installer could not be built."
    exit 1
}

# Run Inno Setup
& $isccPath "setup.iss"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Success! Installer created in 'installer' directory."
    $installerPath = Join-Path (Get-Location) "installer"
    Write-Host "Installer Location: $installerPath\PseudoPad_Setup.exe"
    Invoke-Item $installerPath
} else {
    Write-Host "Inno Setup failed."
}
