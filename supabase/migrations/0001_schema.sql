-- Rasmal — real-data backend schema
-- ----------------------------------------------------------------------------
-- Cache + reference tables for Saudi (Tadawul) market data, plus per-user
-- holdings. Populated by the Edge Functions (service role bypasses RLS);
-- read by the app with each user's Supabase JWT.
--
-- Security model:
--   * Reference/cache tables (companies, quotes, fundamentals,
--     financial_statements, news, recommendations) are READ-ONLY to
--     authenticated users. Only the service role (Edge Functions) writes them.
--   * holdings is per-user, guarded by RLS so a user only ever sees their own.
-- ----------------------------------------------------------------------------

-- Reusable "keep updated_at fresh" trigger fn -------------------------------
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- companies ------------------------------------------------------------------
create table if not exists public.companies (
    code        text primary key,          -- Tadawul code, e.g. '2222'
    name        text not null,
    name_ar     text,
    badge       text,                       -- short ticker badge, e.g. 'ARC'
    sector      text not null,
    is_active   boolean not null default true,
    created_at  timestamptz not null default now()
);

-- quotes (cache, one row per company, overwritten by market-refresh) ---------
create table if not exists public.quotes (
    code        text primary key references public.companies(code) on delete cascade,
    price       numeric(12,2),
    change_pct  numeric(7,2),
    prev_close  numeric(12,2),
    day_high    numeric(12,2),
    day_low     numeric(12,2),
    volume      bigint,
    updated_at  timestamptz not null default now()
);

-- fundamentals (cache) -------------------------------------------------------
create table if not exists public.fundamentals (
    code           text primary key references public.companies(code) on delete cascade,
    pe             numeric(10,2),
    market_cap     numeric(18,2),
    dividend_yield numeric(7,2),
    updated_at     timestamptz not null default now()
);

-- financial_statements (HAND-SEEDED, ~quarterly) ----------------------------
create table if not exists public.financial_statements (
    id              bigint generated always as identity primary key,
    code            text not null references public.companies(code) on delete cascade,
    period          text not null,          -- e.g. 'Q1-2026'
    revenue         numeric(18,2),           -- SAR
    net_profit      numeric(18,2),           -- SAR
    net_profit_yoy  numeric(7,2),            -- % YoY change in net profit
    gross_margin    numeric(7,2),            -- %
    debt_ratio      numeric(7,2),            -- total debt / total assets, %
    reported_at     date,
    unique (code, period)
);

-- news (cache, appended by market-refresh) ----------------------------------
create table if not exists public.news (
    id            bigint generated always as identity primary key,
    code          text references public.companies(code) on delete cascade, -- null = market-wide
    headline      text not null,
    url           text unique,
    source        text,
    sentiment     numeric(4,3),              -- -1.000 .. 1.000
    published_at  timestamptz,
    created_at    timestamptz not null default now()
);
create index if not exists news_code_published_idx
    on public.news (code, published_at desc);

-- recommendations (cached scoring-engine output, per company + risk profile) -
create table if not exists public.recommendations (
    code          text not null references public.companies(code) on delete cascade,
    risk_profile  text not null,             -- 'conservative' | 'balanced' | 'aggressive'
    confidence    int  not null,             -- 0..100
    amount        numeric(18,2),             -- suggested SAR to invest
    buy_low       numeric(12,2),
    buy_high      numeric(12,2),
    target        numeric(12,2),
    stop          numeric(12,2),
    reasons       jsonb not null default '[]'::jsonb,
    narrative     text,
    generated_at  timestamptz not null default now(),
    primary key (code, risk_profile)
);

-- holdings (per-user portfolio) ---------------------------------------------
create table if not exists public.holdings (
    id          bigint generated always as identity primary key,
    user_id     uuid not null references auth.users(id) on delete cascade,
    code        text not null references public.companies(code) on delete cascade,
    shares      numeric(18,4) not null,
    avg_price   numeric(12,2) not null,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    unique (user_id, code)
);

drop trigger if exists holdings_set_updated_at on public.holdings;
create trigger holdings_set_updated_at
    before update on public.holdings
    for each row execute function public.set_updated_at();

-- ----------------------------------------------------------------------------
-- Row Level Security
-- ----------------------------------------------------------------------------
alter table public.companies            enable row level security;
alter table public.quotes               enable row level security;
alter table public.fundamentals         enable row level security;
alter table public.financial_statements enable row level security;
alter table public.news                 enable row level security;
alter table public.recommendations      enable row level security;
alter table public.holdings             enable row level security;

-- Reference/cache tables: authenticated users may read; nobody but the
-- service role (which bypasses RLS) may write.
do $$
declare t text;
begin
  foreach t in array array[
    'companies','quotes','fundamentals','financial_statements','news','recommendations'
  ] loop
    execute format(
      'drop policy if exists "read_%1$s" on public.%1$s;', t);
    execute format(
      'create policy "read_%1$s" on public.%1$s for select to authenticated using (true);', t);
  end loop;
end $$;

-- holdings: each user fully manages only their own rows.
drop policy if exists "holdings_select_own" on public.holdings;
create policy "holdings_select_own" on public.holdings
    for select to authenticated using (auth.uid() = user_id);

drop policy if exists "holdings_insert_own" on public.holdings;
create policy "holdings_insert_own" on public.holdings
    for insert to authenticated with check (auth.uid() = user_id);

drop policy if exists "holdings_update_own" on public.holdings;
create policy "holdings_update_own" on public.holdings
    for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);

drop policy if exists "holdings_delete_own" on public.holdings;
create policy "holdings_delete_own" on public.holdings
    for delete to authenticated using (auth.uid() = user_id);
