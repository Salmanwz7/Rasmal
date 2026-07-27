// Rasmal — external data providers (server-side only; keys come from Edge Function
// secrets and NEVER reach the app). Each provider's parsing is isolated in a small
// adapter so it is easy to correct once verified against the live API response.
//
// ⚠️ VERIFY THE FIELD MAPPINGS: the exact JSON shapes for SAHMK and marketaux must
// be confirmed against their real docs/responses. The request/auth style below
// follows their documented conventions; adjust the `map*` helpers if a field name
// differs. Everything is written defensively (missing fields → null, never throws).

// ---------------------------------------------------------------------------
// SAHMK — Saudi (Tadawul) market data.  Auth: `X-API-Key` header.
// Verified live against the free tier (2026): /quote works; /company omits the
// `fundamentals` block and /financials returns 403 PLAN_LIMIT on free — so P/E
// and statements are hand-seeded in Postgres, and only quotes come from SAHMK.
// ---------------------------------------------------------------------------
const SAHMK_BASE = Deno.env.get("SAHMK_BASE") ?? "https://app.sahmk.sa/api/v1";

export interface Quote {
  price: number | null;
  changePct: number | null;
  prevClose: number | null;
  dayHigh: number | null;
  dayLow: number | null;
  volume: number | null;
}
export interface Fundamentals {
  pe: number | null;
  marketCap: number | null;
  dividendYield: number | null;
}

const num = (v: unknown): number | null => {
  const n = typeof v === "string" ? Number(v) : (v as number);
  return Number.isFinite(n) ? n : null;
};

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

// SAHMK's free tier has an undocumented short burst limit (observed: ~5 quick
// requests succeed, the next ones 429 even seconds later) well under the
// documented 100/day cap. Retry a 429 with backoff before giving up.
async function sahmkGet(path: string): Promise<any | null> {
  const key = Deno.env.get("SAHMK_KEY");
  if (!key) throw new Error("SAHMK_KEY secret is not set");
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const res = await fetch(`${SAHMK_BASE}${path}`, {
        headers: { "X-API-Key": key, "Accept": "application/json" },
      });
      if (res.status === 429) {
        await res.body?.cancel();
        console.error(`SAHMK ${path} -> 429 (attempt ${attempt + 1})`);
        await sleep(1000 * (attempt + 1));
        continue;
      }
      if (!res.ok) {
        console.error(`SAHMK ${path} -> ${res.status}`);
        return null;
      }
      return await res.json();
    } catch (e) {
      console.error(`SAHMK ${path} fetch failed:`, e);
      return null;
    }
  }
  console.error(`SAHMK ${path} -> gave up after 429 retries`);
  return null;
}

/** Fetch a delayed/real-time quote for one Tadawul code. Fields are top-level. */
export async function fetchQuote(code: string): Promise<Quote | null> {
  const j = await sahmkGet(`/quote/${code}/`);
  if (!j) return null;
  const d = j.data ?? j; // SAHMK returns fields flat; guard anyway.
  return {
    price: num(d.price ?? d.last ?? d.close),
    changePct: num(d.change_percent ?? d.changePct ?? d.percent_change),
    prevClose: num(d.previous_close ?? d.prev_close ?? d.previousClose),
    dayHigh: num(d.high ?? d.day_high),
    dayLow: num(d.low ?? d.day_low),
    volume: num(d.volume),
  };
}

/**
 * Fetch fundamentals (P/E, market cap, dividend yield) for one code from
 * /company/{code}/. On the FREE tier the `fundamentals` block is absent, so this
 * returns all-nulls — callers must fall back to the hand-seeded `fundamentals`
 * table and must NOT overwrite seeded values with these nulls. Works as-is if the
 * account is later upgraded to Starter+ (the block then appears).
 */
export async function fetchFundamentals(code: string): Promise<Fundamentals | null> {
  const j = await sahmkGet(`/company/${code}/`);
  if (!j) return null;
  const d = j.data ?? j;
  const f = d.fundamentals ?? {};
  return {
    pe: num(f.pe_ratio ?? f.pe ?? d.pe_ratio),
    marketCap: num(f.market_cap ?? d.market_cap),
    dividendYield: num(f.dividend_yield ?? d.dividend_yield),
  };
}

// ---------------------------------------------------------------------------
// marketaux — finance news with per-entity sentiment.  Auth: `api_token` query.
// ---------------------------------------------------------------------------
const MARKETAUX_BASE = "https://api.marketaux.com/v1/news/all";

export interface NewsRow {
  code: string | null;
  headline: string;
  url: string | null;
  source: string | null;
  sentiment: number | null; // -1..1
  publishedAt: string | null;
}

