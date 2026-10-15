# gen_resources.ps1 - placeholder resources for floral_rite_bloom (ASCII only)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = 'd:\Mods_create\mod_1yuwb1l\src\main\resources\assets\floral_rite_bloom'
$dir = 'd:\Mods_create\mod_1yuwb1l\src\main\resources\data\floral_rite_bloom'
$bs = Join-Path $root 'blockstates'
$mb = Join-Path $root 'models\block'
$mi = Join-Path $root 'models\item'
$tb = Join-Path $root 'textures\block'
$loot = Join-Path $dir 'loot_tables\blocks'
foreach ($p in @($bs,$mb,$mi,$tb,$loot)) { New-Item -ItemType Directory -Force -Path $p | Out-Null }

$types = @('ice','flamm','void','vitality','wither','sky','current')

# ---- blockstates for 7 attribute logs ----
foreach ($t in $types) {
  $json = @"
{
  "variants": {
    "axis=x": { "model": "floral_rite_bloom:block/rune_log_$t", "x": 90, "y": 90 },
    "axis=y": { "model": "floral_rite_bloom:block/rune_log_$t" },
    "axis=z": { "model": "floral_rite_bloom:block/rune_log_$t", "x": 90 }
  }
}
"@
  Set-Content -Path (Join-Path $bs "rune_log_$t.json") -Value $json -Encoding ascii
}

# ---- block models (cube_column with side+end) ----
$allLogs = @('rune_log') + ($types | ForEach-Object { "rune_log_$_" })
foreach ($n in $allLogs) {
  $json = @"
{
  "parent": "minecraft:block/cube_column",
  "textures": {
    "side": "floral_rite_bloom:block/$n`_side",
    "end": "floral_rite_bloom:block/$n`_end"
  }
}
"@
  Set-Content -Path (Join-Path $mb "$n.json") -Value $json -Encoding ascii
  # item model references block model
  $ij = @"
{
  "parent": "floral_rite_bloom:block/$n"
}
"@
  Set-Content -Path (Join-Path $mi "$n.json") -Value $ij -Encoding ascii
  # loot table drops itself
  $lj = @"
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [ { "type": "minecraft:item", "name": "floral_rite_bloom:$n" } ],
      "conditions": [ { "condition": "minecraft:survives_explosion" } ]
    }
  ]
}
"@
  Set-Content -Path (Join-Path $loot "$n.json") -Value $lj -Encoding ascii
}

# ---- altar blockstate/model/itemmodel/loot ----
$altarBs = @"
{
  "variants": {
    "": { "model": "floral_rite_bloom:block/floral_altar" }
  }
}
"@
Set-Content -Path (Join-Path $bs 'floral_altar.json') -Value $altarBs -Encoding ascii
$altarM = @"
{
  "parent": "minecraft:block/cross",
  "render_type": "minecraft:cutout",
  "textures": {
    "cross": "floral_rite_bloom:block/floral_altar"
  }
}
"@
Set-Content -Path (Join-Path $mb 'floral_altar.json') -Value $altarM -Encoding ascii
$altarI = @"
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "floral_rite_bloom:item/floral_altar"
  }
}
"@
Set-Content -Path (Join-Path $mi 'floral_altar.json') -Value $altarI -Encoding ascii
$altarL = @"
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [ { "type": "minecraft:item", "name": "floral_rite_bloom:floral_altar" } ],
      "conditions": [ { "condition": "minecraft:survives_explosion" } ]
    }
  ]
}
"@
Set-Content -Path (Join-Path $loot 'floral_altar.json') -Value $altarL -Encoding ascii

