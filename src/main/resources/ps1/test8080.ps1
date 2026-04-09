$ip = "192.168.1.14"
$port = 8080
$url = "http://${ip}:${port}/"

try {
    $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
    Write-Host "✅ 成功访问 $url"
    Write-Host "状态码: $($response.StatusCode)"
    Write-Host "返回内容前100字符: $($response.Content.Substring(0,100))..."
} catch {
    Write-Host "❌ 无法访问 $url"
    Write-Host "错误信息: $($_.Exception.Message)"
}
