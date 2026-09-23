# PowerShell Script to setup PW DHYAN logo across Android App and Web Admin Console
$sourceImage = "c:\Users\dhyan\Downloads\Images\ChatGPT Image Sep 23, 2026, 02_16_04 PM.png"

if (-not (Test-Path $sourceImage)) {
    Write-Host "Searching for latest logo image in Downloads\Images..." -ForegroundColor Yellow
    $fallback = Get-ChildItem "c:\Users\dhyan\Downloads\Images\*.png" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($fallback) {
        $sourceImage = $fallback.FullName
    } else {
        Write-Error "Could not locate logo image at $sourceImage"
        exit 1
    }
}

Write-Host "Found logo source: $sourceImage" -ForegroundColor Cyan

# 1. Copy to Drawable and Web Admin Console
$drawableTarget = "d:\Dhyan\websites\pw-dhyan\app\src\main\res\drawable\app_logo.png"
$adminTarget = "d:\Dhyan\websites\pw-dhyan\admin-manage\logo.png"

Copy-Item -Path $sourceImage -Destination $drawableTarget -Force
Copy-Item -Path $sourceImage -Destination $adminTarget -Force
Write-Host "Copied logo to drawable/app_logo.png and admin-manage/logo.png" -ForegroundColor Green

# 2. Resize to Android mipmap launcher densities
Add-Type -AssemblyName System.Drawing

function Resize-Image($srcPath, $destPath, $width, $height) {
    $srcImg = [System.Drawing.Image]::FromFile($srcPath)
    $destBitmap = New-Object System.Drawing.Bitmap($width, $height)
    $graphics = [System.Drawing.Graphics]::FromImage($destBitmap)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $graphics.DrawImage($srcImg, 0, 0, $width, $height)
    
    $destDir = [System.IO.Path]::GetDirectoryName($destPath)
    if (-not (Test-Path $destDir)) {
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
    }
    
    # Save as PNG
    $destBitmap.Save($destPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $destBitmap.Dispose()
    $srcImg.Dispose()
}

$densities = @(
    @{ Name = "mdpi"; Size = 48 },
    @{ Name = "hdpi"; Size = 72 },
    @{ Name = "xhdpi"; Size = 96 },
    @{ Name = "xxhdpi"; Size = 144 },
    @{ Name = "xxxhdpi"; Size = 192 }
)

foreach ($d in $densities) {
    $targetDir = "d:\Dhyan\websites\pw-dhyan\app\src\main\res\mipmap-$($d.Name)"
    $iconPath = Join-Path $targetDir "ic_launcher.png"
    $roundPath = Join-Path $targetDir "ic_launcher_round.png"
    
    # Remove old webp if exists so png takes priority
    $oldWebp = Join-Path $targetDir "ic_launcher.webp"
    $oldRoundWebp = Join-Path $targetDir "ic_launcher_round.webp"
    if (Test-Path $oldWebp) { Remove-Item $oldWebp -Force }
    if (Test-Path $oldRoundWebp) { Remove-Item $oldRoundWebp -Force }
    
    Resize-Image $sourceImage $iconPath $d.Size $d.Size
    Resize-Image $sourceImage $roundPath $d.Size $d.Size
    Write-Host "Generated mipmap-$($d.Name) icons ($($d.Size)x$($d.Size))" -ForegroundColor Green
}

Write-Host "`nAll PW DHYAN logos and launcher icons successfully generated!" -ForegroundColor Cyan
