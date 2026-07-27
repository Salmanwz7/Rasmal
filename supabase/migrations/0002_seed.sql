-- Rasmal — seed data
-- ----------------------------------------------------------------------------
-- Companies: the 8 starter Tadawul names (codes match the app's catalog).
-- Re-runnable: on conflict updates the descriptive fields.
-- ----------------------------------------------------------------------------
insert into public.companies (code, name, name_ar, badge, sector) values
    ('2222', 'Saudi Aramco',        'أرامكو السعودية', 'ARC',  'Energy'),
    ('1120', 'Al Rajhi Bank',       'مصرف الراجحي',    'RJHI', 'Banks'),
    ('7010', 'stc',                 'إس تي سي',        'STC',  'Telecom'),
    ('2010', 'SABIC',               'سابك',            'SBC',  'Materials'),
    ('1180', 'Saudi National Bank', 'البنك الأهلي',    'SNB',  'Banks'),
    ('1211', 'Ma''aden',            'معادن',           'MADN', 'Materials'),
    ('2082', 'ACWA Power',          'أكوا باور',       'ACWA', 'Utilities'),
    ('7203', 'Elm',                 'علم',             'ELM',  'Technology')
on conflict (code) do update
    set name = excluded.name, name_ar = excluded.name_ar,
        badge = excluded.badge, sector = excluded.sector;

-- ----------------------------------------------------------------------------
-- Financial statements + fundamentals — REAL DATA (auto-generated).
-- ----------------------------------------------------------------------------
-- The two blocks below are produced by `scripts/fetch_statements.ts` from Yahoo
-- Finance's free fundamentals-timeseries endpoint — real FY-2025 annual figures,
-- NOT placeholders. SAHMK's free tier can't provide these (/company omits
-- fundamentals, /financials returns 403), and Yahoo blocks datacenter IPs, so we
-- generate at seed time and commit the numbers rather than fetch them at runtime.
--
-- To refresh after new filings (~quarterly):
--   deno run --allow-net supabase/scripts/fetch_statements.ts > supabase/scripts/seed.generated.sql
-- then paste the two blocks below. Live prices still come from SAHMK at runtime.
--
-- Notes on the real data: banks (1120/1180) report no "gross profit" so
-- gross_margin is their net margin; SABIC (2010) posted an FY-2025 net LOSS, so
-- its net_profit is negative, YoY is a large swing, and P/E is null (no positive
-- earnings) — the scoring engine clamps these and correctly ranks it low.
--
-- Values in SAR; net_profit_yoy / gross_margin / debt_ratio in %.
insert into public.financial_statements
    (code, period, revenue, net_profit, net_profit_yoy, gross_margin, debt_ratio, reported_at) values
    ('2222', 'FY-2025', 1671204000000, 348042000000, -11.64, 50.16, 14.25, '2025-12-31'),
    ('1120', 'FY-2025', 38967166000, 24791754000, 25.7, 63.62, 7.7, '2025-12-31'),
    ('7010', 'FY-2025', 77818675000, 14828030000, -39.94, 48.45, 11.08, '2025-12-31'),
    ('2010', 'FY-2025', 116525214000, -25779231000, -1775.56, 18.02, 15.17, '2025-12-31'),
    ('1180', 'FY-2025', 40843879000, 25013279000, 18.03, 61.24, 11.01, '2025-12-31'),
    ('1211', 'FY-2025', 38577730228, 7347878280, 155.89, 38.34, 28.26, '2025-12-31'),
    ('2082', 'FY-2025', 7413501000, 1852225000, 5.42, 50.78, 43.99, '2025-12-31'),
    ('7203', 'FY-2025', 9464884988, 2090353004, 14.42, 38.85, 22.1, '2025-12-31')
on conflict (code, period) do update
    set revenue = excluded.revenue, net_profit = excluded.net_profit,
        net_profit_yoy = excluded.net_profit_yoy, gross_margin = excluded.gross_margin,
        debt_ratio = excluded.debt_ratio, reported_at = excluded.reported_at;

-- pe = trailing price/earnings (x); market_cap in SAR; dividend_yield left null
-- (needs the crumb-gated dividend endpoint). market-refresh overwrites these iff
-- SAHMK is upgraded to a plan that returns fundamentals.
insert into public.fundamentals (code, pe, market_cap, dividend_yield) values
    ('2222', 18.53, 6448444833333, null),
    ('1120', 16.53,  409699627000, null),
    ('7010', 14.54,  215580584310, null),
    ('2010', null,   null,         null),
    ('1180',  9.46,  236726168998, null),
    ('1211', 29.40,  216012233205, null),
    ('2082', 74.37,  137754547571, null),
    ('7203', 23.99,   50152872447, null)
on conflict (code) do update
    set pe = excluded.pe, market_cap = excluded.market_cap,
        dividend_yield = excluded.dividend_yield, updated_at = now();
