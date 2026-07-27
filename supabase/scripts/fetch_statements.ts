// Rasmal — real financial-statement + fundamentals seed generator.
// ---------------------------------------------------------------------------
// Pulls REAL annual figures for the 8 starter companies from Yahoo Finance's
// free, no-auth `fundamentals-timeseries` endpoint (plus `chart` for price),
// computes the fields the scoring engine needs, and prints ready-to-run SQL.
//
//   deno run --allow-net supabase/scripts/fetch_statements.ts > seed.generated.sql
//
// NOTE: Yahoo's endpoints are unofficial/undocumented and may block datacenter
// IPs, so this is a SEED-TIME tool (run locally, commit the output) — NOT a
// runtime dependency. Statements only change ~quarterly; re-run then. Quotes
// still come live from SAHMK at runtime.
// ---------------------------------------------------------------------------

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

const COMPANIES = [
  { code: "2222", sector: "Energy" },
  { code: "1120", sector: "Banks" },
  { code: "7010", sector: "Telecom" },
  { code: "2010", sector: "Materials" },
  { code: "1180", sector: "Banks" },
  { code: "1211", sector: "Materials" },
  { code: "2082", sector: "Utilities" },
  { code: "7203", sector: "Technology" },
];

type Point = { date: string; val: number };
const YEAR = 365 * 24 * 3600;

/** Fetch several annual timeseries types at once; returns type -> chronological points. */
async function timeseries(code: string, types: string[]): Promise<Record<string, Point[]>> {
  const sym = `${code}.SR`;
  const p2 = Math.floor(Date.now() / 1000);
  const p1 = p2 - 8 * YEAR;
  const url = `https://query1.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/${sym}` +
    `?symbol=${sym}&type=${types.join(",")}&period1=${p1}&period2=${p2}`;
  const res = await fetch(url, { headers: { "User-Agent": UA } });
  if (!res.ok) throw new Error(`timeseries ${code} -> ${res.status}`);
  const j = await res.json();
  const out: Record<string, Point[]> = {};
  for (const s of j?.timeseries?.result ?? []) {
    const type = s?.meta?.type?.[0];
    if (!type) continue;
    const pts: Point[] = [];
    for (const v of s[type] ?? []) {
      const raw = v?.reportedValue?.raw;
      if (v?.asOfDate && typeof raw === "number") pts.push({ date: v.asOfDate, val: raw });
    }
    pts.sort((a, b) => a.date.localeCompare(b.date));
    out[type] = pts;
  }
  return out;
}

/** Current price + EPS + market cap from the chart endpoint meta. */
async function quoteMeta(code: string): Promise<{ price: number | null }> {
  const sym = `${code}.SR`;
  const res = await fetch(
    `https://query1.finance.yahoo.com/v8/finance/chart/${sym}?interval=1d&range=1d`,
    { headers: { "User-Agent": UA } },
  );
  if (!res.ok) return { price: null };
  const j = await res.json();
  const m = j?.chart?.result?.[0]?.meta ?? {};
  return { price: typeof m.regularMarketPrice === "number" ? m.regularMarketPrice : null };
}

const last = (p?: Point[]) => (p && p.length ? p[p.length - 1] : null);
const prev = (p?: Point[]) => (p && p.length > 1 ? p[p.length - 2] : null);
const r2 = (n: number) => Math.round(n * 100) / 100;
const sqlNum = (n: number | null) => (n === null || !Number.isFinite(n) ? "null" : String(n));
const sqlStr = (s: string | null) => (s === null ? "null" : `'${s.replace(/'/g, "''")}'`);

const stmtRows: string[] = [];
const fundRows: string[] = [];

