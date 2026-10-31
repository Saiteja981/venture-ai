$ErrorActionPreference = "Stop"
$base = "http://localhost:8082/api"
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$startupEmail = "startup_$timestamp@test.com"
$investorEmail = "investor_$timestamp@test.com"
$adminEmail = "admin_$timestamp@test.com"
$password = "Password123!"

function Log-Step($msg) { Write-Host "`n[TEST] $msg" -ForegroundColor Cyan }
function Log-Pass($msg) { Write-Host "  -> PASS: $msg" -ForegroundColor Green }
function Log-Fail($msg) { Write-Host "  -> FAIL: $msg" -ForegroundColor Red; exit 1 }

# 1. REGISTER USERS
Log-Step "Registering users..."
$startup = Invoke-RestMethod -Uri "$base/auth/register" -Method Post -ContentType "application/json" -Body (@{name="E2E Startup"; email=$startupEmail; password=$password; role="STARTUP"} | ConvertTo-Json)
$investor = Invoke-RestMethod -Uri "$base/auth/register" -Method Post -ContentType "application/json" -Body (@{name="E2E Investor"; email=$investorEmail; password=$password; role="INVESTOR"} | ConvertTo-Json)
$admin = Invoke-RestMethod -Uri "$base/auth/register" -Method Post -ContentType "application/json" -Body (@{name="E2E Admin"; email=$adminEmail; password=$password; role="ADMIN"} | ConvertTo-Json)
Log-Pass "Users registered (Startup: $($startup.userId), Investor: $($investor.userId), Admin: $($admin.userId))"

$hStartup = @{ Authorization = "Bearer $($startup.token)" }
$hInvestor = @{ Authorization = "Bearer $($investor.token)" }
$hAdmin = @{ Authorization = "Bearer $($admin.token)" }

# 2. CREATE PROFILES
Log-Step "Creating profiles..."
$startupProfile = @{ companyName="E2E Startup Co"; description="Testing"; industry="FinTech"; businessModel="B2B SaaS"; startupStage="SEED"; teamSize=10; totalUsers=1000; customerCount=100; monthlyRevenue=10000; revenueGrowthRate=15; totalFunding=500000; monthlyBurnRate=5000; runwayMonths=18; valuation=2000000 } | ConvertTo-Json
$sProfileRes = Invoke-RestMethod -Uri "$base/startups" -Method Post -Headers $hStartup -ContentType "application/json" -Body $startupProfile
$startupId = $sProfileRes.id

$investorProfile = @{ investorName="E2E Investor"; organizationName="E2E Ventures"; investorType="VC_FIRM"; preferredIndustries="FinTech"; preferredStages="SEED"; minimumInvestment=25000; maximumInvestment=500000; riskPreference="MEDIUM"; investmentExperienceYears=5 } | ConvertTo-Json
Invoke-RestMethod -Uri "$base/investors" -Method Post -Headers $hInvestor -ContentType "application/json" -Body $investorProfile | Out-Null
Log-Pass "Profiles created"

# 3. AI PREDICTION (Tests Spring Boot -> FastAPI)
Log-Step "Calling AI Prediction (Spring Boot -> FastAPI integration)..."
$aiPayload = @{ founded_year=2020; team_size=10; total_users=1000; customer_count=100; monthly_revenue=10000; revenue_growth_rate=15; total_funding=500000; monthly_burn_rate=5000; runway_months=18; valuation=2000000; revenue_per_customer=100; users_per_team_member=100; growth_adjusted_runway=18; funding_to_revenue=50; burn_to_revenue=0.5; industry="FinTech"; business_model="B2B SaaS"; startup_stage="SEED" } | ConvertTo-Json
$aiRes = Invoke-RestMethod -Uri "$base/ai/predict-startup" -Method Post -Headers $hStartup -ContentType "application/json" -Body $aiPayload
if ($null -ne $aiRes.potential_score -and $null -ne $aiRes._execution.id) {
    Log-Pass "AI Prediction successful. Score: $($aiRes.potential_score). Execution ID: $($aiRes._execution.id)"
} else {
    Log-Fail "AI Prediction failed or missing execution metadata."
}

# 4. COLLABORATION & MESSAGING
Log-Step "Testing collaboration and messaging..."
$collabReq = @{ startupId=$startupId } | ConvertTo-Json
$collab = Invoke-RestMethod -Uri "$base/collaborations" -Method Post -Headers $hInvestor -ContentType "application/json" -Body $collabReq

Invoke-RestMethod -Uri "$base/collaborations/$($collab.id)/accept" -Method Patch -Headers $hStartup | Out-Null
Log-Pass "Collaboration accepted"

$conv = Invoke-RestMethod -Uri "$base/collaborations/$($collab.id)/conversation" -Method Post -Headers $hInvestor
Invoke-RestMethod -Uri "$base/conversations/$($conv.id)/messages" -Method Post -Headers $hInvestor -ContentType "application/json" -Body (@{content="Hello from E2E"} | ConvertTo-Json) | Out-Null
Log-Pass "Message sent"

# 5. ADMIN & REPORTS
Log-Step "Testing Admin actions and Reports..."
$stats = Invoke-RestMethod -Uri "$base/admin/stats" -Method Get -Headers $hAdmin
if ($stats.totalUsers -ge 3) { Log-Pass "Admin stats reflect registrations ($($stats.totalUsers) users)" }

# Test Report Export
# ADDED: -UseBasicParsing to prevent the PowerShell IE engine security prompt
$reportRes = Invoke-WebRequest -Uri "$base/reports/startup/profile?format=pdf" -Method Get -Headers $hStartup -UseBasicParsing
if ($reportRes.StatusCode -eq 200 -and $reportRes.Headers["Content-Type"] -match "application/pdf") {
    Log-Pass "PDF Report generated successfully"
} else {
    Log-Fail "PDF Report failed to download"
}

Write-Host "`n=== ALL INTEGRATION TESTS PASSED ===" -ForegroundColor Green