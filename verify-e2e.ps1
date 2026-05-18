# End-to-end verification for transport-service.
# Covers every behaviour the microservice must deliver:
#   REST API, incoming consumers, published events visible in CloudAMQP.
#
# Usage:
#   Local app: .\verify-e2e.ps1 -Password "yourpass"
#   AWS app:   .\verify-e2e.ps1 -Password "yourpass" -ServiceUrl "http://your-nlb-dns:8080"

param(
    [string]$RabbitHost = $(if ($env:RABBITMQ_HOST) { $env:RABBITMQ_HOST } else { "seal.lmq.cloudamqp.com" }),
    [string]$Vhost      = $(if ($env:RABBITMQ_VIRTUAL_HOST) { $env:RABBITMQ_VIRTUAL_HOST } else { "ibvztclz" }),
    [string]$User       = $(if ($env:RABBITMQ_USERNAME) { $env:RABBITMQ_USERNAME } else { "ibvztclz" }),
    [string]$Password   = $env:RABBITMQ_PASSWORD,
    [string]$ServiceUrl = "http://localhost:8080",
    [int]$TickDelaySeconds = 1,
    [switch]$SkipPurge,
    [switch]$PurgeMapQueues
)

if (-not $Password) { Write-Error "Provide -Password or set RABBITMQ_PASSWORD"; exit 1 }

$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$passed = 0
$failed = 0

function Pass($msg) { Write-Host "  [PASS] $msg" -ForegroundColor Green;  $script:passed++ }
function Fail($msg) { Write-Host "  [FAIL] $msg" -ForegroundColor Red;    $script:failed++ }
function Step($msg) { Write-Host "`n--- $msg" -ForegroundColor Cyan }

$creds  = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${User}:${Password}"))
$rmqH   = @{ Authorization = "Basic $creds"; "Content-Type" = "application/json" }
$exchB  = "https://$RabbitHost/api/exchanges/$([Uri]::EscapeDataString($Vhost))"
$queueB = "https://$RabbitHost/api/queues/$([Uri]::EscapeDataString($Vhost))"

function Publish($exchange, $key, $payload) {
    $body = @{
        properties       = @{}
        routing_key      = $key
        payload          = ($payload | ConvertTo-Json -Compress)
        payload_encoding = "string"
    } | ConvertTo-Json -Compress
    $r = Invoke-RestMethod -Uri "$exchB/$([Uri]::EscapeDataString($exchange))/publish" -Method Post -Headers $rmqH -Body $body -ErrorAction Stop
    return $r.routed -eq $true
}

function QueueMessages($name) {
    try {
        return (Invoke-RestMethod -Uri "$queueB/$([Uri]::EscapeDataString($name))" -Headers $rmqH -ErrorAction Stop).messages
    } catch {
        return -1
    }
}

function GetTruck($id) {
    $all = Invoke-RestMethod -Uri "$ServiceUrl/trucks"
    return $all | Where-Object { $_.truckId -eq $id }
}

function WaitUntil($description, [scriptblock]$condition, [int]$TimeoutSeconds = 20, [int]$DelayMilliseconds = 500) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $result = & $condition
        if ($result) { return $result }
        Start-Sleep -Milliseconds $DelayMilliseconds
    } while ((Get-Date) -lt $deadline)
    return $null
}

function WaitForTruck($id, [scriptblock]$predicate, [int]$TimeoutSeconds = 20) {
    return WaitUntil "truck $id" {
        $truck = GetTruck $id
        if ($truck -and (& $predicate $truck)) { return $truck }
        return $null
    } $TimeoutSeconds
}

function WaitForQueueMessages($name, [int]$MinimumMessages = 1, [int]$TimeoutSeconds = 20) {
    return WaitUntil "queue $name" {
        $messages = QueueMessages $name
        if ($messages -ge $MinimumMessages) { return $messages }
        return $null
    } $TimeoutSeconds
}

function DeleteQueue($name) {
    try { Invoke-RestMethod -Uri "$queueB/$([Uri]::EscapeDataString($name))" -Method Delete -Headers $rmqH -ErrorAction Stop | Out-Null } catch {}
}

