# Script to get ngrok URL
try {
    $response = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels" -Method Get
    if ($response.tunnels -and $response.tunnels.Count -gt 0) {
        $httpsUrl = $response.tunnels | Where-Object { $_.proto -eq "https" } | Select-Object -First 1
        if ($httpsUrl) {
            Write-Host "Ngrok HTTPS URL: $($httpsUrl.public_url)"
        } else {
            $httpUrl = $response.tunnels | Where-Object { $_.proto -eq "http" } | Select-Object -First 1
            if ($httpUrl) {
                Write-Host "Ngrok HTTP URL: $($httpUrl.public_url)"
            }
        }
    } else {
        Write-Host "No tunnels found"
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)"
}