for (const { code, sector } of COMPANIES) {
  try {
    const t = await timeseries(code, [
      "annualTotalRevenue",
      "annualNetIncome",
      "annualGrossProfit",
      "annualTotalDebt",
      "annualTotalAssets",
      "annualDilutedEPS",
      "annualBasicEPS",
    ]);
    const { price } = await quoteMeta(code);

    const revNow = last(t.annualTotalRevenue);
    const niNow = last(t.annualNetIncome);
    const niPrev = prev(t.annualNetIncome);
    const grossNow = last(t.annualGrossProfit);
    const debtNow = last(t.annualTotalDebt);
    const assetsNow = last(t.annualTotalAssets);
    const eps = last(t.annualDilutedEPS) ?? last(t.annualBasicEPS);

    const asOf = revNow?.date ?? niNow?.date ?? null;
    const period = asOf ? `FY-${asOf.slice(0, 4)}` : "FY-unknown";

    // YoY net-profit growth (%)
    const yoy = niNow && niPrev && niPrev.val !== 0
      ? r2(((niNow.val - niPrev.val) / Math.abs(niPrev.val)) * 100)
      : null;

    // Margin (%). Banks report no "gross profit" → fall back to net margin.
    let margin: number | null = null;
    if (grossNow && revNow && revNow.val !== 0) margin = r2((grossNow.val / revNow.val) * 100);
    else if (niNow && revNow && revNow.val !== 0) margin = r2((niNow.val / revNow.val) * 100);

    // Debt ratio (%) = total debt / total assets.
    const debtRatio = debtNow && assetsNow && assetsNow.val !== 0
      ? r2((debtNow.val / assetsNow.val) * 100)
      : null;

    // Trailing P/E = price / EPS.
    const pe = price && eps && eps.val > 0 ? r2(price / eps.val) : null;
    // Market cap = price x shares; shares implied by net income / EPS (same FY, SAR).
    const marketCap = price && eps && eps.val > 0 && niNow
      ? Math.round((niNow.val / eps.val) * price)
      : null;
    const divYield = null; // needs the dividend endpoint (crumb-gated); left null.

    stmtRows.push(
      `    ('${code}', '${period}', ${sqlNum(revNow?.val ?? null)}, ${sqlNum(niNow?.val ?? null)}, ` +
        `${sqlNum(yoy)}, ${sqlNum(margin)}, ${sqlNum(debtRatio)}, ${sqlStr(asOf)}),`,
    );
    fundRows.push(
      `    ('${code}', ${sqlNum(pe)}, ${sqlNum(marketCap)}, ${sqlNum(divYield)}),`,
    );

    console.error(
      `${code} ${sector.padEnd(10)} ${period}  rev=${revNow?.val ?? "—"} ni=${niNow?.val ?? "—"} ` +
        `yoy=${yoy ?? "—"}% margin=${margin ?? "—"}% debt=${debtRatio ?? "—"}% pe=${pe ?? "—"}`,
    );
  } catch (e) {
    console.error(`!! ${code}: ${(e as Error).message}`);
  }
}

// Trim the trailing comma on the last row of each block.
const trim = (rows: string[]) => rows.map((r, i) => (i === rows.length - 1 ? r.replace(/,$/, "") : r));

const today = new Date().toISOString().slice(0, 10);
console.log(`-- Generated by scripts/fetch_statements.ts from Yahoo Finance on ${today}.
-- REAL annual figures (not placeholders). Regenerate quarterly:
--   deno run --allow-net supabase/scripts/fetch_statements.ts > seed.generated.sql
-- Values in SAR; net_profit_yoy / gross_margin / debt_ratio in %.

insert into public.financial_statements
    (code, period, revenue, net_profit, net_profit_yoy, gross_margin, debt_ratio, reported_at) values
${trim(stmtRows).join("\n")}
on conflict (code, period) do update
    set revenue = excluded.revenue, net_profit = excluded.net_profit,
        net_profit_yoy = excluded.net_profit_yoy, gross_margin = excluded.gross_margin,
        debt_ratio = excluded.debt_ratio, reported_at = excluded.reported_at;

insert into public.fundamentals (code, pe, market_cap, dividend_yield) values
${trim(fundRows).join("\n")}
on conflict (code) do update
    set pe = excluded.pe, market_cap = excluded.market_cap,
        dividend_yield = excluded.dividend_yield, updated_at = now();`);
