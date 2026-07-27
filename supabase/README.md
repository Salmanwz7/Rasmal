# Rasmal backend (Supabase) — setup & deploy

This folder is the secure backend for Rasmal. **All third-party API keys live here as
Supabase secrets and never ship inside the Android APK.** The app talks only to these
Edge Functions (and to PostgREST for the user's own `holdings`), authenticated with the
user's Supabase JWT.

> ⚠️ Shared project — coordinate with Firas before deploying. This is the same Supabase
> project used for auth.

```
supabase/
├── config.toml                     # CLI config (set project_id / run `supabase link`)
├── migrations/
│   ├── 0001_schema.sql             # tables + RLS
│   └── 0002_seed.sql               # 8 companies + PLACEHOLDER financial statements
└── functions/
    ├── _shared/                    # supabase.ts, scoring.ts, providers.ts
    ├── market-refresh/             # scheduled cache filler (SAHMK + marketaux)
    ├── recommendations/            # hybrid scoring + LLM narrative
    ├── chat/                       # AI chat proxy to OpenRouter
    └── scoring_test.ts             # `deno test` unit tests for the engine
```

## 0. Get free API keys
- **SAHMK** (Saudi market data) → https://www.sahmk.sa/en/developers — free tier, 100 req/day.
  Base URL `https://app.sahmk.sa/api/v1`. **Free tier gives live `/quote/` only**; `/company`
  omits fundamentals and `/financials` returns 403 — so P/E and statements are hand-seeded.
- **marketaux** (finance news) → https://www.marketaux.com — free tier, ~100 req/day. Verified.
- **OpenRouter** (LLM) → https://openrouter.ai — create a key; free models cost $0.
  Primary model **`nvidia/nemotron-3-super-120b-a12b:free`** (verified, Arabic-capable); the
  function falls through a chain of free models on 429, so no single model being throttled breaks it.

## 1. Set secrets (never committed)
```bash
supabase link --project-ref YOUR_PROJECT_REF
supabase secrets set \
  SAHMK_KEY=shmk_live_... \
  MARKETAUX_KEY=... \
  OPENROUTER_KEY=sk-or-v1-... \
  OPENROUTER_MODEL=nvidia/nemotron-3-super-120b-a12b:free \
  CRON_SECRET=$(openssl rand -hex 16)
```
`SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY` are injected automatically.
(`OPENROUTER_MODEL` is optional — omit it to use the built-in default + fallback chain.)

## 2. Apply the schema + seed
```bash
supabase db push          # runs migrations/*.sql
```
Then **replace the placeholder financial statements** in `0002_seed.sql` (or via the
dashboard) with each company's real latest quarterly numbers — only 8 rows.

## 3. Deploy the functions
```bash
supabase functions deploy market-refresh
supabase functions deploy recommendations
supabase functions deploy chat
```

## 4. Schedule the refresh (pg_cron + pg_net)
Run once in the SQL editor (fill in your ref + the CRON_SECRET you set above). 3 runs/day
× 8 companies keeps SAHMK well under 100 req/day:
```sql
create extension if not exists pg_cron;
create extension if not exists pg_net;

select cron.schedule('rasmal-market-refresh', '0 4,9,13 * * *', $$
  select net.http_post(
    url     := 'https://YOUR_PROJECT_REF.functions.supabase.co/market-refresh',
    headers := jsonb_build_object('x-cron-secret', 'YOUR_CRON_SECRET'),
    body    := '{}'::jsonb
  );
$$);
```
Kick off a first fill manually:
```bash
curl -X POST https://YOUR_PROJECT_REF.functions.supabase.co/market-refresh \
  -H "x-cron-secret: YOUR_CRON_SECRET"
```

## 5. Test locally
```bash
deno test supabase/functions/scoring_test.ts     # scoring engine
supabase functions serve                          # then curl with a real user JWT
```

## Notes
- `market-refresh` is protected by the `x-cron-secret` header (JWT verification off).
- `recommendations` and `chat` require a valid Supabase user JWT.
- Provider adapters in `functions/_shared/providers.ts` were **verified live** (2026): SAHMK
  `/quote/{code}/` and marketaux both confirmed; SAHMK fundamentals/financials are paid-only,
  hence the hand-seeded `fundamentals` + `financial_statements` rows in `0002_seed.sql`.
