$base = Join-Path $PSScriptRoot '..\src\main\resources\static' -Resolve
$src = Join-Path $base 'SelectedDogCursor.jpg'
$dst = Join-Path $base 'SelectedDogCursor32.png'
if (-not (Test-Path $src)) { Write-Error "Source not found: $src"; exit 1 }
Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile($src)
$bmp = New-Object System.Drawing.Bitmap 32, 32
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.DrawImage($img, 0, 0, 32, 32)
$bmp.Save($dst, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$img.Dispose()
$bmp.Dispose()
Write-Host "Saved resized cursor to: $dst"
