# Check ESP32-S3 HID devices
Write-Host "=== Checking ESP32-S3 HID Devices ===" -ForegroundColor Cyan

Write-Host "`n1. All ESP32-S3 related devices (VID_303A):" -ForegroundColor Yellow
Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"} | Format-Table FriendlyName, Status, DeviceID, Class

Write-Host "`n2. HID Class devices with VID_303A:" -ForegroundColor Yellow
Get-PnpDevice | Where-Object {$_.Class -eq 'HIDClass' -and $_.DeviceID -match 'VID_303A'} | Format-Table FriendlyName, Status, DeviceID

Write-Host "`n3. Mouse devices with VID_303A:" -ForegroundColor Yellow
Get-PnpDevice | Where-Object {$_.Class -eq 'Mouse' -and $_.DeviceID -match 'VID_303A'} | Format-Table FriendlyName, Status, DeviceID

Write-Host "`n4. Keyboard devices with VID_303A:" -ForegroundColor Yellow
Get-PnpDevice | Where-Object {$_.Class -eq 'Keyboard' -and $_.DeviceID -match 'VID_303A'} | Format-Table FriendlyName, Status, DeviceID

Write-Host "`n5. USB Composite Device details:" -ForegroundColor Yellow
Get-PnpDevice | Where-Object {$_.FriendlyName -like "*USB Composite Device*" -and $_.DeviceID -match 'VID_303A'} | ForEach-Object {
    Write-Host "  Device: $($_.FriendlyName) - $($_.Status)" -ForegroundColor White
    Write-Host "  DeviceID: $($_.DeviceID)" -ForegroundColor Gray
}
