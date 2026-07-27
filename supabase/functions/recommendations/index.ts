// recommendations — the hybrid engine the app calls for the AI Recommendation
// screen. Reads cached market data + seeded statements, scores every company
// (scoring.ts), picks the top candidate for the user's risk profile, and asks
// the LLM to narrate the numbers. Falls back to a rules-only narrative if the
// LLM is unavailable/rate-limited, and caches per (code, risk_profile).
import { requireUser, serviceClient, json } from "../_shared/supabase.ts";
import { openRouterChat } from "../_shared/providers.ts";
import {
  CompanyData,
  deriveTrade,
  RiskProfile,
  Scored,
  scoreAll,
} from "../_shared/scoring.ts";

const FRESH_MS = 6 * 60 * 60 * 1000; // reuse a cached recommendation for 6h

function normRisk(v: unknown): RiskProfile {
  const s = String(v ?? "").toLowerCase();
  return s === "conservative" || s === "aggressive" ? s : "balanced";
}

Deno.serve(async (req) => {
  const user = await requireUser(req);
  if (!user) return json({ error: "unauthorized" }, 401);

  const body = await req.json().catch(() => ({}));
  const risk = normRisk(body.risk_profile);
  const liquidity = Number(body.liquidity) > 0 ? Number(body.liquidity) : 100000;

  const db = serviceClient();

  // --- Load everything the engine needs, in parallel ---
  const sinceIso = new Date(Date.now() - 14 * 24 * 60 * 60 * 1000).toISOString();
  const [companiesR, quotesR, fundR, stmtR, newsR] = await Promise.all([
    db.from("companies").select("code,name,sector").eq("is_active", true),
    db.from("quotes").select("code,price,change_pct"),
    db.from("fundamentals").select("code,pe"),
    db.from("financial_statements")
      .select("code,net_profit_yoy,gross_margin,debt_ratio,reported_at")
      .order("reported_at", { ascending: false }),
    db.from("news").select("code,sentiment").gte("published_at", sinceIso),
  ]);

  const companies = companiesR.data ?? [];
  if (companies.length === 0) return json({ error: "no companies configured" }, 500);

  const quote = new Map((quotesR.data ?? []).map((q) => [q.code, q]));
  const fund = new Map((fundR.data ?? []).map((f) => [f.code, f]));

  // latest statement per code (rows already sorted newest-first)
  const stmt = new Map<string, any>();
  for (const s of stmtR.data ?? []) if (!stmt.has(s.code)) stmt.set(s.code, s);

  // mean sentiment per code
  const sentAgg = new Map<string, { total: number; n: number }>();
  for (const n of newsR.data ?? []) {
    if (n.code == null || n.sentiment == null) continue;
    const a = sentAgg.get(n.code) ?? { total: 0, n: 0 };
    a.total += Number(n.sentiment);
    a.n += 1;
    sentAgg.set(n.code, a);
  }

  const dataset: CompanyData[] = companies.map((c) => {
    const q = quote.get(c.code);
    const f = fund.get(c.code);
    const s = stmt.get(c.code);
    const se = sentAgg.get(c.code);
    return {
      code: c.code,
      name: c.name,
      sector: c.sector,
      price: q?.price ?? null,
      changePct: q?.change_pct ?? null,
      pe: f?.pe ?? null,
      netProfitYoy: s?.net_profit_yoy ?? null,
      grossMargin: s?.gross_margin ?? null,
      debtRatio: s?.debt_ratio ?? null,
      sentiment: se ? se.total / se.n : null,
    };
  });

  const ranked = scoreAll(dataset, risk);
  const ranking = ranked.map((r) => {
    const c = companies.find((x) => x.code === r.code)!;
    return { code: r.code, name: c.name, confidence: r.confidence };
  });

  // Pick the best candidate that actually has a price to trade around.
  const top = ranked.find((r) => (quote.get(r.code)?.price ?? 0) > 0) ?? ranked[0];
  const topData = dataset.find((d) => d.code === top.code)!;
  const price = quote.get(top.code)?.price ?? 0;

  // --- Serve a fresh cached recommendation if we have one ---
  const cached = await db.from("recommendations").select("*")
    .eq("code", top.code).eq("risk_profile", risk).maybeSingle();
  if (
    cached.data &&
    Date.now() - new Date(cached.data.generated_at).getTime() < FRESH_MS
  ) {
    return json({ pick: cached.data, ranking, cached: true });
  }

  const trade = deriveTrade(price || 0, top.confidence, risk, liquidity);

  // --- Narrative: LLM preferred, rules-only fallback ---
  let narrative = "";
  let reasons: string[] = [];
  let llm = false;
  try {
    const out = await openRouterChat([
      {
        role: "system",
        content:
          "You are a Saudi (Tadawul) equity analyst for the Rasmal app. Use ONLY the " +
          "numbers provided — never invent figures. Reply as strict JSON: " +
          '{"narrative": string (2-3 sentences), "reasons": string[] (exactly 4 short bullets)}.',
      },
      {
        role: "user",
        content: JSON.stringify({
          company: topData.name,
          code: topData.code,
          sector: topData.sector,
          risk_profile: risk,
          confidence: top.confidence,
          price,
          pe: topData.pe,
          net_profit_yoy: topData.netProfitYoy,
          gross_margin: topData.grossMargin,
          debt_ratio: topData.debtRatio,
          news_sentiment: topData.sentiment,
          suggested: trade,
        }),
      },
    ]);
    const parsed = JSON.parse(out);
    narrative = String(parsed.narrative ?? "").trim();
    reasons = Array.isArray(parsed.reasons) ? parsed.reasons.map(String) : [];
    llm = narrative.length > 0;
  } catch (e) {
    console.error("LLM narrative failed, using fallback:", (e as Error).message);
  }
  if (!llm) {
    ({ narrative, reasons } = fallbackNarrative(topData, top, trade, risk));
  }

  const pick = {
    code: top.code,
    risk_profile: risk,
    confidence: top.confidence,
    amount: trade.amount,
    buy_low: trade.buyLow,
    buy_high: trade.buyHigh,
    target: trade.target,
    stop: trade.stop,
    reasons,
    narrative,
    generated_at: new Date().toISOString(),
  };
  await db.from("recommendations").upsert(pick);

  return json({ pick, ranking, cached: false, llm });
});

