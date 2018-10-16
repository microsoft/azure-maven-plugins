$response = curl http://localhost:7071/api/HttpTrigger-Java?name=CI
$success = $response.Content -eq "Hello, CI"
if (-not $success) { exit 1 }