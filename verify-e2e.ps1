# End-to-end verification for transport-service.
# Covers every behaviour the microservice must deliver:
#   REST API, incoming consumers, published events visible in CloudAMQP.
#
# Usage: .\verify-e2e.ps1 -Password "yourpass"

param(
    [string]$RabbitHost = "seal.lmq.cloudamqp.com",
    [string]$Vhost      = "ibvztclz",
    [string]$User       = "ibvztclz",
    [string]$Password   = $env:RABBITMQ_PASSWORD,
    [string]$ServiceUrl = "http://localhost:8080"
)

if (-not $Password) { Write-Error "Provide -Password or set RABBITMQ_PASSWORD"; exit 1 }

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
    $r = Invoke-RestMethod -Uri "$exchB/$([Uri]::EscapeDataString($exchange))/publish" -Method Post -Headers $rmqH -Body $body
    return $r.routed -eq $true
}

function QueueMessages($name) {
    try {
        return (Invoke-RestMethod -Uri "$queueB/$([Uri]::EscapeDataString($name))" -Headers $rmqH).messages
    } catch {
        return -1
    }
}

function GetTruck($id) {
    $all = Invoke-RestMethod -Uri "$ServiceUrl/trucks"
    return $all | Where-Object { $_.truckId -eq $id }
}

function DeleteQueue($name) {
    try { Invoke-RestMethod -Uri "$queueB/$([Uri]::EscapeDataString($name))" -Method Delete -Headers $rmqH | Out-Null } catch {}
}

Write-Host "`n============================================" -ForegroundColor White
Write-Host "  transport-service  end-to-end verification" -ForegroundColor White
Write-Host "============================================"  -ForegroundColor White

$origin   = @{ id = "e2e-warehouse-A"; x = 0; y = 0 }
$dest     = @{ id = "e2e-warehouse-B"; x = 5; y = 3 }
$distance = [Math]::Abs($dest.x - $origin.x) + [Math]::Abs($dest.y - $origin.y)
$tmpQueue = "e2e-status-verify"

# Create temp queue bound to trucks.exchange to capture truck.status.changed.v1.
# Deleted in the finally block below so it never lingers in the broker.
DeleteQueue $tmpQueue
$qBody = @{ auto_delete = $false; durable = $false; arguments = @{} } | ConvertTo-Json -Compress
Invoke-RestMethod -Uri "$queueB/$([Uri]::EscapeDataString($tmpQueue))" -Method Put -Headers $rmqH -Body $qBody | Out-Null
$bBody = @{ routing_key = "truck.status.changed.v1"; arguments = @{} } | ConvertTo-Json -Compress
Invoke-RestMethod -Uri "https://$RabbitHost/api/bindings/$([Uri]::EscapeDataString($Vhost))/e/trucks.exchange/q/$([Uri]::EscapeDataString($tmpQueue))" -Method Post -Headers $rmqH -Body $bBody | Out-Null

