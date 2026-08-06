-- Rasmal — earnings calendar + per-user alert feed (Story 013)
-- ----------------------------------------------------------------------------
-- Two tables:
--   * earnings_calendar — reference/cache of upcoming Tadawul reporting dates.
--     Written only by the service role (Edge Functions); readable by any
--     authenticated user, same contract as companies/quotes in 0001_schema.sql.
--   * alerts — the per-user notification feed rendered by AlertsFragment.
--     Guarded by RLS so a user only ever sees their own rows, and the client
--     may flip nothing but `read` (tap-to-dismiss).
--
-- Rows are fanned out from the calendar to holders by
-- public.generate_earnings_alerts(), below. The app reads:
--   GET   /rest/v1/alerts?select=id,code,title,body,earnings_date,read
--                        &order=created_at.desc
--   GET   /rest/v1/alerts?select=id&read=eq.false          -- unread badge
--   PATCH /rest/v1/alerts?id=eq.<id>   {"read": true}
-- ----------------------------------------------------------------------------

-- earnings_calendar (cache/reference) ----------------------------------------
create table if not exists public.earnings_calendar (
    code          text not null references public.companies(code) on delete cascade,
    period        text not null,             -- e.g. 'Q3-2026'
    earnings_date date not null,             -- expected or confirmed report date
    confirmed     boolean not null default false,
    updated_at    timestamptz not null default now(),
    primary key (code, period)
);

create index if not exists earnings_calendar_date_idx
    on public.earnings_calendar (earnings_date);

drop trigger if exists earnings_calendar_set_updated_at on public.earnings_calendar;
create trigger earnings_calendar_set_updated_at
    before update on public.earnings_calendar
    for each row execute function public.set_updated_at();

-- alerts (per-user feed) -----------------------------------------------------
create table if not exists public.alerts (
    id            bigint generated always as identity primary key,
    user_id       uuid not null references auth.users(id) on delete cascade,
    code          text references public.companies(code) on delete cascade, -- null = account-wide
    kind          text not null default 'earnings'
                  check (kind in ('earnings', 'price', 'system')),
    title         text not null,
    body          text,
    earnings_date date,
    read          boolean not null default false,
    created_at    timestamptz not null default now(),
    -- Makes the fan-out below idempotent: re-running it never duplicates an
    -- alert for the same user/company/report date. Note that Postgres treats
    -- NULLs as distinct, so this only constrains rows where code and
    -- earnings_date are set — i.e. exactly the 'earnings' rows we generate.
    unique (user_id, code, kind, earnings_date)
);

-- Feed order: newest first, scoped to one user.
create index if not exists alerts_user_created_idx
    on public.alerts (user_id, created_at desc);

-- Unread badge count: partial index keeps it cheap as the feed grows.
create index if not exists alerts_user_unread_idx
    on public.alerts (user_id) where read = false;

-- ----------------------------------------------------------------------------
-- Row Level Security
-- ----------------------------------------------------------------------------
alter table public.earnings_calendar enable row level security;
alter table public.alerts            enable row level security;

-- Calendar is read-only to users; only the service role (bypasses RLS) writes.
drop policy if exists "read_earnings_calendar" on public.earnings_calendar;
create policy "read_earnings_calendar" on public.earnings_calendar
    for select to authenticated using (true);

-- Alerts: read your own, and mark your own as read. Deliberately no insert or
-- delete policy — the feed is written by generate_earnings_alerts() only, so a
-- client cannot forge or destroy notifications.
drop policy if exists "alerts_select_own" on public.alerts;
create policy "alerts_select_own" on public.alerts
    for select to authenticated using (auth.uid() = user_id);

drop policy if exists "alerts_update_own" on public.alerts;
create policy "alerts_update_own" on public.alerts
    for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- RLS cannot restrict *which* columns an update touches, so narrow it with a
-- column grant: tap-to-dismiss may set `read` and nothing else.
revoke update on public.alerts from authenticated;
grant  update (read) on public.alerts to authenticated;

-- ----------------------------------------------------------------------------
-- Fan-out: calendar entry -> one alert per holder
-- ----------------------------------------------------------------------------
-- Call from the market-refresh Edge Function (service role) on its schedule:
--   select public.generate_earnings_alerts();
-- Returns the number of alerts actually created. security definer so it can
-- write alerts for every affected user while the client keeps no insert rights.
create or replace function public.generate_earnings_alerts(within_days int default 14)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
    created int;
begin
    insert into public.alerts (user_id, code, kind, title, body, earnings_date)
    select h.user_id,
           e.code,
           'earnings',
           c.name || ' reports on ' || to_char(e.earnings_date, 'DD Mon YYYY'),
           c.name || ' (' || e.code || ') is due to report ' || e.period
               || ' results on ' || to_char(e.earnings_date, 'DD Mon YYYY')
               || '. You hold ' || trim(to_char(h.shares, 'FM999999990.####'))
               || ' shares.',
           e.earnings_date
    from public.earnings_calendar e
    join public.companies c on c.code = e.code
    join public.holdings  h on h.code = e.code
    where e.earnings_date >= current_date
      and e.earnings_date <= current_date + within_days
    on conflict (user_id, code, kind, earnings_date) do nothing;

    get diagnostics created = row_count;
    return created;
end;
$$;

-- Only the service role may trigger the fan-out.
revoke all on function public.generate_earnings_alerts(int) from public, anon, authenticated;
