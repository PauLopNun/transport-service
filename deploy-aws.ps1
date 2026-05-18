param(
    [string]$Region = "eu-west-1",
    [string]$AccountId = "822414985516",
    [string]$RepositoryName = "transport-service",
    [string]$ImageTag = "latest",
    [string]$Cluster = "junior-workshop-2026-vlc",
    [string]$Service = "transport-service",
    [string]$ServiceUrl = "http://taller-deploy-aws-2026-nlb-pulz-adee7212e878911f.elb.eu-west-1.amazonaws.com:8080",
    [switch]$NoWait
)

$ErrorActionPreference = "Stop"

function Resolve-CommandPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [string[]]$FallbackPaths = @()
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    foreach ($path in $FallbackPaths) {
        if (Test-Path $path) {
            return $path
        }
    }

    return $null
}

function Invoke-Native {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Step,
        [Parameter(Mandatory = $true)]
        [scriptblock]$Command
    )

    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Step failed with exit code $LASTEXITCODE."
    }
}

$image = "${AccountId}.dkr.ecr.${Region}.amazonaws.com/${RepositoryName}:${ImageTag}"
$aws = Resolve-CommandPath -Name "aws" -FallbackPaths @(
    (Join-Path $env:LOCALAPPDATA "Programs\AWSCLI\Amazon\AWSCLIV2\aws.exe"),
    "C:\Program Files\Amazon\AWSCLIV2\aws.exe"
)
$docker = Resolve-CommandPath -Name "docker"

Write-Host "==> Checking local deployment tools" -ForegroundColor Cyan
if (-not $aws) {
    throw "aws was not found. Install AWS CLI v2 from https://aws.amazon.com/cli/ and run 'aws configure sso' or configure credentials for account $AccountId. Restart IntelliJ or your terminal after updating PATH."
}
if (-not $docker) {
    throw "docker was not found on PATH. Install Docker Desktop and make sure the Docker engine is running. Restart IntelliJ or your terminal after updating PATH."
}

Write-Host "==> Checking AWS identity" -ForegroundColor Cyan
Invoke-Native -Step "AWS identity check" -Command {
    & $aws sts get-caller-identity --region $Region | Out-Host
}

Write-Host "==> Logging in to ECR: $image" -ForegroundColor Cyan
$ecrPassword = & $aws ecr get-login-password --region $Region
if ($LASTEXITCODE -ne 0) {
    throw "ECR login password request failed with exit code $LASTEXITCODE."
}
$ecrPassword | & $docker login --username AWS --password-stdin "${AccountId}.dkr.ecr.${Region}.amazonaws.com"
if ($LASTEXITCODE -ne 0) {
    throw "Docker login to ECR failed with exit code $LASTEXITCODE."
}

Write-Host "==> Building Docker image" -ForegroundColor Cyan
Invoke-Native -Step "Docker image build" -Command {
    & $docker build -t "${RepositoryName}:${ImageTag}" .
}

Write-Host "==> Tagging Docker image" -ForegroundColor Cyan
Invoke-Native -Step "Docker image tag" -Command {
    & $docker tag "${RepositoryName}:${ImageTag}" $image
}

Write-Host "==> Pushing Docker image" -ForegroundColor Cyan
Invoke-Native -Step "Docker image push" -Command {
    & $docker push $image
}

Write-Host "==> Forcing ECS deployment" -ForegroundColor Cyan
Invoke-Native -Step "ECS deployment update" -Command {
    & $aws ecs update-service `
        --region $Region `
        --cluster $Cluster `
        --service $Service `
        --force-new-deployment | Out-Host
}

if (-not $NoWait) {
    Write-Host "==> Waiting for ECS service to become stable" -ForegroundColor Cyan
    Invoke-Native -Step "ECS service stability wait" -Command {
        & $aws ecs wait services-stable `
            --region $Region `
            --cluster $Cluster `
            --services $Service
    }
}

Write-Host "==> Deployment requested" -ForegroundColor Green
Write-Host "Swagger: $ServiceUrl/swagger-ui/index.html"
Write-Host "Trucks:  $ServiceUrl/trucks"

try {
    $docs = Invoke-RestMethod -Uri "$ServiceUrl/v3/api-docs" -TimeoutSec 20
    Write-Host "OpenAPI servers:" -ForegroundColor Cyan
    $docs.servers | Format-Table -AutoSize | Out-Host
} catch {
    Write-Host "Could not read $ServiceUrl/v3/api-docs yet: $($_.Exception.Message)" -ForegroundColor Yellow
}