/** Deterministic narrative when the LLM is unavailable. */
function fallbackNarrative(d: CompanyData, s: Scored, trade: any, risk: RiskProfile) {
  const reasons: string[] = [];
  if (d.netProfitYoy != null) {
    reasons.push(
      d.netProfitYoy >= 0
        ? `Net profit up ${d.netProfitYoy}% YoY in the latest quarter.`
        : `Net profit down ${Math.abs(d.netProfitYoy)}% YoY — watch closely.`,
    );
  }
  if (s.subscores.valuation > 0.55) reasons.push("Valuation (P/E) below its sector peers.");
  if (d.grossMargin != null && d.grossMargin >= 40) {
    reasons.push(`Healthy gross margin of ${d.grossMargin}%.`);
  }
  if (d.sentiment != null) {
    reasons.push(
      d.sentiment >= 0 ? "Recent news sentiment is positive." : "Recent news sentiment is cautious.",
    );
  }
  reasons.push(
    `Suggested allocation of SAR ${trade.amount.toLocaleString()} fits a ${risk} profile.`,
  );
  const narrative =
    `${d.name} (${d.code}) scores ${s.confidence}/100 for a ${risk} profile based on ` +
    `momentum, valuation, fundamentals and news. Consider buying in SAR ${trade.buyLow}–${trade.buyHigh}, ` +
    `target ${trade.target}, stop ${trade.stop}.`;
  return { narrative, reasons: reasons.slice(0, 4) };
}