function DeclareCaptureQueue($name, $exchange, $routingKey) {
    DeleteQueue $name
    $qBody = @{ auto_delete = $false; durable = $false; arguments = @{} } | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$queueB/$([Uri]::EscapeDataString($name))" -Method Put -Headers $rmqH -Body $qBody -ErrorAction Stop | Out-Null
    $bBody = @{ routing_key = $routingKey; arguments = @{} } | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "https://$RabbitHost/api/bindings/$([Uri]::EscapeDataString($Vhost))/e/$([Uri]::EscapeDataString($exchange))/q/$([Uri]::EscapeDataString($name))" -Method Post -Headers $rmqH -Body $bBody -ErrorAction Stop | Out-Null
}

function PurgeQueue($name) {
    try {
        Invoke-RestMethod -Uri "$queueB/$([Uri]::EscapeDataString($name))/contents" -Method Delete -Headers $rmqH -ErrorAction Stop | Out-Null
        Write-Host "         Purged $name" -ForegroundColor DarkGray
    } catch {
        Write-Host "         Skipped $name (queue not found or no permission)" -ForegroundColor DarkGray
    }
}

Write-Host "`n============================================" -ForegroundColor White
Write-Host "  transport-service  end-to-end verification" -ForegroundColor White
Write-Host "============================================"  -ForegroundColor White

$runId = ([Guid]::NewGuid().ToString("N")).Substring(0, 8)
$origin   = @{ id = "e2e-warehouse-A-$runId"; x = 0; y = 0 }
$dest     = @{ id = "e2e-warehouse-B-$runId"; x = 10; y = 7 }
$distance = [Math]::Max([Math]::Abs($dest.x - $origin.x), [Math]::Abs($dest.y - $origin.y))
$truckCapacity = 100000000 + (Get-Random -Minimum 0 -Maximum 1000000)
$shipmentQuantity = $truckCapacity
$truckRegisteredQueue = "e2e.$runId.truck.registered"
$truckStatusQueue = "e2e.$runId.truck.status"
$truckPositionQueue = "e2e.$runId.truck.position"
$deliveryCompletedQueue = "e2e.$runId.delivery.completed"

if (-not $SkipPurge) {
    Step "0. CloudAMQP cleanup -- purge stale E2E messages"
    $queuesToPurge = @(
        "trucks.warehouse.registered",
        "trucks.shipment.requested",
        "trucks.time.advanced"
    )
    if ($PurgeMapQueues) {
        $queuesToPurge += @(
            "ms-map.truck-registered.q",
            "ms-map.truck-position-updated.q"
        )
    }
    foreach ($queue in $queuesToPurge) { PurgeQueue $queue }
}

# Create temp queues to capture events published by Transport.
# Deleted in the finally block below so they never linger in the broker.
DeclareCaptureQueue $truckRegisteredQueue "trucks.exchange" "truck.registered.v1"
DeclareCaptureQueue $truckStatusQueue "trucks.exchange" "truck.status.changed.v1"
DeclareCaptureQueue $truckPositionQueue "trucks.exchange" "truck.position.updated.v1"
DeclareCaptureQueue $deliveryCompletedQueue "shipments.exchange" "delivery.completed.v1"
Start-Sleep -Seconds 2

