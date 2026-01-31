# FilmHub Project Line Count Script
# Counts lines in HTML, CSS, JavaScript, Java, SQL files

Write-Host "=== FilmHub Project Line Count ===" -ForegroundColor Green
Write-Host "Count Time: $(Get-Date)"
Write-Host ""

# Count total lines
$totalLines = 0
$totalFiles = 0

# Count HTML files
Write-Host "Counting HTML files (*.html)..." -ForegroundColor Yellow
$htmlFiles = Get-ChildItem -Path "src" -Recurse -Filter "*.html" -File
$htmlLines = 0
foreach ($file in $htmlFiles) {
    $lineCount = (Get-Content $file.FullName | Measure-Object -Line).Lines
    $htmlLines += $lineCount
}
Write-Host "  Files: $($htmlFiles.Count)" -ForegroundColor Cyan
Write-Host "  Lines: $htmlLines" -ForegroundColor Cyan
Write-Host ""
$totalLines += $htmlLines
$totalFiles += $htmlFiles.Count

# Count CSS files
Write-Host "Counting CSS files (*.css)..." -ForegroundColor Yellow
$cssFiles = Get-ChildItem -Path "src" -Recurse -Filter "*.css" -File
$cssLines = 0
foreach ($file in $cssFiles) {
    $lineCount = (Get-Content $file.FullName | Measure-Object -Line).Lines
    $cssLines += $lineCount
}
Write-Host "  Files: $($cssFiles.Count)" -ForegroundColor Cyan
Write-Host "  Lines: $cssLines" -ForegroundColor Cyan
Write-Host ""
$totalLines += $cssLines
$totalFiles += $cssFiles.Count

# Count JavaScript files
Write-Host "Counting JavaScript files (*.js)..." -ForegroundColor Yellow
$jsFiles = Get-ChildItem -Path "src" -Recurse -Filter "*.js" -File
$jsLines = 0
foreach ($file in $jsFiles) {
    $lineCount = (Get-Content $file.FullName | Measure-Object -Line).Lines
    $jsLines += $lineCount
}
Write-Host "  Files: $($jsFiles.Count)" -ForegroundColor Cyan
Write-Host "  Lines: $jsLines" -ForegroundColor Cyan
Write-Host ""
$totalLines += $jsLines
$totalFiles += $jsFiles.Count

# Count Java files
Write-Host "Counting Java files (*.java)..." -ForegroundColor Yellow
$javaFiles = Get-ChildItem -Path "src" -Recurse -Filter "*.java" -File
$javaLines = 0
foreach ($file in $javaFiles) {
    $lineCount = (Get-Content $file.FullName | Measure-Object -Line).Lines
    $javaLines += $lineCount
}
Write-Host "  Files: $($javaFiles.Count)" -ForegroundColor Cyan
Write-Host "  Lines: $javaLines" -ForegroundColor Cyan
Write-Host ""
$totalLines += $javaLines
$totalFiles += $javaFiles.Count

# Count SQL files
Write-Host "Counting SQL files (*.sql)..." -ForegroundColor Yellow
$sqlFiles = Get-ChildItem -Path "db" -Recurse -Filter "*.sql" -File
$sqlLines = 0
foreach ($file in $sqlFiles) {
    $lineCount = (Get-Content $file.FullName | Measure-Object -Line).Lines
    $sqlLines += $lineCount
}
Write-Host "  Files: $($sqlFiles.Count)" -ForegroundColor Cyan
Write-Host "  Lines: $sqlLines" -ForegroundColor Cyan
Write-Host ""
$totalLines += $sqlLines
$totalFiles += $sqlFiles.Count

# Show summary
Write-Host "=== Summary ===" -ForegroundColor Green
Write-Host "Total Files: $totalFiles" -ForegroundColor Magenta
Write-Host "Total Lines: $totalLines" -ForegroundColor Magenta
Write-Host "Count completed!" -ForegroundColor Green