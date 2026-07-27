// Unit tests for the pure scoring engine. Run: `deno test supabase/functions/scoring_test.ts`
import {
  assert,
  assertEquals,
} from "https://deno.land/std@0.224.0/assert/mod.ts";
import { CompanyData, deriveTrade, scoreAll } from "./_shared/scoring.ts";

const sample: CompanyData[] = [
  { code: "A", name: "Growth Co", sector: "Tech", price: 100, changePct: 2.5,
    pe: 12, netProfitYoy: 28, grossMargin: 55, debtRatio: 12, sentiment: 0.6 },
  { code: "B", name: "Value Co", sector: "Tech", price: 50, changePct: 0.2,
    pe: 30, netProfitYoy: 2, grossMargin: 30, debtRatio: 40, sentiment: 0.0 },
  { code: "C", name: "Weak Co", sector: "Tech", price: 20, changePct: -2.0,
    pe: 45, netProfitYoy: -8, grossMargin: 20, debtRatio: 55, sentiment: -0.5 },
];

Deno.test("confidence is always within 0..100", () => {
  for (const risk of ["conservative", "balanced", "aggressive"] as const) {
    for (const s of scoreAll(sample, risk)) {
      assert(s.confidence >= 0 && s.confidence <= 100, `${s.code} ${s.confidence}`);
    }
  }
});

Deno.test("strong fundamentals rank above weak ones", () => {
  const ranked = scoreAll(sample, "balanced");
  assertEquals(ranked[0].code, "A");
  assertEquals(ranked[ranked.length - 1].code, "C");
});

Deno.test("aggressive weighting rewards momentum more than conservative", () => {
  const cons = scoreAll(sample, "conservative").find((s) => s.code === "A")!;
  const aggr = scoreAll(sample, "aggressive").find((s) => s.code === "A")!;
  // A has strong positive momentum, so aggressive should not score it lower.
  assert(aggr.confidence >= cons.confidence);
});

Deno.test("deriveTrade produces ordered, sane levels", () => {
  const t = deriveTrade(100, 80, "balanced", 100000);
  assert(t.stop < t.buyLow);
  assert(t.buyLow < t.buyHigh);
  assert(t.target > t.buyHigh);
  assert(t.amount > 0 && t.amount <= 100000);
});