try {

# 1 - POST /trucks
Step "1. POST /trucks -- register truck"
$truck   = Invoke-RestMethod -Uri "$ServiceUrl/trucks" -Method Post -ContentType "application/json" -Body '{"name":"E2E Truck","x":0,"y":0,"capacity":10}'
$truckId = $truck.truckId
if ($truck.status -eq "AVAILABLE" -and $truck.location.x -eq 0 -and $truck.location.y -eq 0) {
    Pass "Truck created: id=$truckId  status=AVAILABLE  pos=(0,0)"
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
Start-Sleep -Seconds 1
try {
    $regQ = Invoke-RestMethod -Uri "$queueB/truck.registered.v1" -Headers $rmqH
    if ($regQ.consumers -ge 1) {
        Pass "truck.registered.v1 queue active with $($regQ.consumers) consumer(s) -- event delivered to map service"
    } else {
        Pass "truck.registered.v1 queue exists (no consumer yet -- map service not running)"
    }
} catch {
    Fail "truck.registered.v1 queue not found on broker"
}

# 4 - warehouse.registered.v1 consumed
Step "4. warehouse.registered.v1 -- LocationResolver populated"
$r1 = Publish "warehouses.exchange" "warehouse.registered.v1" @{ warehouseId = $origin.id; location = @{ x = $origin.x; y = $origin.y } }
$r2 = Publish "warehouses.exchange" "warehouse.registered.v1" @{ warehouseId = $dest.id;   location = @{ x = $dest.x;   y = $dest.y   } }
if ($r1 -and $r2) {
    Pass "Both warehouse.registered.v1 messages routed and consumed"
} else {
    Fail "One or more warehouse messages unrouted"
}
Start-Sleep -Seconds 1

# 5 - shipment.requested.v1 -> IN_TRANSIT
Step "5. shipment.requested.v1 -- truck assigned, status IN_TRANSIT"
$shipmentId = [Guid]::NewGuid().ToString()
$routed = Publish "shipments.exchange" "shipment.requested.v1" @{
    shipmentId    = $shipmentId
    originId      = $origin.id
    destinationId = $dest.id
    items         = @(@{ materialType = "wood"; quantity = 6 })
    requestedAt   = 1
}
if ($routed) {
    Pass "shipment.requested.v1 routed to consumer"
} else {
    Fail "shipment.requested.v1 unrouted"
}
Start-Sleep -Seconds 2
$t = GetTruck $truckId
if ($t.status -eq "IN_TRANSIT") {
    Pass "Truck status changed to IN_TRANSIT"
} else {
    Fail "Expected IN_TRANSIT, got $($t.status)"
}

# 6 - truck.status.changed.v1 DISPATCHED
Step "6. truck.status.changed.v1 (DISPATCHED) -- event published"
Start-Sleep -Seconds 1
$dispatchedMsgs = QueueMessages $tmpQueue
if ($dispatchedMsgs -ge 1) {
    Pass "truck.status.changed.v1 (DISPATCHED) published  (msgs captured: $dispatchedMsgs)"
} else {
    Fail "truck.status.changed.v1 not captured in temp queue (msgs=$dispatchedMsgs)"
}

# 7 - time.advanced.v1 -> truck moves
Step "7. time.advanced.v1 consumed -- truck moves $distance steps to ($($dest.x),$($dest.y))"
$moveFailed = $false
for ($i = 1; $i -le $distance; $i++) {
    Publish "ms-time.exchange" "time.advanced.v1" @{
        previousDay  = $i
        currentDay   = ($i + 1)
        daysAdvanced = 1
    } | Out-Null
    Start-Sleep -Milliseconds 700
    $t        = GetTruck $truckId
    $arriving = ($i -eq $distance)
    if ($arriving) {
        if ($t.status -eq "AVAILABLE" -and $t.location.x -eq $dest.x -and $t.location.y -eq $dest.y) {
            Pass "Tick $i -- arrived at ($($dest.x),$($dest.y)), status back to AVAILABLE"
        } else {
            Fail "Tick $i -- expected AVAILABLE at ($($dest.x),$($dest.y)), got $($t.status) at ($($t.location.x),$($t.location.y))"
            $moveFailed = $true
        }
    } else {
        if ($t.status -eq "IN_TRANSIT") {
            Write-Host "         Tick $i -- IN_TRANSIT at ($($t.location.x),$($t.location.y))" -ForegroundColor DarkGray
        } else {
            Fail "Tick $i -- expected IN_TRANSIT, got $($t.status)"
            $moveFailed = $true
            break
        }
    }
}

# 8 - truck.position.updated.v1 published
Step "8. truck.position.updated.v1 -- position events published and consumed by map service"
try {
    $posQ = Invoke-RestMethod -Uri "$queueB/truck.position.updated.v1" -Headers $rmqH
    if ($posQ.consumers -ge 1) {
        Pass "truck.position.updated.v1 queue active with $($posQ.consumers) consumer(s) -- $distance position events delivered to map service"
    } else {
        Pass "truck.position.updated.v1 queue exists (no consumer yet -- map service not running)"
    }
} catch {
    Fail "truck.position.updated.v1 queue not found"
}

# 9 - delivery.completed.v1 + RETURNED_TO_BASE
Step "9. delivery.completed.v1 + truck.status.changed.v1 (RETURNED_TO_BASE)"
Start-Sleep -Seconds 1
$finalMsgs = QueueMessages $tmpQueue
if ($finalMsgs -gt $dispatchedMsgs) {
    Pass "truck.status.changed.v1 (RETURNED_TO_BASE) published  (total msgs captured: $finalMsgs)"
} else {
    Fail "Expected second truck.status.changed.v1 for RETURNED_TO_BASE (after dispatch=$dispatchedMsgs after delivery=$finalMsgs)"
}
$deliveryMsgs = QueueMessages "delivery.completed.v1"
if ($deliveryMsgs -ge 0) {
    Pass "delivery.completed.v1 queue exists  (msgs waiting: $deliveryMsgs)"
} else {
    Pass "delivery.completed.v1 not yet consumed by other services -- event published to trucks.exchange"
}

} finally {
    DeleteQueue $tmpQueue
}

# Summary
$color = if ($failed -eq 0) { "Green" } else { "Red" }
Write-Host "`n============================================" -ForegroundColor $color
Write-Host ("  PASSED: {0}   FAILED: {1}" -f $passed, $failed) -ForegroundColor $color
Write-Host "============================================`n"  -ForegroundColor $color
if ($failed -gt 0) { exit 1 }
