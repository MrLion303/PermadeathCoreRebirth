$ErrorActionPreference = "Stop"

Write-Host "================================================"
Write-Host " PermadeathCoreRebirth - NMS 26.3 - Paso 1"
Write-Host "================================================"
Write-Host ""

$javaRoot = Join-Path $PSScriptRoot "v26_3\src\main\java"

if (!(Test-Path $javaRoot)) {
    throw "No se encontro: $javaRoot"
}

$replacements = [ordered]@{

    # Paquete propio del plugin
    "tech.sebazcrc.permadeath.nms.v1_20_R1" =
        "tech.sebazcrc.permadeath.nms.v26_3"

    # Desde Minecraft moderno CraftBukkit ya no usa v1_20_R1 en el package
    "org.bukkit.craftbukkit.v1_20_R1" =
        "org.bukkit.craftbukkit"

    # Mojang NMS 26.x
    "net.minecraft.world.entity.MobSpawnType" =
        "net.minecraft.world.entity.EntitySpawnReason"

    "MobSpawnType" =
        "EntitySpawnReason"

    # Animales movidos a subpackages
    "net.minecraft.world.entity.animal.Cod" =
        "net.minecraft.world.entity.animal.fish.Cod"

    "net.minecraft.world.entity.animal.Bee" =
        "net.minecraft.world.entity.animal.bee.Bee"

    "net.minecraft.world.entity.animal.Pig" =
        "net.minecraft.world.entity.animal.pig.Pig"

    # Minecart spawner
    "net.minecraft.world.entity.vehicle.MinecartSpawner" =
        "net.minecraft.world.entity.vehicle.minecart.MinecartSpawner"

    # Bukkit Attributes modernos
    "Attribute.GENERIC_ARMOR_TOUGHNESS" =
        "Attribute.ARMOR_TOUGHNESS"

    "Attribute.GENERIC_ATTACK_DAMAGE" =
        "Attribute.ATTACK_DAMAGE"

    "Attribute.GENERIC_ATTACK_SPEED" =
        "Attribute.ATTACK_SPEED"

    "Attribute.GENERIC_ATTACK_KNOCKBACK" =
        "Attribute.ATTACK_KNOCKBACK"

    "Attribute.GENERIC_KNOCKBACK_RESISTANCE" =
        "Attribute.KNOCKBACK_RESISTANCE"

    "Attribute.GENERIC_FLYING_SPEED" =
        "Attribute.FLYING_SPEED"

    "Attribute.GENERIC_FOLLOW_RANGE" =
        "Attribute.FOLLOW_RANGE"

    "Attribute.GENERIC_MOVEMENT_SPEED" =
        "Attribute.MOVEMENT_SPEED"

    "Attribute.GENERIC_MAX_HEALTH" =
        "Attribute.MAX_HEALTH"

    "Attribute.GENERIC_ARMOR" =
        "Attribute.ARMOR"

    "Attribute.GENERIC_LUCK" =
        "Attribute.LUCK"

    "Attribute.HORSE_JUMP_STRENGTH" =
        "Attribute.JUMP_STRENGTH"
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

$files = Get-ChildItem `
    -Path $javaRoot `
    -Filter "*.java" `
    -File `
    -Recurse

$changedFiles = 0
$totalChanges = 0

foreach ($file in $files) {

    $original = Get-Content `
        -Path $file.FullName `
        -Raw `
        -Encoding UTF8

    $content = $original

    foreach ($entry in $replacements.GetEnumerator()) {

        $old = [string]$entry.Key
        $new = [string]$entry.Value

        $count = (
            [regex]::Matches(
                $content,
                [regex]::Escape($old)
            )
        ).Count

        if ($count -gt 0) {

            $content = $content.Replace(
                $old,
                $new
            )

            $totalChanges += $count
        }
    }

    if ($content -ne $original) {

        [System.IO.File]::WriteAllText(
            $file.FullName,
            $content,
            $utf8NoBom
        )

        Write-Host "[MODIFICADO] $($file.FullName)"

        $changedFiles++
    }
}

Write-Host ""
Write-Host "Corrigiendo carpeta del package NMS..."

$oldPackageFolder = Join-Path `
    $javaRoot `
    "tech\sebazcrc\permadeath\nms\v1_20_R1"

$newPackageFolder = Join-Path `
    $javaRoot `
    "tech\sebazcrc\permadeath\nms\v26_3"

if (Test-Path $oldPackageFolder) {

    if (Test-Path $newPackageFolder) {
        throw "Ya existe la carpeta destino: $newPackageFolder"
    }

    Move-Item `
        -Path $oldPackageFolder `
        -Destination $newPackageFolder

    Write-Host "[RENOMBRADO] v1_20_R1 -> v26_3"
}
else {
    Write-Host "[INFO] La carpeta v1_20_R1 ya no existe; no fue necesario renombrarla."
}

Write-Host ""
Write-Host "================================================"
Write-Host " Paso 1 completado"
Write-Host " Archivos modificados: $changedFiles"
Write-Host " Reemplazos realizados: $totalChanges"
Write-Host "================================================"