$dbName = "venture_ai"
$dbUser = "root"
$dbPass = "srinivas" # Update if your local password differs
$backupFile = "backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"

Write-Host "--- 1. Creating Backup ---" -ForegroundColor Cyan
docker exec -i venture-ai-mysql-1 mysqldump -u $dbUser -p$dbPass $dbName > $backupFile
if (Test-Path $backupFile) { Write-Host "PASS: Backup created ($backupFile)" -ForegroundColor Green }

Write-Host "--- 2. Restoring to test database ---" -ForegroundColor Cyan
docker exec -i venture-ai-mysql-1 mysql -u $dbUser -p$dbPass -e "DROP DATABASE IF EXISTS ${dbName}_restore_test; CREATE DATABASE ${dbName}_restore_test;"
Get-Content $backupFile | docker exec -i venture-ai-mysql-1 mysql -u $dbUser -p$dbPass ${dbName}_restore_test
Write-Host "PASS: Restored to ${dbName}_restore_test" -ForegroundColor Green

Write-Host "--- 3. Verifying row counts ---" -ForegroundColor Cyan
docker exec -i venture-ai-mysql-1 mysql -u $dbUser -p$dbPass -e "SELECT COUNT(*) as user_count FROM users;" ${dbName}_restore_test