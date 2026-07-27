-- Rasmal — per-user profile (onboarding answers)
-- ----------------------------------------------------------------------------
-- One row per user holding the answers captured during onboarding:
--   * risk_profile — risk appetite (Story 004), a factor in recommendations.
--   * liquidity    — available cash (Story 003), sizes suggested trades.
--   * onboarded    — whether the user finished the onboarding flow.
-- Written by the app with the user's JWT; guarded by RLS so a user only ever
-- sees and edits their own row. Consumed by the recommendations Edge Function
-- (risk_profile + liquidity) and, later, by the Profile/Settings screen.
-- ----------------------------------------------------------------------------

create table if not exists public.profiles (
    user_id      uuid primary key references auth.users(id) on delete cascade,
    risk_profile text not null default 'balanced'
                 check (risk_profile in ('conservative', 'balanced', 'aggressive')),
    liquidity    numeric(18,2) not null default 0,
    onboarded    boolean not null default false,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

-- Reuse the shared "keep updated_at fresh" trigger fn from 0001_schema.sql.
drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
    before update on public.profiles
    for each row execute function public.set_updated_at();

-- ----------------------------------------------------------------------------
-- Row Level Security — each user fully manages only their own row.
-- ----------------------------------------------------------------------------
alter table public.profiles enable row level security;

drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own" on public.profiles
    for select to authenticated using (auth.uid() = user_id);

drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own" on public.profiles
    for insert to authenticated with check (auth.uid() = user_id);

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own" on public.profiles
    for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
