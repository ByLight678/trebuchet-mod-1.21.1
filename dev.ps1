# Use the project-local Java 21 JDK when available.
$ErrorActionPreference = 'Stop'
$projectJdk = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot '.tools') -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($projectJdk) {
    $env:JAVA_HOME = $projectJdk.FullName
    $env:PATH = (Join-Path $env:JAVA_HOME 'bin') + [IO.Path]::PathSeparator + $env:PATH
}
$env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-user-home'
Push-Location $PSScriptRoot
try {
    & .\gradlew.bat @args
    $gradleExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $gradleExitCode
