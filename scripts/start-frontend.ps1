$logDir = "E:\zhuomian\face_ai\logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$output = Join-Path $logDir "frontend.log"
$errorLog = Join-Path $logDir "frontend-error.log"

Set-Location "E:\zhuomian\face_ai\frontend"
npm run dev *> $output 2> $errorLog
