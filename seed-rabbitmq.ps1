# Seed RabbitMQ with test data to simulate the full dispatch flow.
# Usage: .\seed-rabbitmq.ps1 -Password "pass" [-OriginX 0] [-OriginY 0] [-DestX 5] [-DestY 3] [-Quantity 6] [-Days 1]
# Use -Days to advance time N ticks after assigning the shipment (8 ticks reaches (5,3) from (0,0)).

param(
    [string]$RabbitHost = "seal.lmq.cloudamqp.com",
    [string]$Vhost      = "ibvztclz",
    [string]$User       = "ibvztclz",
    [string]$Password   = $env:RABBITMQ_PASSWORD,
    [int]$OriginX   = 0,
    [int]$OriginY   = 0,
    [int]$DestX     = 5,
    [int]$DestY     = 3,
    [int]$Quantity  = 6,
    [int]$StartDay  = 1,
    [int]$Days      = 1
)

if (-not $Password) {
    Write-Error "Provide -Password or set env var RABBITMQ_PASSWORD"
    exit 1
}

$base       = "https://$RabbitHost/api/exchanges/$([Uri]::EscapeDataString($Vhost))"
$creds      = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${User}:${Password}"))
$headers    = @{ Authorization = "Basic $creds"; "Content-Type" = "application/json" }
$shipmentId = [Guid]::NewGuid().ToString()

function Publish($exchange, $routingKey, $payload) {
    $body = @{
        properties       = @{}
        routing_key      = $routingKey
        payload          = ($payload | ConvertTo-Json -Compress)
        payload_encoding = "string"
    } | ConvertTo-Json -Compress

    $url = "$base/$([Uri]::EscapeDataString($exchange))/publish"
    $r   = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body
    if ($r.routed -eq $true) {
        Write-Host "  [OK] $routingKey -> $exchange"
    } else {
        Write-Warning "  [UNROUTED] $routingKey -> $exchange (queue not bound?)"
    }
}

Write-Host "`n=== 1. Register warehouses ==="
Publish "warehouses.exchange" "warehouse.registered.v1" @{
    warehouseId = "warehouse-north-01"
    location    = @{ x = $OriginX; y = $OriginY }
}
Publish "warehouses.exchange" "warehouse.registered.v1" @{
    warehouseId = "warehouse-south-03"
    location    = @{ x = $DestX; y = $DestY }
}

Write-Host "`n=== 2. Request shipment ==="
Publish "shipments.exchange" "shipment.requested.v1" @{
    shipmentId    = $shipmentId
    originId      = "warehouse-north-01"
    destinationId = "warehouse-south-03"
    items         = @(@{ materialType = "wood"; quantity = $Quantity })
    requestedAt   = $StartDay
}

Write-Host "`n=== 3. Advance time ($Days tick(s)) ==="
for ($i = 0; $i -lt $Days; $i++) {
    $current = $StartDay + $i + 1
    Publish "simulation.events" "time.advanced.v1" @{
        previousDay  = $current - 1
        currentDay   = $current
        daysAdvanced = 1
    }
}

$distance = [Math]::Abs($DestX - $OriginX) + [Math]::Abs($DestY - $OriginY)
Write-Host "`nDone - Manhattan distance to destination: $distance steps."
Write-Host "Run with -Days $distance to complete the full delivery."
