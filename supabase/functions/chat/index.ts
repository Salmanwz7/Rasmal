// chat — proxies the AI Chat screen to OpenRouter, injecting the user's holdings
// and a live market snapshot as context. Keeps the OpenRouter key server-side.
import { requireUser, serviceClient, json } from "../_shared/supabase.ts";
import { ChatMessage, openRouterChat } from "../_shared/providers.ts";

Deno.serve(async (req) => {
  const user = await requireUser(req);
  if (!user) return json({ error: "unauthorized" }, 401);

  const body = await req.json().catch(() => ({}));
  const message = String(body.message ?? "").trim();
  if (!message) return json({ error: "empty message" }, 400);
  // Optional prior turns from the app: [{role:'user'|'assistant', content}]
  const history: ChatMessage[] = Array.isArray(body.history)
    ? body.history.filter((m: any) =>
      (m.role === "user" || m.role === "assistant") && typeof m.content === "string"
    ).slice(-8)
    : [];

  const db = serviceClient();
  const [holdingsR, quotesR, compsR] = await Promise.all([
    db.from("holdings").select("code,shares,avg_price").eq("user_id", user.id),
    db.from("quotes").select("code,price,change_pct"),
    db.from("companies").select("code,name"),
  ]);

  const name = new Map((compsR.data ?? []).map((c) => [c.code, c.name]));
  const quote = new Map((quotesR.data ?? []).map((q) => [q.code, q]));

  const holdings = (holdingsR.data ?? []).map((h) => ({
    company: name.get(h.code) ?? h.code,
    code: h.code,
    shares: h.shares,
    avg_price: h.avg_price,
    current_price: quote.get(h.code)?.price ?? null,
  }));
  const market = (quotesR.data ?? []).map((q) => ({
    company: name.get(q.code) ?? q.code,
    code: q.code,
    price: q.price,
    change_pct: q.change_pct,
  }));

  const system: ChatMessage = {
    role: "system",
    content:
      "You are Rasmal's AI assistant for the Saudi (Tadawul) stock market. Be concise, " +
      "friendly, and practical. Base answers on the user's holdings and the market " +
      "snapshot below; do not invent prices or figures. You may reply in the user's " +
      "language (English or Arabic). This is informational, not licensed financial advice.\n\n" +
      "Your reply is shown as plain text in a mobile chat bubble, not rendered markdown. " +
      "Do not use markdown syntax: no **bold**, no # headers, no | tables |, no code " +
      "fences. Write plain sentences and short paragraphs; if you need a list, put each " +
      "item on its own line starting with a plain dash.\n\n" +
      `User holdings: ${JSON.stringify(holdings)}\n` +
      `Market snapshot: ${JSON.stringify(market)}`,
  };

  try {
    const reply = await openRouterChat([system, ...history, { role: "user", content: message }]);
    return json({ reply });
  } catch (e) {
    const msg = (e as Error).message;
    const friendly = msg === "rate_limited"
      ? "I'm getting a lot of requests right now — please try again in a minute."
      : "Sorry, I couldn't reach the AI service just now. Please try again shortly.";
    return json({ reply: friendly, error: msg }, 200);
  }
});
