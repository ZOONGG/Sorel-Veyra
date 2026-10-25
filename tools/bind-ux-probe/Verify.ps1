param([string]$ClasspathFile = "$PSScriptRoot/../../build/bind-ux-probe-classpath.txt")
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$java = Join-Path $root '.toolchains/jdk17/jdk-17.0.19+10/bin'
$output = Join-Path $root 'build/bind-ux-probe'
New-Item -ItemType Directory -Force $output | Out-Null
if (!(Test-Path -LiteralPath $ClasspathFile)) { throw 'Run gradlew.bat -I tools/bind-ux-probe/classpath.init.gradle bindUxClasspath --offline first.' }
$dependencies = [IO.File]::ReadAllText([IO.Path]::GetFullPath($ClasspathFile)).Trim()
$classpath = "$output;$dependencies"
$files = @((Join-Path $PSScriptRoot 'BindUxProbe.java'))
$files += @(Get-ChildItem (Join-Path $root 'tools/renderer-preview/src/net') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
& "$java/javac.exe" -encoding UTF-8 -cp $dependencies -d $output @files
if ($LASTEXITCODE -ne 0) { throw 'Bind UX fixture compilation failed' }
$natives = Join-Path $env:USERPROFILE '.lunarclient/offline/multiver/natives'
& "$java/java.exe" -Xverify:all "-Dorg.lwjgl.librarypath=$natives" -cp $classpath BindUxProbe (Join-Path $output 'bind-cards.png')
if ($LASTEXITCODE -ne 0) { throw 'Bind UX fixture failed' }
