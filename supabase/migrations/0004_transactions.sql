-- Rasmal — trade ledger (executed buys/sells)
-- ----------------------------------------------------------------------------
-- Append-only record of the trades a user confirms after a recommendation
-- (Story 009). Each row is one executed buy or sell at an actual price and
-- quantity; the app also updates the matching holdings row. Written by the app
-- with the user's JWT and guarded by RLS so a user only sees their own trades.
-- Insert + read only — a ledger is never edited or deleted from the client.
-- ----------------------------------------------------------------------------

create table if not exists public.transactions (
    id          bigint generated always as identity primary key,
    user_id     uuid not null references auth.users(id) on delete cascade,
    code        text not null references public.companies(code) on delete cascade,
    side        text not null check (side in ('buy', 'sell')),
    shares      numeric(18,4) not null,
    price       numeric(12,2) not null,
    created_at  timestamptz not null default now()
);

create index if not exists transactions_user_created_idx
    on public.transactions (user_id, created_at desc);

-- ----------------------------------------------------------------------------
-- Row Level Security — each user reads and appends only their own trades.
-- ----------------------------------------------------------------------------
alter table public.transactions enable row level security;

drop policy if exists "transactions_select_own" on public.transactions;
create policy "transactions_select_own" on public.transactions
    for select to authenticated using (auth.uid() = user_id);

drop policy if exists "transactions_insert_own" on public.transactions;
create policy "transactions_insert_own" on public.transactions
    for insert to authenticated with check (auth.uid() = user_id);
