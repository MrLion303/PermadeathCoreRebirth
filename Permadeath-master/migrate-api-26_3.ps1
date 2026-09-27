$ErrorActionPreference = "Stop"

Write-Host "==============================================="
Write-Host " PermadeathCoreRebirth - Migracion API 26.3"
Write-Host "==============================================="
Write-Host ""

$root = Join-Path $PSScriptRoot "main\src\main\java"

if (!(Test-Path $root)) {
    throw "No se encontro la carpeta de codigo Java: $root"
}

$replacements = [ordered]@{

    # PotionEffectType
    "PotionEffectType.SLOW_DIGGING"      = "PotionEffectType.MINING_FATIGUE"
    "PotionEffectType.DAMAGE_RESISTANCE" = "PotionEffectType.RESISTANCE"
    "PotionEffectType.INCREASE_DAMAGE"   = "PotionEffectType.STRENGTH"
    "PotionEffectType.CONFUSION"         = "PotionEffectType.NAUSEA"
    "PotionEffectType.HARM"              = "PotionEffectType.INSTANT_DAMAGE"
    "PotionEffectType.SLOW"              = "PotionEffectType.SLOWNESS"

    # PotionType
    "PotionType.INSTANT_DAMAGE"          = "PotionType.HARMING"

    # Attributes
    "Attribute.GENERIC_ARMOR_TOUGHNESS"  = "Attribute.ARMOR_TOUGHNESS"
    "Attribute.GENERIC_ATTACK_DAMAGE"    = "Attribute.ATTACK_DAMAGE"
    "Attribute.GENERIC_MAX_HEALTH"       = "Attribute.MAX_HEALTH"
    "Attribute.GENERIC_ARMOR"            = "Attribute.ARMOR"

    # Enchantments
    "Enchantment.PROTECTION_ENVIRONMENTAL" = "Enchantment.PROTECTION"
    "Enchantment.ARROW_INFINITE"           = "Enchantment.INFINITY"
    "Enchantment.ARROW_KNOCKBACK"          = "Enchantment.PUNCH"
    "Enchantment.ARROW_DAMAGE"             = "Enchantment.POWER"
    "Enchantment.DAMAGE_ALL"               = "Enchantment.SHARPNESS"

    # EntityType
    "EntityType.ENDER_CRYSTAL" = "EntityType.END_CRYSTAL"
    "EntityType.PRIMED_TNT"    = "EntityType.TNT"
    "EntityType.DROPPED_ITEM"  = "EntityType.ITEM"
    "EntityType.FIREWORK"      = "EntityType.FIREWORK_ROCKET"

    # Particles
    "Particle.VILLAGER_HAPPY"  = "Particle.HAPPY_VILLAGER"
    "Particle.SMOKE_NORMAL"    = "Particle.SMOKE"
    "Particle.EXPLOSION_HUGE"  = "Particle.EXPLOSION_EMITTER"
    "Particle.BLOCK_CRACK"     = "Particle.BLOCK"

    # GameRules
    "GameRule.NATURAL_REGENERATION" = "GameRule.NATURAL_HEALTH_REGENERATION"
}

$files = Get-ChildItem -Path $root -Recurse -Filter "*.java" -File

$changedFiles = 0
$totalReplacements = 0

foreach ($file in $files) {

    $original = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $content = $original

    foreach ($entry in $replacements.GetEnumerator()) {

        $old = [string]$entry.Key
        $new = [string]$entry.Value

        # Evita reemplazar una constante que ya fue migrada
        $pattern = [regex]::Escape($old) + "(?![A-Za-z0-9_])"

        $matches = [regex]::Matches($content, $pattern).Count

        if ($matches -gt 0) {
            $content = [regex]::Replace($content, $pattern, $new)
            $totalReplacements += $matches
        }
    }

    # Bukkit 26.3 tiene dos overloads:
    # damage(double, Entity)
    # damage(double, DamageSource)
    #
    # Pasar null ahora es ambiguo.
    # En estos casos el plugin simplemente quiere causar daño sin fuente.
    $oldDamage = "damage(e.getPlayer().getHealth() + 1.0D, null)"
    $newDamage = "damage(e.getPlayer().getHealth() + 1.0D)"

    $damageMatches = ([regex]::Matches(
        $content,
        [regex]::Escape($oldDamage)
    )).Count

    if ($damageMatches -gt 0) {
        $content = $content.Replace($oldDamage, $newDamage)
        $totalReplacements += $damageMatches
    }

    if ($content -ne $original) {

        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)

        [System.IO.File]::WriteAllText(
            $file.FullName,
            $content,
            $utf8NoBom
        )

        $changedFiles++

        Write-Host "[MODIFICADO] $($file.FullName)"
    }
}

Write-Host ""
Write-Host "==============================================="
Write-Host " Migracion terminada"
Write-Host " Archivos modificados: $changedFiles"
Write-Host " Reemplazos realizados: $totalReplacements"
Write-Host "==============================================="