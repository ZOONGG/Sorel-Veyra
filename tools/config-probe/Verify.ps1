param([switch]$Original)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$java=Join-Path $root '.toolchains/jdk17/jdk-17.0.19+10/bin'
$output=Join-Path $root ('build/config-probe'+$(if($Original){'-original'}else{''}))
New-Item -ItemType Directory -Force $output | Out-Null
$source=Join-Path $root 'src/main/java/dev/veyra/client/config/ClientConfig.java'
if($Original){
 $source=Join-Path $output 'ClientConfig.java'
 git -C $root show HEAD:src/main/java/dev/veyra/client/config/ClientConfig.java | Set-Content -Encoding utf8 $source
}
$files=@(Get-ChildItem (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
& "$java/javac.exe" -encoding UTF-8 -d $output @files $source
if($LASTEXITCODE -ne 0){throw 'Config probe compilation failed'}
& "$java/java.exe" -Xverify:all -cp $output ClientConfigProbe (Join-Path $output 'test-home')
if($LASTEXITCODE -ne 0){throw 'Config probe failed'}
