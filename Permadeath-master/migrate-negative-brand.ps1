$ErrorActionPreference = "Stop"

$root = $PSScriptRoot

Write-Host ""
Write-Host "=== NegativeStudios / LAYON migration ===" -ForegroundColor Cyan
Write-Host "Root: $root"
Write-Host ""

if (-not (Test-Path (Join-Path $root "pom.xml"))) {
    throw "Este script debe estar dentro de Permadeath-master."
}

$newDiscord = "https://discord.gg/nKxBA7qzAU"

# ------------------------------------------------------------
# 1. Archivos de texto del proyecto
# ------------------------------------------------------------

$files = Get-ChildItem $root -Recurse -File |
    Where-Object {
        $_.FullName -notmatch "\\target\\" -and
        $_.FullName -notmatch "\\\.git\\" -and
        $_.Name -notmatch "^(LICENSE|LICENSE\.txt|COPYING|COPYING\.txt)$" -and
        $_.Extension -in @(
            ".java",
            ".xml",
            ".yml",
            ".yaml",
            ".properties",
            ".md"
        )
    }

foreach ($file in $files) {

    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $original = $content

    # Namespace Java/Maven
    $content = $content.Replace(
        "tech.sebazcrc.permadeath",
        "tech.layon.permadeath"
    )

    # Discords antiguos
    $content = $content.Replace(
        "https://discord.gg/BcgUCvGA9s",
        $newDiscord
    )

    $content = $content.Replace(
        "https://discord.gg/8evPbuxPke",
        $newDiscord
    )

    $content = $content.Replace(
        "https://discord.gg/w58wzrcJU8",
        $newDiscord
    )

    $content = $content.Replace(
        "https://discord.gg/infernalcore",
        $newDiscord
    )

    # Evitar que /pdc discord termine mostrando el mismo link 2 veces
    $content = $content.Replace(
        "$newDiscord | $newDiscord",
        $newDiscord
    )

    # Branding del prefijo principal
    $content = $content.Replace(
        "&cPermadeath &7➤ &f",
        "&cNegativeStudios &7➤ &f"
    )

    # Nombre personal fuera de créditos.
    #
    # Esto transforma configuraciones como:
    # AntiAFK.Bypass
    # CustomDeathMessages
    #
    # Los créditos oficiales se restauran después.
    $content = $content.Replace(
        "SebazCRC",
        "layon"
    )

    if ($content -ne $original) {

        Set-Content `
            -Path $file.FullName `
            -Value $content `
            -Encoding UTF8

        Write-Host "Actualizado: $($file.FullName)" -ForegroundColor DarkGray
    }
}

# ------------------------------------------------------------
# 2. Renombrar físicamente los paquetes Java
# ------------------------------------------------------------

$modules = @(
    "main",
    "api",
    "v26_3",
    "v1_15_R1",
    "v1_16_R3",
    "v1_20_R1"
)

foreach ($module in $modules) {

    $techPath = Join-Path $root "$module\src\main\java\tech"
    $oldPath = Join-Path $techPath "sebazcrc"
    $newPath = Join-Path $techPath "layon"

    if (Test-Path $oldPath) {

        if (Test-Path $newPath) {
            throw "Ya existe $newPath. No se moverá $oldPath automáticamente."
        }

        Move-Item `
            -Path $oldPath `
            -Destination $newPath

        Write-Host "Paquete movido: $module -> tech\layon" -ForegroundColor Green
    }
}

# ------------------------------------------------------------
# 3. plugin.yml
#    SebazCRC permanece SOLO como crédito de la base original.
# ------------------------------------------------------------

$pluginYml = Join-Path $root "main\src\main\resources\plugin.yml"

$pluginContent = @'
name: Permadeath
version: 1.3
main: tech.layon.permadeath.Main

load: STARTUP
api-version: '26.3'

authors:
  - LAYON
  - SebazCRC
  - vo1d_dev

description: Versión adaptada y actualizada por LAYON para NegativeStudios, desarrollada sobre la base original de PermadeathCore de SebazCRC.

website: https://github.com/MrLion303/PermadeathCoreRebirth

softdepend:
  - WorldEdit

commands:
  pdc:
    description: Comando principal de Permadeath
    aliases:
      - permadeath
'@

Set-Content `
    -Path $pluginYml `
    -Value $pluginContent `
    -Encoding UTF8

Write-Host "plugin.yml actualizado." -ForegroundColor Green

# ------------------------------------------------------------
# 4. Créditos del comando /pdc info
# ------------------------------------------------------------

$pdcPath = Join-Path $root "main\src\main\java\tech\layon\permadeath\PDCCommand.java"

if (Test-Path $pdcPath) {

    $content = Get-Content $pdcPath -Raw -Encoding UTF8

    $content = $content.Replace(
        'player.sendMessage(ChatColor.GRAY + "- Autor: " + ChatColor.GREEN + "Equipo de InfernalCore (Desarrollador principal: layon)");',
        'player.sendMessage(ChatColor.GRAY + "- Desarrollo actual: " + ChatColor.GREEN + "LAYON / NegativeStudios");' + "`r`n" +
        '                    player.sendMessage(ChatColor.GRAY + "- Base original: " + ChatColor.GREEN + "SebazCRC");'
    )

    Set-Content `
        -Path $pdcPath `
        -Value $content `
        -Encoding UTF8

    Write-Host "Créditos de /pdc info actualizados." -ForegroundColor Green
}

Write-Host ""
Write-Host "=== Migración terminada ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Ahora compila con:"
Write-Host "mvn -U clean package" -ForegroundColor Yellow
Write-Host ""