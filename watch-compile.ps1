
$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = "$PSScriptRoot\src\main\java"
$watcher.Filter = "*.java"
$watcher.IncludeSubdirectories = $true
$watcher.EnableRaisingEvents = $true

$action = {
    $path = $Event.SourceEventArgs.FullPath
    Write-Host "Detected change in $path" -ForegroundColor Yellow
    Write-Host "Triggering Maven Compile..." -ForegroundColor Cyan
    
    # Run maven compile in the script's directory (hogu-server)
    Push-Location $PSScriptRoot
    
    # We use cmd /c to ensure it runs correctly on Windows
    cmd /c "mvn compile -DskipTests"
    
    if ($?) {
        Write-Host "Compilation successful. Backend should auto-restart." -ForegroundColor Green
    } else {
        Write-Host "Compilation failed." -ForegroundColor Red
    }
    
    Pop-Location
}

# Register events
Register-ObjectEvent $watcher "Changed" -Action $action
# Also watch for creation/deletion if you add new files
Register-ObjectEvent $watcher "Created" -Action $action
Register-ObjectEvent $watcher "Deleted" -Action $action
Register-ObjectEvent $watcher "Renamed" -Action $action

Write-Host "Monitoring $PSScriptRoot\src\main\java for changes..." -ForegroundColor Green
Write-Host "Press Ctrl+C to stop watcher." -ForegroundColor Gray

# Keep script running
while ($true) { Start-Sleep -Seconds 1 }
