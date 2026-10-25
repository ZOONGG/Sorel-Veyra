param(
    [string]$Baseline = "$PSScriptRoot/../../../builds/Veyra-modern-old-timer-candidate-20261024/Veyra-0.1.1-modern-old-timer-candidate.jar",
    [string]$Candidate = "$PSScriptRoot/../../build/libs/Veyra-0.1.1-moonrise.1.jar",
    [string]$Output = "$PSScriptRoot/../../build/bind-ux-probe/jar-audit.json"
)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Read-Jar([string]$Path) {
    $zip=[IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Path));$map=@{}
    try {
        foreach($entry in $zip.Entries) {
            if($entry.FullName.EndsWith('/')) { continue }
            $stream=$entry.Open()
            try { $map[$entry.FullName]=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($stream)) }
            finally { $stream.Dispose() }
        }
    } finally { $zip.Dispose() }
    return $map
}
$before=Read-Jar $Baseline;$after=Read-Jar $Candidate
$differences=@(foreach($key in @(@($before.Keys)+@($after.Keys) | Sort-Object -Unique)) {
    if($before[$key] -ne $after[$key]) { [ordered]@{path=$key;before=$before[$key];after=$after[$key]} }
})
$allowed='^(META-INF/MANIFEST\.MF|weave\.mod\.json|dev/veyra/(client/(clickgui/(ClickGui(\$|\.)|ModuleDescriptions\.)|module/(Module(\$|\.)|ModuleManager\.|bind/CapturedBindInput\.|modules/(movement/Sprint\.|player/(SafeWalk(\$|\.)|AutoBlockIn\.)))|player/bridge/BridgeAssist(Context|Engine|Settings)\.)|weave/mixins/EntityPlayerSPMixin\.))'
foreach($change in $differences) { if($change.path -notmatch $allowed) { throw "Unexpected JAR change: $($change.path)" } }
foreach($key in $before.Keys) {
    if($key -match 'Timer|Freeze|assets/veyra/fonts|client/notification|client/clickgui/(Ui|font)|client/module/modules/combat/AutoBlock') {
        if($before[$key] -ne $after[$key]) { throw "Protected entry changed: $key" }
    }
}
if(@($after.Keys | Where-Object { $_ -match 'AutoBlockIn|bind-ux-probe|BindUxProbe' }).Count -ne 0) { throw 'Removed module or probe packaged in JAR' }
$candidateZip=[IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Candidate))
try {
    foreach($entry in $candidateZip.Entries) {
        if(!$entry.FullName.EndsWith('.class')) { continue }
        $stream=$entry.Open()
        try {
            $header=[byte[]]::new(8);$stream.ReadExactly($header)
            if((([int]$header[6] -shl 8) -bor $header[7]) -ne 61) { throw "Non-Java-17 class: $($entry.FullName)" }
        } finally { $stream.Dispose() }
    }
} finally { $candidateZip.Dispose() }
$report=[ordered]@{baseline=[IO.Path]::GetFullPath($Baseline);candidate=[IO.Path]::GetFullPath($Candidate);baselineEntries=$before.Count;candidateEntries=$after.Count;differences=$differences;protectedTimerFreezeFontsNotificationsAutoBlock='byte-identical';javaClassMajor=61}
$report | ConvertTo-Json -Depth 7 | Set-Content -Encoding utf8 $Output
Write-Output "JAR audit PASS: $($differences.Count) expected changed entries; Timer/Freeze/fonts/notifications/AutoBlock byte-identical; AutoBlockIn absent."
$differences.path
