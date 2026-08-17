param(
    [switch]$UpdateBaseline
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$baselinePath = Join-Path $PSScriptRoot 'protected-player.sha256'

$protectedFiles = @(
    'app/src/main/kotlin/com/mudassir131/yt/ui/player/Player.kt',
    'app/src/main/kotlin/com/mudassir131/yt/ui/player/PlayerComponents.kt',
    'app/src/main/kotlin/com/mudassir131/yt/ui/player/Thumbnail.kt',
    'app/src/main/kotlin/com/mudassir131/yt/ui/player/Queue.kt',
    'app/src/main/kotlin/com/mudassir131/yt/ui/player/QueueComponents.kt',
    'app/src/main/kotlin/com/mudassir131/yt/ui/player/LyricsScreen.kt',
    'app/src/main/kotlin/com/mudassir131/yt/ui/component/Lyrics.kt',
    'app/src/main/kotlin/com/mudassir131/yt/ui/component/LyricsV2.kt',
    'app/src/main/kotlin/com/mudassir131/yt/ui/menu/PlayerMenu.kt',
    'app/src/main/kotlin/com/mudassir131/yt/ui/menu/LyricsMenu.kt'
)

function Get-ProtectedHashes {
    foreach ($relativePath in $protectedFiles) {
        $absolutePath = Join-Path $repositoryRoot $relativePath
        if (-not (Test-Path -LiteralPath $absolutePath)) {
            throw "Protected player file is missing: $relativePath"
        }

        $content = [System.IO.File]::ReadAllText($absolutePath)
        $normalized = $content -replace "\r\n", "`n" -replace "\r", "`n"
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($normalized)
        $sha = [System.Security.Cryptography.SHA256]::Create()
        $hashBytes = $sha.ComputeHash($bytes)
        $hash = ($hashBytes | ForEach-Object { '{0:x2}' -f $_ }) -join ''
        "$hash  $relativePath"
    }
}

if ($UpdateBaseline) {
    Get-ProtectedHashes | Set-Content -LiteralPath $baselinePath -Encoding utf8
    Write-Host "Updated protected-player baseline: $baselinePath"
    exit 0
}

if (-not (Test-Path -LiteralPath $baselinePath)) {
    throw "Protected-player baseline is missing: $baselinePath"
}

$expected = Get-Content -LiteralPath $baselinePath | Where-Object { $_.Trim() }
$actual = @(Get-ProtectedHashes)
$differences = Compare-Object -ReferenceObject $expected -DifferenceObject $actual

if ($differences) {
    Write-Error "Protected Nocturne player files changed:`n$($differences | Out-String)"
    exit 1
}

Write-Host "Protected Nocturne player verification passed ($($actual.Count) files)."
