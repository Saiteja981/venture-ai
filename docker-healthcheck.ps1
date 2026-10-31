Write-Host "--- Docker Compose Health Check ---" -ForegroundColor Cyan

# 1. Check if services are running
$services = docker compose ps --services
if (-not $services) {
    Write-Host "FAIL: No services running. Run 'docker compose up -d' first." -ForegroundColor Red
    exit 1
}

# 2. Check Frontend (5173)
try {
    $res = Invoke-WebRequest -Uri "http://localhost:5173" -UseBasicParsing -TimeoutSec 5
    if ($res.StatusCode -eq 200) { Write-Host "PASS: Frontend (5173) is UP" -ForegroundColor Green }
} catch { Write-Host "FAIL: Frontend (5173) is DOWN" -ForegroundColor Red }

# 3. Check Backend (8082)
try {
    $res = Invoke-WebRequest -Uri "http://localhost:8082/api/auth/login" -Method Head -UseBasicParsing -TimeoutSec 5
    Write-Host "PASS: Backend (8082) is responding (HTTP $($res.StatusCode))" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "PASS: Backend (8082) is UP and enforcing security (401)" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Backend (8082) is DOWN or erroring" -ForegroundColor Red
    }
}

# 4. Check AI Service (8000)
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8000/health" -TimeoutSec 5
    if ($res.status -eq "healthy") { Write-Host "PASS: AI Service (8000) is UP" -ForegroundColor Green }
} catch { Write-Host "FAIL: AI Service (8000) is DOWN" -ForegroundColor Red }

# 5. Check MySQL (3306 inside docker, usually mapped differently)
Write-Host "Checking database connectivity via Docker..."
docker compose exec -T mysql mysqladmin ping -h localhost -u root -p"$env:MYSQL_ROOT_PASSWORD" 2>$null
if ($LASTEXITCODE -eq 0) { Write-Host "PASS: MySQL is UP" -ForegroundColor Green }
else { Write-Host "FAIL: MySQL ping failed" -ForegroundColor Red }