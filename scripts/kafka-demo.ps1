# Publica un evento de prueba sin abrir un productor interactivo.
# Ejecución desde la raíz: powershell -ExecutionPolicy Bypass -File .\scripts\kafka-demo.ps1
$ErrorActionPreference = "Stop"
$container = "pedidos360-kafka-1-1"
$broker = "kafka-1:9092"
$topic = "pedidos.eventos"

$id = "PED-DEMO-" + [guid]::NewGuid().ToString("N").Substring(0, 8).ToUpperInvariant()
$payload = [ordered]@{
    schemaVersion = 1
    eventType = "PEDIDO_PRUEBA"
    pedidoId = $id
    origen = "consola"
    estado = "CREADO"
} | ConvertTo-Json -Compress

# La barra vertical separa la Key del Value JSON.
$record = "$id|$payload"
$record | docker exec -i $container kafka-console-producer `
    --bootstrap-server $broker `
    --topic $topic `
    --property "parse.key=true" `
    --property "key.separator=|"

if ($LASTEXITCODE -ne 0) {
    throw "El productor Kafka devolvió el código $LASTEXITCODE."
}
Write-Host "Evento enviado a $topic con Key: $id"
Write-Host "Abre Kafka UI > Topics > pedidos.eventos > Messages y consulta el evento reciente."
