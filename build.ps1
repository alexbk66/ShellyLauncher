[CmdletBinding()]
param(
    [string]$SdkPath = 'C:\My\Android\Sdk',
    [string]$BuildToolsVersion = '35.0.0',
    [string]$JdkPath = $env:JAVA_HOME
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
# Every native invocation is checked explicitly, including on Windows PowerShell 5.1.
function Invoke-Checked {
    param([string]$Tool, [string[]]$Arguments)
    & $Tool @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Tool failed with exit code $LASTEXITCODE" }
}
function Require-File([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Required file missing: $Path" }
}

if (-not $JdkPath) { $JdkPath = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User') }
if (-not $JdkPath) { $JdkPath = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine') }
if (-not $JdkPath) { throw 'Set JAVA_HOME to JDK 17 or supply -JdkPath.' }
$java = Join-Path $JdkPath 'bin/java.exe'
$javac = Join-Path $JdkPath 'bin/javac.exe'
$jar = Join-Path $JdkPath 'bin/jar.exe'
$keytool = Join-Path $JdkPath 'bin/keytool.exe'
$tools = Join-Path $SdkPath "build-tools/$BuildToolsVersion"
$androidJar = Join-Path $SdkPath 'platforms/android-27/android.jar'
$d8 = Join-Path $tools 'lib/d8.jar'
$signer = Join-Path $tools 'lib/apksigner.jar'
$aapt = Join-Path $tools 'aapt.exe'
$zipalign = Join-Path $tools 'zipalign.exe'
foreach ($file in @($java, $javac, $jar, $keytool, $androidJar, $d8, $signer, $aapt, $zipalign)) {
    Require-File $file
}

$root = $PSScriptRoot
$buildRoot = Join-Path $root 'build'
$dist = Join-Path $root 'dist'
$local = Join-Path $root '.local'
foreach ($directory in @($buildRoot, $dist, $local)) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}
# A new work directory avoids stale classes/DEX without deleting existing files.
$work = Join-Path $buildRoot ([Guid]::NewGuid().ToString('N'))
$classes = Join-Path $work 'classes'
$testClasses = Join-Path $work 'tests'
$dex = Join-Path $work 'dex'
foreach ($directory in @($work, $classes, $testClasses, $dex)) {
    New-Item -ItemType Directory -Path $directory | Out-Null
}
$sourceRoot = Join-Path $root 'src/com/ak/shellylauncher'
Write-Host 'Testing automatic-launch policy...'
Invoke-Checked $javac @('-encoding', 'UTF-8', '-d', $testClasses,
    (Join-Path $sourceRoot 'AutoLaunchPolicy.java'),
    (Join-Path $root 'tests/com/ak/shellylauncher/AutoLaunchPolicyTest.java'))
Invoke-Checked $java @('-cp', $testClasses, 'com.ak.shellylauncher.AutoLaunchPolicyTest')
Write-Host 'Testing strict update-prompt matching...'
Invoke-Checked $javac @('-encoding', 'UTF-8', '-d', $testClasses,
    (Join-Path $sourceRoot 'UpdatePromptPolicy.java'),
    (Join-Path $root 'tests/com/ak/shellylauncher/UpdatePromptPolicyTest.java'))
Invoke-Checked $java @('-cp', $testClasses, 'com.ak.shellylauncher.UpdatePromptPolicyTest',
    (Join-Path $root 'tests/fixtures/shelly-update-prompt-en.tsv'))
Write-Host 'Testing bounded accessibility startup retries...'
Invoke-Checked $javac @('-encoding', 'UTF-8', '-d', $testClasses,
    (Join-Path $sourceRoot 'UpdateInspectionRetryPolicy.java'),
    (Join-Path $root 'tests/com/ak/shellylauncher/UpdateInspectionRetryPolicyTest.java'))
Invoke-Checked $java @('-cp', $testClasses, 'com.ak.shellylauncher.UpdateInspectionRetryPolicyTest')
Write-Host 'Testing popup-diagnostics privacy...'
Invoke-Checked $javac @('-encoding', 'UTF-8', '-d', $testClasses,
    (Join-Path $sourceRoot 'UpdatePromptPolicy.java'),
    (Join-Path $sourceRoot 'UpdateInspectionDiagnostics.java'),
    (Join-Path $root 'tests/com/ak/shellylauncher/UpdateInspectionDiagnosticsTest.java'))
Invoke-Checked $java @('-cp', $testClasses, 'com.ak.shellylauncher.UpdateInspectionDiagnosticsTest')
Write-Host 'Testing fixed matching reasons and diagnostic throttling...'
Invoke-Checked $javac @('-encoding', 'UTF-8', '-d', $testClasses,
    (Join-Path $sourceRoot 'UpdatePromptPolicy.java'),
    (Join-Path $root 'tests/com/ak/shellylauncher/UpdatePromptReasonTest.java'),
    (Join-Path $sourceRoot 'HelperLogThrottle.java'),
    (Join-Path $root 'tests/com/ak/shellylauncher/HelperLogThrottleTest.java'))
Invoke-Checked $java @('-cp', $testClasses, 'com.ak.shellylauncher.UpdatePromptReasonTest')
Invoke-Checked $java @('-cp', $testClasses, 'com.ak.shellylauncher.HelperLogThrottleTest')
Write-Host 'Compiling against Android API 27...'
$sources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src') -Recurse -Filter '*.java' |
    Sort-Object FullName | ForEach-Object { $_.FullName })
Invoke-Checked $javac (@('-encoding', 'UTF-8', '-source', '8', '-target', '8',
    '-bootclasspath', $androidJar, '-d', $classes) + $sources)
$classesJar = Join-Path $work 'classes.jar'
Invoke-Checked $jar @('cf', $classesJar, '-C', $classes, '.')
Invoke-Checked $java @('-cp', $d8, 'com.android.tools.r8.D8', '--min-api', '27',
    '--lib', $androidJar, '--output', $dex, $classesJar)

Write-Host 'Packaging and aligning...'
$unsigned = Join-Path $work 'unsigned.apk'
$aligned = Join-Path $work 'aligned.apk'
Invoke-Checked $aapt @('package', '-f', '-M', (Join-Path $root 'AndroidManifest.xml'),
    '-S', (Join-Path $root 'res'), '-I', $androidJar, '-F', $unsigned)
Push-Location $dex
try { Invoke-Checked $aapt @('add', $unsigned, 'classes.dex') }
finally { Pop-Location }
Invoke-Checked $zipalign @('-f', '4', $unsigned, $aligned)

$keystore = Join-Path $local 'debug.keystore'
if (-not (Test-Path -LiteralPath $keystore)) {
    Write-Host 'Creating project-local debug signing key...'
    Invoke-Checked $keytool @('-genkeypair', '-keystore', $keystore, '-storetype', 'JKS',
        '-storepass', 'android', '-keypass', 'android', '-alias', 'androiddebugkey',
        '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000',
        '-dname', 'CN=Android Debug,O=Android,C=US', '-noprompt')
}
$signed = Join-Path $work 'ShellyLauncher-debug.apk'
Invoke-Checked $java @('-jar', $signer, 'sign', '--ks', $keystore,
    '--ks-key-alias', 'androiddebugkey', '--ks-pass', 'pass:android', '--key-pass',
    'pass:android', '--min-sdk-version', '27', '--out', $signed, $aligned)
Invoke-Checked $java @('-jar', $signer, 'verify', '--verbose', '--print-certs', $signed)
Invoke-Checked $zipalign @('-c', '-v', '4', $signed)
Invoke-Checked $aapt @('dump', 'badging', $signed)
$output = Join-Path $dist 'ShellyLauncher-debug.apk'
Copy-Item -LiteralPath $signed -Destination $output -Force
Write-Host "Built: $output"
Get-FileHash -LiteralPath $output -Algorithm SHA256
