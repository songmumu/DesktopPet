# DesktopPet GitHub Deployment Script
# Run this to push your code to GitHub

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  GitHub Deployment Wizard" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Check Git installation
Write-Host "Checking Git installation..." -ForegroundColor Yellow
$gitVersion = git --version 2>$null
if ($null -eq $gitVersion) {
    Write-Host "ERROR: Git is not installed" -ForegroundColor Red
    Write-Host "Please install Git first: winget install Git.Git" -ForegroundColor Red
    exit 1
}
Write-Host "OK: Git installed: $gitVersion" -ForegroundColor Green

# Navigate to project directory
$projectPath = $PSScriptRoot
Set-Location $projectPath
Write-Host "`nProject path: $projectPath" -ForegroundColor Cyan

# Initialize Git repository
Write-Host "`nInitializing Git repository..." -ForegroundColor Yellow
if (Test-Path ".git") {
    Write-Host "OK: Git repository already exists" -ForegroundColor Green
} else {
    git init
    git branch -M main
    Write-Host "OK: Git repository initialized" -ForegroundColor Green
}

# Ask GitHub username
Write-Host "`nEnter your GitHub username:" -ForegroundColor Yellow
$githubUsername = Read-Host "(e.g., john123)"

# Ask repository name
Write-Host "`nEnter repository name (press Enter for default):" -ForegroundColor Yellow
Write-Host "  Default: DesktopPet" -ForegroundColor Gray
$repoName = Read-Host
if ([string]::IsNullOrWhiteSpace($repoName)) {
    $repoName = "DesktopPet"
}

# Create remote URL
$remoteUrl = "https://github.com/$githubUsername/$repoName.git"

Write-Host "`nRemote URL: $remoteUrl" -ForegroundColor Cyan

# Check existing remote
$currentRemote = git remote get-url origin 2>$null
if ($currentRemote) {
    Write-Host "Current remote: $currentRemote" -ForegroundColor Yellow
    $change = Read-Host "Change remote URL? (y/n)"
    if ($change -eq "y") {
        git remote set-url origin $remoteUrl
        Write-Host "OK: Remote URL updated" -ForegroundColor Green
    }
} else {
    git remote add origin $remoteUrl
    Write-Host "OK: Remote added" -ForegroundColor Green
}

# Commit code
Write-Host "`nCommitting code..." -ForegroundColor Yellow
git add .
$commitMessage = Read-Host "Commit message (press Enter for default)"
if ([string]::IsNullOrWhiteSpace($commitMessage)) {
    $commitMessage = "Desktop Pet v1.0"
}
git commit -m $commitMessage
Write-Host "OK: Code committed" -ForegroundColor Green

# Push to GitHub
Write-Host "`nPushing to GitHub..." -ForegroundColor Yellow
Write-Host "Note: Use GitHub Personal Access Token as password" -ForegroundColor Gray
Write-Host "Get token: https://github.com/settings/tokens" -ForegroundColor Gray
Write-Host ""

git push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n=====================================" -ForegroundColor Green
    Write-Host "  SUCCESS!" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Open: https://github.com/$githubUsername/$repoName" -ForegroundColor Yellow
    Write-Host "2. Click 'Actions' to see build status" -ForegroundColor Yellow
    Write-Host "3. Wait 5-10 minutes for build" -ForegroundColor Yellow
    Write-Host "4. Download APK from 'Actions' > 'Artifacts'" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "See GITHUB_SETUP.md for details" -ForegroundColor Gray
} else {
    Write-Host "`nERROR: Push failed" -ForegroundColor Red
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "1. Wrong username/repository name" -ForegroundColor Gray
    Write-Host "2. GitHub repository not created" -ForegroundColor Gray
    Write-Host "3. Authentication failed (need Personal Access Token)" -ForegroundColor Gray
}