# ---- textures ----
function New-Tex { param([int]$w,[int]$h) $bmp = New-Object System.Drawing.Bitmap($w,$h,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb); return $bmp }
function Set-Px { param($bmp,[int]$x,[int]$y,[int]$r,[int]$g,[int]$b,[int]$a=255) $bmp.SetPixel($x,$y,[System.Drawing.Color]::FromArgb($a,$r,$g,$b)) }
function Save-Tex { param($bmp,$path) $bmp.Save($path,[System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose() }

# colors per log type: side base, side dark, rune bright, end base
$colors = @{
  'rune_log'   = @(@(139,105,60),@(105,75,42),@(190,160,220),@(160,120,70))
  'rune_log_ice'     = @(@(70,130,160),@(50,100,130),@(170,235,255),@(110,170,200))
  'rune_log_flamm'   = @(@(120,45,35),@(90,30,25),@(255,140,60),@(150,60,45))
  'rune_log_void'    = @(@(60,35,95),@(40,22,65),@(180,110,255),@(85,50,125))
  'rune_log_vitality'= @(@(70,125,55),@(48,95,40),@(160,255,140),@(100,150,75))
  'rune_log_wither'  = @(@(105,95,85),@(80,72,64),@(190,175,150),@(130,118,105))
  'rune_log_sky'     = @(@(120,175,215),@(95,145,185),@(235,250,255),@(155,200,230))
  'rune_log_current' = @(@(45,90,150),@(30,65,115),@(120,215,255),@(65,115,175))
}

$rnd = New-Object System.Random(20260916)
foreach ($n in $allLogs) {
  $c = $colors[$n]
  # side 16x16: bark stripes + vertical rune glyph in center
  $bmp = New-Tex 16 16
  for ($x=0; $x -lt 16; $x++) {
    $dark = ($x % 4 -eq 1) -or ($rnd.Next(6) -eq 0)
    for ($y=0; $y -lt 16; $y++) {
      $col = if ($dark) { $c[1] } else { $c[0] }
      Set-Px $bmp $x $y $col[0] $col[1] $col[2]
    }
  }
  # rune glyph: simple symmetric pattern column 7-8 plus branches
  $rb = $c[2]
  foreach ($pt in @(@(7,2),@(8,2),@(7,4),@(8,4),@(6,5),@(9,5),@(7,6),@(8,6),@(7,8),@(8,8),@(6,9),@(9,9),@(7,10),@(8,10),@(7,12),@(8,12),@(5,13),@(10,13))) {
    Set-Px $bmp $pt[0] $pt[1] $rb[0] $rb[1] $rb[2]
  }
  Save-Tex $bmp (Join-Path $tb "$n`_side.png")
  # end 16x16: rings
  $bmp2 = New-Tex 16 16
  for ($x=0; $x -lt 16; $x++) { for ($y=0; $y -lt 16; $y++) {
    $dx = $x - 7.5; $dy = $y - 7.5
    $d = [math]::Sqrt($dx*$dx + $dy*$dy)
    $ring = ([math]::Floor($d) % 3) -eq 1
    $col = if ($ring) { $c[1] } else { $c[3] }
    Set-Px $bmp2 $x $y $col[0] $col[1] $col[2]
  } }
  Save-Tex $bmp2 (Join-Path $tb "$n`_end.png")
}

# altar flower texture 16x16 cross: transparent bg, stem bottom, bloom top
$bmp = New-Tex 16 16
for ($i=0; $i -lt 256; $i++) { Set-Px $bmp ($i % 16) ([int][math]::Floor($i/16)) 0 0 0 0 }
# stem
for ($y=7; $y -lt 16; $y++) { Set-Px $bmp 7 $y 60 130 55; Set-Px $bmp 8 $y 75 150 65 }
# leaves
Set-Px $bmp 6 11 70 150 60; Set-Px $bmp 5 10 70 150 60; Set-Px $bmp 9 13 70 150 60; Set-Px $bmp 10 12 70 150 60
# bloom (upward-facing cup of petals)
$petal = @(230,110,180); $petalD = @(200,80,150); $core = @(255,220,120)
foreach ($pt in @(@(4,4),@(5,3),@(6,2),@(7,2),@(8,2),@(9,2),@(10,3),@(11,4),@(4,5),@(11,5),@(5,5),@(10,5),@(6,4),@(9,4))) { Set-Px $bmp $pt[0] $pt[1] $petal[0] $petal[1] $petal[2] }
foreach ($pt in @(@(6,5),@(9,5),@(7,5),@(8,5),@(5,4),@(10,4))) { Set-Px $bmp $pt[0] $pt[1] $petalD[0] $petalD[1] $petalD[2] }
foreach ($pt in @(@(7,3),@(8,3),@(7,4),@(8,4))) { Set-Px $bmp $pt[0] $pt[1] $core[0] $core[1] $core[2] }
Save-Tex $bmp (Join-Path $tb 'floral_altar.png')
# item copy of altar texture
New-Item -ItemType Directory -Force -Path (Join-Path $root 'textures\item') | Out-Null
Copy-Item (Join-Path $tb 'floral_altar.png') (Join-Path $root 'textures\item\floral_altar.png') -Force

Write-Output 'RESOURCES_DONE'
