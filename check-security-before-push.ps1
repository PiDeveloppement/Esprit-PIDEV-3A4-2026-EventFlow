# ✅ Script de vérification de sécurité avant push
# À exécuter avant chaque git push

Write-Host "`n🔐 VÉRIFICATION DE SÉCURITÉ GIT`n" -ForegroundColor Cyan

# 1. Vérifier .env n'est pas tracké
Write-Host "1️⃣ Vérification que .env n'est pas dans Git..."
if (git ls-files --error-unmatch ".env" 2>$null) {
    Write-Host "   ❌ .env EST tracké par Git ! ARRÊTE TON PUSH !" -ForegroundColor Red
    exit 1
} else {
    Write-Host "   ✅ .env N'est pas tracké (bon)" -ForegroundColor Green
}

# 2. Vérifier workspace.xml
Write-Host "`n2️⃣ Vérification du workspace.xml..."
if (git ls-files --error-unmatch ".idea/workspace.xml" 2>$null) {
    Write-Host "   ⚠️  workspace.xml EST tracké (devrait être ignoré)" -ForegroundColor Yellow
} else {
    Write-Host "   ✅ workspace.xml N'est pas tracké (bon)" -ForegroundColor Green
}

# 3. Vérifier s'il y a des tokens dans les fichiers à committer
Write-Host "`n3️⃣ Recherche de tokens exposés..."
$hasTokens = $false
$stagedFiles = git diff --cached --name-only
foreach ($file in $stagedFiles) {
    if (git diff --cached $file | Select-String "hf_[A-Za-z0-9]{20,}" -Quiet) {
        Write-Host "   ❌ Token trouvé dans: $file" -ForegroundColor Red
        $hasTokens = $true
    }
}
if (-not $hasTokens) {
    Write-Host "   ✅ Aucun token HF trouvé dans les fichiers à committer" -ForegroundColor Green
}

# 4. Vérifier la variable d'environnement HF_TOKEN
Write-Host "`n4️⃣ Vérification de la variable HF_TOKEN..."
$hfToken = [Environment]::GetEnvironmentVariable("HF_TOKEN", "User")
if ($hfToken) {
    Write-Host "   ✅ HF_TOKEN existe dans les variables système" -ForegroundColor Green
    Write-Host "   💡 Valeur: $($hfToken.Substring(0,10))***" -ForegroundColor Gray
} else {
    Write-Host "   ⚠️  HF_TOKEN N'existe pas dans les variables système" -ForegroundColor Yellow
    Write-Host "   💡 À configurer: [Environment]::SetEnvironmentVariable('HF_TOKEN', 'ta_valeur', 'User')" -ForegroundColor Gray
}

Write-Host "`n" -ForegroundColor Cyan
if ($hasTokens) {
    Write-Host "❌ Ne fais PAS le push - tokens exposés détectés!" -ForegroundColor Red
    exit 1
} else {
    Write-Host "✅ Ok pour faire le push en toute sécurité" -ForegroundColor Green
    Write-Host "(Mais fais 'git status' pour confirmer)" -ForegroundColor Gray
}