try {

# 0b - Wait for service to be ready
Step "0b. Waiting for service to be ready"
$ready = WaitUntil "service health" {
    try {
        $h = Invoke-RestMethod -Uri "$ServiceUrl/actuator/health" -ErrorAction Stop
        if ($h.status -eq "UP") { return $true }
    } catch {}
    return $null
} 60
if ($ready) { Pass "Service is UP" } else { Fail "Service did not become ready in time" }

# 1 - POST /trucks
Step "1. POST /trucks -- register truck"
$truckName = "E2E Truck $runId"
$createTruckBody = @{ name = $truckName; x = $origin.x; y = $origin.y; capacity = $truckCapacity } | ConvertTo-Json -Compress
$truck   = Invoke-RestMethod -Uri "$ServiceUrl/trucks" -Method Post -ContentType "application/json" -Body $createTruckBody
$truckId = $truck.truckId
if ($truck.status -eq "AVAILABLE" -and $truck.location.x -eq $origin.x -and $truck.location.y -eq $origin.y) {
    Pass "Truck created: id=$truckId  status=AVAILABLE  pos=($($origin.x),$($origin.y))  capacity=$truckCapacity"
} else {
    Fail "Unexpected response: $($truck | ConvertTo-Json -Compress)"
}

# 2 - GET /trucks
Step "2. GET /trucks -- truck visible in read model"
$all = Invoke-RestMethod -Uri "$ServiceUrl/trucks"
if ($all | Where-Object { $_.truckId -eq $truckId }) {
    Pass "Truck present in GET /trucks  (total trucks: $($all.Count))"
} else {
    Fail "Truck not found in GET /trucks"
}

# 3 - truck.registered.v1 published
Step "3. truck.registered.v1 -- event published to broker"
$registeredMsgs = WaitForQueueMessages $truckRegisteredQueue 1
if ($registeredMsgs -ge 1) {
    Pass "truck.registered.v1 published to trucks.exchange"
} else {
    Fail "truck.registered.v1 not captured"
}

# 4 - warehouse.registered.v1 consumed
Step "4. warehouse.registered.v1 -- LocationResolver populated"
$r1 = Publish "warehouses.exchange" "warehouse.registered.v1" @{ warehouseId = $origin.id; location = @{ x = $origin.x; y = $origin.y } }
$r2 = Publish "warehouses.exchange" "warehouse.registered.v1" @{ warehouseId = $dest.id;   location = @{ x = $dest.x;   y = $dest.y   } }
if ($r1 -and $r2) {
    Pass "Both warehouse.registered.v1 messages routed"
} else {
    Fail "One or more warehouse messages unrouted"
}
Start-Sleep -Seconds 2

# 5 - shipment.requested.v1 -> IN_TRANSIT
Step "5. shipment.requested.v1 -- truck assigned, status IN_TRANSIT"
$shipmentId = [Guid]::NewGuid().ToString()
$routed = Publish "shipments.exchange" "shipment.requested.v1" @{
    shipmentId             = $shipmentId
    originId               = $origin.id
    destinationId          = $dest.id
    originWarehouseId      = $origin.id
    destinationWarehouseId = $dest.id
    items                  = @(@{ productId = [Guid]::NewGuid().ToString(); quantity = $shipmentQuantity })
    requestedAt            = 1
}
if ($routed) {
    Pass "shipment.requested.v1 routed to consumer"
} else {
    Fail "shipment.requested.v1 unrouted"
}
$assignedTruck = WaitForTruck $truckId { param($t) $t.status -eq "IN_TRANSIT" } 30
if ($assignedTruck) {
    $distance = [Math]::Max([Math]::Abs($dest.x - $assignedTruck.location.x), [Math]::Abs($dest.y - $assignedTruck.location.y))
    Pass "Truck assigned and IN_TRANSIT: id=$truckId  pos=($($assignedTruck.location.x),$($assignedTruck.location.y))  remaining=$distance steps"
} else {
    $currentTruck = GetTruck $truckId
    if ($currentTruck) {
        Fail "Created truck did not change to IN_TRANSIT after shipment (status=$($currentTruck.status))"
    } else {
        Fail "Created truck not found after shipment"
    }
}

# 6 - truck.status.changed.v1 DISPATCHED
Step "6. truck.status.changed.v1 (DISPATCHED) -- event published"
$dispatchedMsgs = WaitForQueueMessages $truckStatusQueue 1
if ($dispatchedMsgs -ge 1) {
    Pass "truck.status.changed.v1 (DISPATCHED) published  (msgs captured: $dispatchedMsgs)"
} else {
    Fail "truck.status.changed.v1 not captured in temp queue (msgs=$dispatchedMsgs)"
}

# 7 - time.advanced.v1 -> truck moves
# currentDay starts at 2 (day 1 = shipment assignment day).
# eventId + occurredAt included to match the ms-time contract.
Step "7. time.advanced.v1 consumed -- truck moves $distance steps to ($($dest.x),$($dest.y))"
$moveFailed  = $false
$simulationDay = 1
for ($i = 1; $i -le $distance; $i++) {
    $simulationDay++
    Publish "ms-time.exchange" "time.advanced.v1" @{
        previousDay  = $simulationDay - 1
        currentDay   = $simulationDay
        daysAdvanced = 1
        eventId      = [Guid]::NewGuid().ToString()
        occurredAt   = (Get-Date -Format "o")
    } | Out-Null
    $arriving = ($i -eq $distance)
    if ($arriving) {
        $t = WaitForTruck $truckId { param($truck) $truck.status -eq "AVAILABLE" -and $truck.location.x -eq $dest.x -and $truck.location.y -eq $dest.y } 30
        if ($t.status -eq "AVAILABLE" -and $t.location.x -eq $dest.x -and $t.location.y -eq $dest.y) {
            Pass "Tick $i (day $simulationDay) -- arrived at ($($dest.x),$($dest.y)), status back to AVAILABLE"
        } else {
            $t = GetTruck $truckId
            Fail "Tick $i (day $simulationDay) -- expected AVAILABLE at ($($dest.x),$($dest.y)), got $($t.status) at ($($t.location.x),$($t.location.y))"
            $moveFailed = $true
        }
    } else {
        $t = WaitForTruck $truckId { param($truck) $truck.status -eq "IN_TRANSIT" -and ($truck.location.x -ne $origin.x -or $truck.location.y -ne $origin.y) } 30
        if ($t.status -eq "IN_TRANSIT") {
            Write-Host "         Tick $i (day $simulationDay) -- IN_TRANSIT at ($($t.location.x),$($t.location.y))" -ForegroundColor DarkGray
            if ($TickDelaySeconds -gt 0) { Start-Sleep -Seconds $TickDelaySeconds }
        } else {
            $t = GetTruck $truckId
            Fail "Tick $i (day $simulationDay) -- expected IN_TRANSIT, got $($t.status)"
            $moveFailed = $true
            break
        }
    }
}

# 8 - truck.position.updated.v1 published
Step "8. truck.position.updated.v1 -- position events published"
$positionMsgs = WaitForQueueMessages $truckPositionQueue 1
if ($positionMsgs -ge 1) {
    Pass "truck.position.updated.v1 published to trucks.exchange"
} else {
    Fail "truck.position.updated.v1 not captured"
}

# 9 - delivery.completed.v1 + RETURNED_TO_BASE
Step "9. delivery.completed.v1 + truck.status.changed.v1 (RETURNED_TO_BASE)"
$finalMsgs = WaitForQueueMessages $truckStatusQueue ($dispatchedMsgs + 1)
if ($finalMsgs -gt $dispatchedMsgs) {
    Pass "truck.status.changed.v1 (RETURNED_TO_BASE) published  (total msgs captured: $finalMsgs)"
} else {
    Fail "Expected second truck.status.changed.v1 for RETURNED_TO_BASE (after dispatch=$dispatchedMsgs after delivery=$finalMsgs)"
}
$deliveryMsgs = WaitForQueueMessages $deliveryCompletedQueue 1
if ($deliveryMsgs -ge 1) {
    Pass "delivery.completed.v1 published to shipments.exchange"
} else {
    Fail "delivery.completed.v1 not captured on shipments.exchange"
}

} finally {
    DeleteQueue $truckRegisteredQueue
    DeleteQueue $truckStatusQueue
    DeleteQueue $truckPositionQueue
    DeleteQueue $deliveryCompletedQueue
}

# Summary
$color = if ($failed -eq 0) { "Green" } else { "Red" }
Write-Host "`n============================================" -ForegroundColor $color
Write-Host ("  PASSED: {0}   FAILED: {1}" -f $passed, $failed) -ForegroundColor $color
Write-Host "============================================`n"  -ForegroundColor $color
if ($failed -gt 0) { exit 1 }
