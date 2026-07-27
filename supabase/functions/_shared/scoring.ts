// Rasmal — hybrid recommendation scoring engine (pure, no I/O so it's unit-testable).
// Produces a 0..100 confidence per company from momentum, valuation,
// fundamentals and news sentiment, weighted by the user's risk profile.
// The LLM only narrates the numbers this engine computes — it never invents them.

export type RiskProfile = "conservative" | "balanced" | "aggressive";

export interface CompanyData {
  code: string;
  name: string;
  sector: string;
  price: number | null;
  changePct: number | null; // latest daily % change
  pe: number | null;
  netProfitYoy: number | null; // %
  grossMargin: number | null; // %
  debtRatio: number | null; // % (total debt / assets)
  sentiment: number | null; // mean news sentiment, -1..1
}

export interface SubScores {
  momentum: number; // 0..1
  valuation: number; // 0..1
  fundamentals: number; // 0..1
  sentiment: number; // 0..1
}

export interface Scored {
  code: string;
  confidence: number; // 0..100
  subscores: SubScores;
}

export interface Trade {
  amount: number;
  buyLow: number;
  buyHigh: number;
  target: number;
  stop: number;
}

const clamp01 = (x: number): number => Math.max(0, Math.min(1, x));

// Weight sets per risk profile (must each sum to 1).
const WEIGHTS: Record<RiskProfile, SubScores> = {
  conservative: { momentum: 0.10, valuation: 0.30, fundamentals: 0.40, sentiment: 0.20 },
  balanced: { momentum: 0.25, valuation: 0.25, fundamentals: 0.30, sentiment: 0.20 },
  aggressive: { momentum: 0.40, valuation: 0.15, fundamentals: 0.25, sentiment: 0.20 },
};

// --- Individual sub-scores (each 0..1, 0.5 = neutral when data is missing) ---

function momentumScore(changePct: number | null): number {
  if (changePct == null) return 0.5;
  // Map a daily move of -3%..+3% onto 0..1.
  return clamp01((changePct + 3) / 6);
}

/** Lower P/E relative to the peer (sector) average scores higher. */
function valuationScore(pe: number | null, sectorAvgPe: number | null): number {
  if (pe == null || pe <= 0 || sectorAvgPe == null || sectorAvgPe <= 0) return 0.5;
  const ratio = pe / sectorAvgPe; // 1 = in line with peers
  // ratio 0.5 -> 1.0, ratio 1.0 -> 0.5, ratio 1.5 -> 0.0
  return clamp01(1.5 - ratio);
}

function fundamentalsScore(d: CompanyData): number {
  const parts: number[] = [];
  if (d.netProfitYoy != null) parts.push(clamp01((d.netProfitYoy + 10) / 40)); // -10%..+30%
  if (d.grossMargin != null) parts.push(clamp01(d.grossMargin / 70)); // 0..70%
  if (d.debtRatio != null) parts.push(clamp01(1 - d.debtRatio / 60)); // lower debt is better
  if (parts.length === 0) return 0.5;
  return parts.reduce((a, b) => a + b, 0) / parts.length;
}

function sentimentScore(sentiment: number | null): number {
  if (sentiment == null) return 0.5;
  return clamp01((sentiment + 1) / 2);
}

/** Average P/E per sector across the supplied set (ignores null/non-positive). */
function sectorAveragePe(companies: CompanyData[]): Record<string, number> {
  const sums: Record<string, { total: number; n: number }> = {};
  for (const c of companies) {
    if (c.pe != null && c.pe > 0) {
      const s = (sums[c.sector] ??= { total: 0, n: 0 });
      s.total += c.pe;
      s.n += 1;
    }
  }
  const avg: Record<string, number> = {};
  for (const [sector, { total, n }] of Object.entries(sums)) avg[sector] = total / n;
  return avg;
}

/** Scores one company against the peer averages of the whole set. */
export function scoreCompany(
  d: CompanyData,
  risk: RiskProfile,
  sectorAvgPe: Record<string, number>,
): Scored {
  const w = WEIGHTS[risk];
  const sub: SubScores = {
    momentum: momentumScore(d.changePct),
    valuation: valuationScore(d.pe, sectorAvgPe[d.sector] ?? null),
    fundamentals: fundamentalsScore(d),
    sentiment: sentimentScore(d.sentiment),
  };
  const weighted =
    sub.momentum * w.momentum +
    sub.valuation * w.valuation +
    sub.fundamentals * w.fundamentals +
    sub.sentiment * w.sentiment;
  return { code: d.code, confidence: Math.round(clamp01(weighted) * 100), subscores: sub };
}

/** Scores and ranks the whole set (highest confidence first). */
export function scoreAll(companies: CompanyData[], risk: RiskProfile): Scored[] {
  const avg = sectorAveragePe(companies);
  return companies
    .map((c) => scoreCompany(c, risk, avg))
    .sort((a, b) => b.confidence - a.confidence);
}

/** Turns a price + confidence + risk + liquidity into concrete trade levels. */
export function deriveTrade(
  price: number,
  confidence: number,
  risk: RiskProfile,
  liquidity: number,
): Trade {
  const upsideCap = { conservative: 0.12, balanced: 0.18, aggressive: 0.25 }[risk];
  const stopPct = { conservative: 0.05, balanced: 0.07, aggressive: 0.10 }[risk];
  const allocCap = { conservative: 0.15, balanced: 0.20, aggressive: 0.30 }[risk];

  const upside = (confidence / 100) * upsideCap;
  const round2 = (x: number) => Math.round(x * 100) / 100;

  return {
    amount: round2(Math.min(liquidity, liquidity * allocCap * (confidence / 100))),
    buyLow: round2(price * 0.99),
    buyHigh: round2(price * 1.02),
    target: round2(price * (1 + upside)),
    stop: round2(price * (1 - stopPct)),
  };
}
