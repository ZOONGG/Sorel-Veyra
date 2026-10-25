param(
    [ValidateRange(1,4)][int]$Scale = 2,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$GradleHome = $env:GRADLE_USER_HOME,
    [string]$Natives = "$env:USERPROFILE/.lunarclient/offline/multiver/natives"
)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if (!$JavaHome) { $JavaHome = Join-Path $projectRoot '.toolchains/jdk17/jdk-17.0.19+10' }
if (!$GradleHome) { $GradleHome = Join-Path $env:USERPROFILE '.gradle' }
$dependency = Get-ChildItem (Join-Path $GradleHome 'caches/modules-2/files-2.1/org.lwjgl.lwjgl/lwjgl') -Recurse -Filter 'lwjgl-2.9.4-nightly-20150209.jar' | Select-Object -First 1
if (!$dependency) { throw 'Build Veyra first to cache LWJGL 2.9.4.' }
$classDirectory = Join-Path $projectRoot 'build/ui-preview-classes'
$outputDirectory = Join-Path $projectRoot '.artifacts/redesign'
New-Item -ItemType Directory -Force $classDirectory, $outputDirectory | Out-Null
$sourceFiles = @(Get-ChildItem (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
$sourceFiles += @('notification/NotificationToast.java', 'notification/NotificationQueue.java', 'notification/NotificationMotion.java', 'clickgui/UiTokens.java', 'clickgui/UiIcons.java', 'clickgui/UiDraw.java', 'clickgui/font/VeyraFont.java', 'clickgui/font/VeyraFonts.java', 'utils/render/RenderUtils.java') | ForEach-Object { Join-Path $projectRoot "src/main/java/dev/veyra/client/$_" }
& "$JavaHome/bin/javac.exe" -encoding UTF-8 -cp $dependency.FullName -d $classDirectory @sourceFiles
if ($LASTEXITCODE -ne 0) { throw 'Renderer probe compilation failed.' }
$classPath = "$classDirectory;$(Join-Path $projectRoot 'src/main/resources');$($dependency.FullName)"
& "$JavaHome/bin/java.exe" "-Dorg.lwjgl.librarypath=$Natives" -cp $classPath UiPreview (Join-Path $outputDirectory "primitives-scale$Scale.png") $Scale
if ($LASTEXITCODE -ne 0) { throw 'Renderer probe failed.' }
