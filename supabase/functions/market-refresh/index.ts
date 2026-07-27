// market-refresh — scheduled job (pg_cron). Pulls SAHMK quotes + fundamentals
// and marketaux news for the active companies into the cache tables, so the app
// and the recommendations function read from Postgres, never from the vendors.
// This is what keeps us under SAHMK's 100 req/day free limit: N companies × a
// few runs/day, shared across all users.
//
// Protected by a shared CRON_SECRET header (set the same value in the cron job).
import { serviceClient, json } from "../_shared/supabase.ts";
import { fetchQuote, fetchNews } from "../_shared/providers.ts";

Deno.serve(async (req) => {
  if (req.headers.get("x-cron-secret") !== Deno.env.get("CRON_SECRET")) {
    return json({ error: "forbidden" }, 403);
  }

  const db = serviceClient();
  const { data: companies, error } = await db
    .from("companies")
    .select("code")
    .eq("is_active", true);
  if (error) return json({ error: error.message }, 500);

  const codes = (companies ?? []).map((c) => c.code as string);
  const result = { quotes: 0, news: 0, errors: [] as string[] };

  // Quotes, one company at a time (small N).
  for (const code of codes) {
    try {
      const q = await fetchQuote(code);
      if (q) {
        await db.from("quotes").upsert({
          code,
          price: q.price,
          change_pct: q.changePct,
          prev_close: q.prevClose,
          day_high: q.dayHigh,
          day_low: q.dayLow,
          volume: q.volume,
          updated_at: new Date().toISOString(),
        });
        result.quotes++;
      }
      // Fundamentals only appear on SAHMK Starter+; on the free tier /company/
      // always returns nulls, so calling it here just burns burst-rate quota
      // for nothing. Hand-seeded fundamentals in Postgres are used instead.
    } catch (e) {
      result.errors.push(`${code}: ${(e as Error).message}`);
    }
  }

  // News for the whole batch in one request.
  try {
    const rows = await fetchNews(codes);
    for (const n of rows) {
      // url is unique; ignore duplicates so re-runs don't error.
      await db.from("news").upsert(
        {
          code: n.code,
          headline: n.headline,
          url: n.url,
          source: n.source,
          sentiment: n.sentiment,
          published_at: n.publishedAt,
        },
        { onConflict: "url", ignoreDuplicates: true },
      );
    }
    result.news = rows.length;
  } catch (e) {
    result.errors.push(`news: ${(e as Error).message}`);
  }

  return json(result);
});
