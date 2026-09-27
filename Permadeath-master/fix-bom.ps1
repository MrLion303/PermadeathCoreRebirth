$ErrorActionPreference = "Stop"

$root = $PSScriptRoot

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "     Reparando UTF-8 BOM del proyecto" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$extensions = @(
    ".java",
    ".xml",
    ".yml",
    ".yaml",
    ".properties",
    ".md",
    ".json",
    ".txt"
)

$fixed = 0
$checked = 0

$files = Get-ChildItem -Path $root -Recurse -File |
    Where-Object {

        $_.FullName -notmatch "\\target\\" -and
        $_.FullName -notmatch "\\\.git\\" -and
        $extensions -contains $_.Extension.ToLower()
    }

foreach ($file in $files) {

    $checked++

    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)

    if (
        $bytes.Length -ge 3 -and
        $bytes[0] -eq 0xEF -and
        $bytes[1] -eq 0xBB -and
        $bytes[2] -eq 0xBF
    ) {

        Write-Host "BOM encontrado: $($file.FullName)" -ForegroundColor Yellow

        $newBytes = New-Object byte[] ($bytes.Length - 3)

        [Array]::Copy(
            $bytes,
            3,
            $newBytes,
            0,
            $bytes.Length - 3
        )

        [System.IO.File]::WriteAllBytes(
            $file.FullName,
            $newBytes
        )

        $fixed++

        Write-Host "  -> Reparado" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Archivos revisados: $checked"
Write-Host "Archivos reparados: $fixed" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

if ($fixed -eq 0) {
    Write-Host "No se encontraron archivos con UTF-8 BOM." -ForegroundColor Yellow
} else {
    Write-Host "BOM eliminado correctamente." -ForegroundColor Green
}

Write-Host ""
Write-Host "Ahora ejecuta:" -ForegroundColor White
Write-Host "mvn -U clean package" -ForegroundColor Yellow
Write-Host ""