$base = Join-Path $PSScriptRoot '..\src\main\resources\static' -Resolve
$src = Join-Path $base 'SelectedDogCursor.jpg'
$dst = Join-Path $base 'SelectedDogCursor32.png'
if (-not (Test-Path $src)) { Write-Error "Source not found: $src"; exit 1 }
Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile($src)
# Create a 32bpp ARGB bitmap explicitly (use .NET constructor syntax)
$bmp = [System.Drawing.Bitmap]::new(32, 32, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.DrawImage($img, 0, 0, 32, 32)
$g.Dispose()

# Make near-white pixels transparent. Adjust threshold if needed.
$threshold = 250
for ($x = 0; $x -lt $bmp.Width; $x++) {
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        $c = $bmp.GetPixel($x, $y)
        if ($c.R -ge $threshold -and $c.G -ge $threshold -and $c.B -ge $threshold) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, $c.R, $c.G, $c.B))
        }
    }
}

$bmp.Save($dst, [System.Drawing.Imaging.ImageFormat]::Png)
$img.Dispose()
$bmp.Dispose()
Write-Host "Saved transparent cursor to: $dst"