# Rasmal backend - one-shot deploy.
# ---------------------------------------------------------------------------
# Prereqs (already installed on this machine): Deno + Supabase CLI.
# Run ONCE interactively:   supabase login   (opens a browser for your token)
# Then:                     .\supabase\deploy.ps1 -ProjectRef <your-project-ref>
#
# Your three API keys are entered at runtime (Read-Host) and are NEVER written
# to disk or committed. Paste the same keys you already have.
# ---------------------------------------------------------------------------
param(
  [Parameter(Mandatory = $true)] [string] $ProjectRef,
  [switch] $SkipSecrets   # pass this on re-deploys once secrets are already set
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent   # repo root (this file lives in supabase/)
Set-Location $root

function Step($msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }

# 0. Sanity: is the CLI logged in?
Step 'Checking Supabase login'
supabase projects list *> $null
if ($LASTEXITCODE -ne 0) {
  Write-Host "Not logged in. Run 'supabase login' first, then re-run this script." -ForegroundColor Red
  exit 1
}

# 1. Link this repo to the project
Step "Linking project $ProjectRef"
supabase link --project-ref $ProjectRef
if ($LASTEXITCODE -ne 0) { throw 'supabase link failed' }

# 2. Secrets (typed at runtime, never stored)
if (-not $SkipSecrets) {
  Step 'Setting Edge Function secrets'
  $sahmk      = Read-Host 'SAHMK_KEY (shmk_live_...)'
  $marketaux  = Read-Host 'MARKETAUX_KEY'
  $openrouter = Read-Host 'OPENROUTER_KEY (sk-or-v1-...)'
  $cron       = -join ((1..32) | ForEach-Object { '{0:x}' -f (Get-Random -Maximum 16) })

  $secretArgs = @(
    "SAHMK_KEY=$sahmk",
    "MARKETAUX_KEY=$marketaux",
    "OPENROUTER_KEY=$openrouter",
    'OPENROUTER_MODEL=nvidia/nemotron-3-super-120b-a12b:free',
    "CRON_SECRET=$cron"
  )
  supabase secrets set @secretArgs
  if ($LASTEXITCODE -ne 0) { throw 'supabase secrets set failed' }

  Write-Host "`nGENERATED CRON_SECRET (save this - you need it for the cron SQL below):" -ForegroundColor Yellow
  Write-Host "  $cron" -ForegroundColor Yellow
}

# 3. Schema + seed
Step 'Pushing migrations (schema + seed)'
supabase db push
if ($LASTEXITCODE -ne 0) { throw 'supabase db push failed' }

# 4. Deploy the three functions
foreach ($fn in 'market-refresh', 'recommendations', 'chat') {
  Step "Deploying function: $fn"
  supabase functions deploy $fn
  if ($LASTEXITCODE -ne 0) { throw "deploy $fn failed" }
}

# 5. Next steps (manual, one-time)
Step 'DONE - remaining manual steps'
$next = @'
1) Schedule the cache refresh - run in the Supabase SQL editor (use the CRON_SECRET printed above):

   create extension if not exists pg_cron;
   create extension if not exists pg_net;
   select cron.schedule('rasmal-market-refresh', '0 4,9,13 * * *', $$
     select net.http_post(
       url     := 'https://<REF>.functions.supabase.co/market-refresh',
       headers := jsonb_build_object('x-cron-secret', 'YOUR_CRON_SECRET'),
       body    := '{}'::jsonb
     );
   $$);

2) Kick off a first cache fill now:
   curl -X POST https://<REF>.functions.supabase.co/market-refresh -H "x-cron-secret: YOUR_CRON_SECRET"

3) Replace the PLACEHOLDER financial_statements + fundamentals in migrations/0002_seed.sql
   with real quarterly numbers (8 rows each) for trustworthy recommendations.
'@
Write-Host $next.Replace('<REF>', $ProjectRef)
