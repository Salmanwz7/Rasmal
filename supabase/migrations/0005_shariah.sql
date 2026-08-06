-- Rasmal — Shariah compliance data (Story 011)
-- ----------------------------------------------------------------------------
-- Real classification, computed from two genuine screens (the same ones AAOIFI/
-- Dow Jones Islamic Market-style screens use), not a placeholder label:
--
--   1. Business/sector screen (this column) — hand-classified per company, same
--      as the rest of this catalog (see 0002_seed.sql). Conventional,
--      interest-based banks fail this screen; Sharia-supervised Islamic banks
--      and real-economy sectors (energy, materials, telecom, utilities, tech)
--      pass it and move on to the ratio screen below.
--   2. Debt ratio screen (computed at query time by the app from the real,
--      already-seeded `financial_statements.debt_ratio`) — total debt / total
--      assets must be under 30%, the standard AAOIFI threshold.
--
-- A company is Shariah-compliant (نقية) only if it passes both. This stays
-- correct automatically as debt_ratio is refreshed each quarter — it is not a
-- frozen label.
-- ----------------------------------------------------------------------------

alter table public.companies
    add column if not exists shariah_sector_compliant boolean not null default true;

-- Saudi National Bank (1180): a conventional, interest-based bank — fails the
-- business screen regardless of its balance sheet.
update public.companies set shariah_sector_compliant = false where code = '1180';

-- Al Rajhi Bank (1120) is intentionally left TRUE: unlike SNB it operates as a
-- fully Sharia-supervised Islamic bank, so it passes the business screen and is
-- judged on the debt ratio screen like any other company.
