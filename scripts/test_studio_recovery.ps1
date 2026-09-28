# Exercises the exact command shipped in the .cmd file with fake processes only.
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$line = Get-Content (Join-Path $PSScriptRoot 'recuperar-android-studio.cmd') | Where-Object { $_ -like 'powershell.exe*' }
if ($line -notmatch '^-?.* -Command "(.*)"$') { throw 'Recovery command not found' }
$command = [scriptblock]::Create($Matches[1])
$script:expectedPath = 'C:\Program Files\Android\Android Studio1\bin\studio64.exe'

function Assert-True($condition, $message) { if (!$condition) { throw $message } }
function Reset-Fixture {
    $script:events = [System.Collections.Generic.List[string]]::new()
    $script:installed = $true
    $script:other = @()
    $script:target = [pscustomobject]@{
        Id=5432; ProcessName='studio64'; Path=$script:expectedPath
        Responding=$false; Graceful=$false; Stopped=$false; RefusesExit=$false
    }
    $script:target | Add-Member ScriptMethod CloseMainWindow {
        $script:events.Add('close'); return $this.Graceful
    }
    $script:target | Add-Member ScriptMethod WaitForExit {
        param($timeout)
        return !$this.RefusesExit -and ($this.Graceful -or $this.Stopped)
    }
}
# These shadow only the commands used by the recovery command. No real processes are stopped/launched.
function Test-Path { param($LiteralPath, $PathType) return $script:installed }
function Get-Process {
    param($Id, $Name, $ErrorAction)
    if ($null -ne $Id) { return $script:target }
    return $script:other
}
function Stop-Process {
    param($InputObject, [switch]$Force, $ErrorAction)
    Assert-True ($InputObject -eq $script:target) 'Tried to stop a different process'
    $script:events.Add('stop'); $InputObject.Stopped=$true
}
function Start-Process {
    param($FilePath, $ErrorAction)
    Assert-True ($FilePath -eq $script:expectedPath) 'Wrong executable'
    $script:events.Add('start')
}
function Expect-Failure {
    $failed=$false
    try { & $command } catch { $failed=$true }
    Assert-True $failed 'Expected refusal'
    Assert-True (!$script:events.Contains('start')) 'Must not launch after an error'
}

Reset-Fixture
& $command
Assert-True (($script:events -join ',') -eq 'close,stop,start') 'Hung process was not recovered in order'

Reset-Fixture
$script:target.Graceful=$true
& $command
Assert-True (($script:events -join ',') -eq 'close,start') 'Graceful exit must not be forced'

Reset-Fixture
$script:target.Responding=$true
Expect-Failure
Assert-True (!$script:events.Contains('stop')) 'Responding editor with pending changes was force closed'

Reset-Fixture
$script:target.ProcessName='notepad'
Expect-Failure
Assert-True ($script:events.Count -eq 0) 'A reused PID was touched'

Reset-Fixture
$script:target.Path='C:\Other\Android Studio\bin\studio64.exe'
Expect-Failure
Assert-True ($script:events.Count -eq 0) 'A different installation was touched'

Reset-Fixture
$script:target=$null
& $command
Assert-True (($script:events -join ',') -eq 'start') 'Already exited process should not be killed'

Reset-Fixture
$script:other=@([pscustomobject]@{Path=$script:expectedPath})
$script:target=$null
& $command
Assert-True ($script:events.Count -eq 0) 'Must not duplicate an existing instance'

Reset-Fixture
$script:installed=$false
Expect-Failure
Assert-True ($script:events.Count -eq 0) 'Missing installation should not change anything'

Reset-Fixture
$script:target.RefusesExit=$true
Expect-Failure
Assert-True (($script:events -join ',') -eq 'close,stop') 'Must wait for exit before relaunching'

Write-Host '9 recovery scenarios passed; no real process was stopped or launched.'