/**
 * Fetch recent news for a batch of Tadawul symbols. marketaux tags each article
 * with per-entity sentiment; we attribute the article to the first matching code.
 */
export async function fetchNews(codes: string[]): Promise<NewsRow[]> {
  const token = Deno.env.get("MARKETAUX_KEY");
  if (!token) throw new Error("MARKETAUX_KEY secret is not set");

  // marketaux uses `<CODE>.SR` for Tadawul-listed symbols.
  const symbols = codes.map((c) => `${c}.SR`).join(",");
  const url = `${MARKETAUX_BASE}?symbols=${symbols}&filter_entities=true` +
    `&language=en&limit=3&api_token=${token}`;

  try {
    const res = await fetch(url);
    if (!res.ok) {
      console.error(`marketaux -> ${res.status}`);
      return [];
    }
    const j = await res.json();
    const articles: any[] = j.data ?? [];
    const rows: NewsRow[] = [];
    for (const a of articles) {
      const entities: any[] = a.entities ?? [];
      const matched = entities.find((e) =>
        typeof e.symbol === "string" && e.symbol.endsWith(".SR")
      );
      const code = matched ? String(matched.symbol).replace(/\.SR$/, "") : null;
      rows.push({
        code: code && codes.includes(code) ? code : null,
        headline: a.title ?? "",
        url: a.url ?? null,
        source: a.source ?? null,
        sentiment: num(matched?.sentiment_score ?? a.sentiment ?? null),
        publishedAt: a.published_at ?? null,
      });
    }
    return rows.filter((r) => r.headline);
  } catch (e) {
    console.error("marketaux fetch failed:", e);
    return [];
  }
}

// ---------------------------------------------------------------------------
// OpenRouter — LLM narrative + chat.  Auth: `Authorization: Bearer <key>`.
// ---------------------------------------------------------------------------
const OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

export interface ChatMessage {
  role: "system" | "user" | "assistant";
  content: string;
}

// Free models rotate their upstream rate-limits, so we try a chain and fall
// through on 429. All live-tested (2026) against an Arabic Tadawul recommendation
// prompt and spread across 3 providers so one provider's rate limit doesn't take
// out the whole chain. Override the primary with the OPENROUTER_MODEL secret; it
// is tried first, then these.
const FREE_MODEL_CHAIN = [
  "nvidia/nemotron-3-ultra-550b-a55b:free",
  "nvidia/nemotron-3-super-120b-a12b:free",
  "google/gemma-4-26b-a4b-it:free",
  "openai/gpt-oss-20b:free",
];

async function callModel(key: string, model: string, messages: ChatMessage[]) {
  return await fetch(OPENROUTER_URL, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${key}`,
      "Content-Type": "application/json",
      "HTTP-Referer": "https://github.com/Salmanwz7/Rasmal",
      "X-Title": "Rasmal",
    },
    // include_reasoning: false tells reasoning-capable free models (seen live on
    // gpt-oss-20b:free) to keep their chain-of-thought out of `content` instead
    // of leaking it into the visible reply. max_tokens raised for the fuller,
    // multi-section analyses the chat/recommendations prompts now ask for.
    body: JSON.stringify({
      model,
      messages,
      temperature: 0.4,
      max_tokens: 900,
      include_reasoning: false,
    }),
  });
}

/**
 * Calls the free OpenRouter model chain. Falls through to the next model on a
 * 429 (rate limit). If EVERY model is rate-limited, throws "rate_limited" so
 * callers can serve cache / a rules-only fallback.
 */
export async function openRouterChat(messages: ChatMessage[]): Promise<string> {
  const key = Deno.env.get("OPENROUTER_KEY");
  if (!key) throw new Error("OPENROUTER_KEY secret is not set");

  const preferred = Deno.env.get("OPENROUTER_MODEL");
  const chain = preferred
    ? [preferred, ...FREE_MODEL_CHAIN.filter((m) => m !== preferred)]
    : FREE_MODEL_CHAIN;

  let allRateLimited = true;
  for (const model of chain) {
    const res = await callModel(key, model, messages);
    if (res.status === 429) {
      await res.body?.cancel();
      continue; // try the next free model
    }
    allRateLimited = false;
    if (!res.ok) {
      const body = await res.text();
      throw new Error(`OpenRouter ${res.status} (${model}): ${body.slice(0, 200)}`);
    }
    const j = await res.json();
    const content = j.choices?.[0]?.message?.content ?? "";
    if (content) return content;
  }
  if (allRateLimited) throw new Error("rate_limited");
  return "";
}
