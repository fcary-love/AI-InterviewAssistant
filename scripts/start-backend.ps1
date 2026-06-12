$logDir = "E:\zhuomian\face_ai\logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$output = Join-Path $logDir "backend.log"
$errorLog = Join-Path $logDir "backend-error.log"

Set-Location "E:\zhuomian\face_ai\backend"
mvn spring-boot:run *> $output 2> $errorLog
