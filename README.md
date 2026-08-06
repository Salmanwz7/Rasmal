# Rasmal 📈

**AI‑powered portfolio companion for the Saudi stock market (Tadawul).**

Rasmal is a native Android app that lets a user sign up, describe their portfolio and risk appetite, and get real, personalized AI recommendations and chat about Saudi (Tadawul) stocks — backed by a Supabase backend and an LLM — all wrapped in a clean, dark, Material 3 interface.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84)
![Language](https://img.shields.io/badge/language-Java-orange)
![Backend](https://img.shields.io/badge/backend-Supabase-3ECF8E)
![minSdk](https://img.shields.io/badge/minSdk-24-blue)
![targetSdk](https://img.shields.io/badge/targetSdk-36-blue)

> ℹ️ **Status: real backend, wired end‑to‑end. Sprint 3 in progress.**
> The app runs on a **Supabase backend** — email/password **Auth**, a Postgres database with **Row‑Level Security**, and **Edge Functions** that score Saudi stocks and proxy an LLM for recommendations and chat. **No third‑party API keys ship in the APK**; the app talks only to Edge Functions and PostgREST using the signed‑in user's JWT.
>
> The dashboard **performance chart is now real**, rebuilt from your own transaction ledger. Still outstanding: company **financial statements** are hand‑seeded (the free market‑data tier omits them), the **Shariah** label is a hardcoded placeholder, the **News** tab has layouts but no screen behind it yet, and the **Alerts** screen needs its backing table (see [Known gaps](#-known-gaps)). Recommendations are **AI analysis, not financial advice.** See the [backlog status](#-backlog-status) for the exact picture.

---

## 📱 Screenshots

> Capture these from the running app (e.g. appetize.io has a screenshot button) and drop the PNGs into the [`screenshots/`](screenshots/) folder using the filenames below — they'll appear here automatically. See [`screenshots/README.md`](screenshots/README.md) for the exact list.

| Sign In | Sign Up | Onboarding · Portfolio |
|---|---|---|
| ![Sign In](screenshots/01-signin.png) | ![Sign Up](screenshots/02-signup.png) | ![Portfolio](screenshots/03-onboarding-portfolio.png) |

| Add Stock · Search | Add Stock · Details | Onboarding · Risk |
|---|---|---|
| ![Search](screenshots/04-add-stock-search.png) | ![Details](screenshots/05-add-stock-details.png) | ![Risk](screenshots/06-onboarding-risk.png) |

| Dashboard | AI Recommendation | AI Chat |
|---|---|---|
| ![Dashboard](screenshots/07-dashboard.png) | ![AI Recommendation](screenshots/08-ai-recommendation.png) | ![AI Chat](screenshots/09-ai-chat.png) |

> 📸 **Still to capture:** the screens added in Sprint 3 — **Portfolio tab** (`10-portfolio.png`), **Profile & Settings** (`11-profile-settings.png`), and **Alerts** (`12-alerts.png`). Drop them in `screenshots/` and add a row here.

---

## ✨ Features

- **Authentication (real)** — Sign Up / Sign In via **Supabase Auth**. Client‑side validation, duplicate‑email detection, email‑confirmation support, and a session persisted on device so you stay signed in. Sign out revokes the session.
- **Onboarding flow** (reached via Sign Up):
  - **Portfolio step** — declare available cash and the stocks you own. Cash is **persisted to your Supabase profile**.
  - **Add a stock** — search a Tadawul catalog by name / symbol / ticker, pick a stock, then enter your **number of shares** and **average buy price**; the holding is **saved to Supabase**.
  - **Risk step** — choose Conservative / Balanced / Aggressive; the choice is **persisted and feeds recommendations**.
- **Dashboard (real)** — total portfolio value, **today's P&L**, **total return**, and **available liquidity** computed from your holdings and live cached quotes.
  - **Performance chart (real)** — reconstructed from your actual transaction ledger plus current value, with the change over the period. Shows an empty state until you have your first trade.
  - **Concentration warnings (real)** — flags when a single stock exceeds **25%** or one sector exceeds **40%** of the portfolio, surfacing the more severe of the two.
- **Portfolio tab (real)** — manage holdings any time after onboarding: add, **edit**, or **remove** a holding, and edit your available liquidity. Changes sync to Supabase and the portfolio recalculates.
- **AI Recommendation (real)** — a hybrid scoring engine ranks Saudi companies for your risk profile and an LLM narrates it: amount to invest, buy range, target, stop, reasoning bullets, and a confidence level.
- **Trade Confirmation (real)** — after a recommendation, confirm whether you **bought or sold**, enter the **actual price and quantity**, and the portfolio updates (weighted‑average cost on buys; reduce/close on sells). Each trade is stored in a per‑user ledger.
- **AI Chat (real)** — ask about your portfolio or any Saudi stock; answers come from the LLM with your conversation history as context. Replies are rendered through a lightweight markdown formatter.
- **Profile & Settings (real)** — edit your display name, switch your **risk profile** (persisted to Supabase and immediately reflected in recommendations), and sign out securely.
- **Alerts** — an in‑app feed of earnings notifications for your holdings, with unread badging and tap‑to‑dismiss. ⚠️ *Needs its backing table — see [Known gaps](#-known-gaps).*

### Screen flow

```
Sign In ──► Sign Up ──► Onboarding: Portfolio ──► Add a stock (search ──► details)
                                     │                    ▲
                                     │            (tap a row to edit)
                                     ▼
                              Onboarding: Risk ──► Dashboard ──► AI Recommendation ──► Confirm trade
                                                        ├──────► AI Chat
                                                        └──────► Alerts
```

Once you're past onboarding, a **bottom navigation bar** appears on the main screens:

| Tab | Destination |
|---|---|
| Home | Dashboard |
| Portfolio | Manage holdings + liquidity |
| AI Chat | Chat assistant |
| News | *Not wired yet — shows "coming soon"* |
| Profile | Profile & Settings |

> Note: **Sign In routes to the Dashboard** if you've onboarded, otherwise to onboarding. The onboarding questions appear the first time through.

---

## ✅ Backlog status

Sprints 1–2 are complete; Sprint 3 is underway. Legend: ✅ done · 🟡 partial · ⬜ not started.

| # | Component | Story | Status | Notes |
|---|-----------|-------|:------:|-------|
| 001 | Registration | Registration | ✅ | Supabase Auth sign‑up; validation, duplicate‑email + email‑confirmation handling. |
| 002 | Login & Auth | Login and Authentication | ✅ | Sign in, session persisted across launches, routing to onboarding/dashboard, sign out. |
| 003 | Onboarding | Portfolio Setup | ✅ | Holdings + available cash persisted (`profiles` + `holdings`). |
| 004 | Onboarding | Risk Profiling | ✅ | Risk appetite persisted and used as a factor in recommendations. |
| 005 | Dashboard | Portfolio Dashboard | ✅ | Real value, today's P&L, total return, liquidity — **and the performance chart**, now built from the transaction ledger. |
| 006 | Portfolio Mgmt | Manage Holdings | ✅ | Add, edit, and remove — all persisted (RLS‑scoped per user), reachable any time from the **Portfolio tab**. |
| 007 | AI Engine | Stock Recommendation | ✅ | Scoring engine over cached market data + seeded fundamentals; amount, buy range, target, stop. |
| 008 | AI Engine | Recommendation Reasoning | ✅ | Reasoning bullets + narrative + confidence level. |
| 009 | Portfolio Mgmt | Trade Confirmation | ✅ | Bought/sold prompt, actual price + quantity, portfolio update, trade ledger. |
| 010 | AI Chat | AI Chat Assistant | ✅ | LLM answers grounded in the user's data + conversation history; markdown rendered. |
| 014 | Dashboard | Portfolio Health Analysis | ✅ | Over‑concentration warnings — >25% single stock, >40% single sector. |
| 015 | Profile | Profile and Settings | ✅ | Edit display name, change risk profile (persisted), secure logout. Reachable from the Profile tab. |
| 013 | Notifications | Earnings Alerts | 🟡 | Screen, adapter, unread badge, API calls and the `alerts` + `earnings_calendar` schema are all in place — but **nothing populates the calendar yet**, and delivery is in‑app only (no push). |
| 012 | Market Data | Market News Feed | 🟡 | Backend caches marketaux news into `news`; **layouts exist but no fragment/nav entry** — the News tab still shows "coming soon". |
| 011 | Market Data | Shariah Compliance Filter | ⬜ | Still a hardcoded نقية label in layouts; needs a data source + classification. |

### ⚠️ Known gaps

Two Sprint 3 stories are further from done than their commits suggest — worth knowing before you demo:

- **Alerts (013)** — `0005_alerts.sql` now creates the `alerts` table (RLS‑scoped, client may only flip `read`) plus an `earnings_calendar` reference table and `generate_earnings_alerts()` to fan entries out to holders. What's still missing is a **source of earnings dates**: until something populates `earnings_calendar`, the fan‑out has nothing to do and the feed stays empty. Wiring it into `market-refresh` is the next step.
- **News feed (012)** — commit `b763a75` added `fragment_news.xml` and `item_market_news.xml` only. There is no `NewsFragment`, no adapter, and no `newsFragment` destination in `nav_graph.xml`, so the bottom‑nav News tab falls through to the "coming soon" toast in `MainActivity`.

---

## 🛠 Tech stack

| Area | Choice |
|---|---|
| App language | **Java** |
| UI | Android Views + **Material 3**, **View Binding** |
| Navigation | **Jetpack Navigation Component** (single‑Activity, multi‑Fragment) |
| Networking | **OkHttp** → Supabase Edge Functions + PostgREST (JWT‑authenticated) |
| Backend | **Supabase** — Auth, Postgres + Row‑Level Security, **Edge Functions** (Deno / TypeScript) |
| AI | Scoring engine (rules) + **LLM via OpenRouter** for narrative & chat |
| Market data | SAHMK (quotes) + marketaux (news), cached server‑side via a scheduled function |
| Charts | Custom `LineChartView` (canvas‑drawn) |
| Build | Gradle (Kotlin DSL) + version catalog (`gradle/libs.versions.toml`) |
| Min / Target SDK | 24 / 36 · compiled with SDK 36 · Java 11 |

---

## 📂 Project structure

```
app/src/main/
├── java/com/example/rasmal/
│   ├── MainActivity.java          # single Activity: nav graph + bottom nav + auth deep link
│   ├── auth/                      # SupabaseAuth, Session, SessionManager, AuthCallback
│   ├── data/
│   │   ├── ApiClient.java         # JWT-signed calls to Edge Functions + PostgREST
│   │   └── OnboardingHoldings.java# holdings staged during onboarding, before sign-up completes
│   ├── model/                     # Holding, Stock, ChatMessage, Recommendation, Alert
│   ├── adapter/                   # RecyclerView adapters (dashboard, portfolio, chat, alerts)
│   ├── ui/                        # Fragments (one per screen)
│   ├── util/MarkdownLite.java     # minimal markdown → Spannable for AI replies
│   └── view/LineChartView.java    # custom canvas-drawn chart view
└── res/
    ├── layout/                    # XML layouts
    ├── navigation/nav_graph.xml   # screen graph + actions
    ├── menu/bottom_nav_menu.xml   # bottom navigation tabs
    ├── drawable/                  # icons, backgrounds, gradients
    └── values/                    # colors, strings, dimens, styles, themes

supabase/                          # the backend (see supabase/README.md)
├── migrations/                    # 0001 schema + RLS · 0002 seed · 0003 profiles
│                                  # 0004 transactions · 0005 alerts + earnings calendar
├── functions/                     # recommendations, chat, market-refresh (Deno/TS) + _shared/
├── scripts/                       # fetch_statements.ts, generated seed SQL
└── deploy.ps1                     # one-shot deploy helper for Windows
```

---

## 🚀 Getting started

### 1. Run the app UI

The Android app builds and runs on its own; without backend config it uses placeholder Supabase values and degrades gracefully (auth/network calls simply fail and screens fall back to demo values).

**Android Studio:** clone, open, let Gradle sync, press **Run ▶** on an emulator or Android device (native Android — iOS is not supported).

**Command line** (handy on low‑RAM machines — build the APK and run it on a device or a cloud emulator like [appetize.io](https://appetize.io)):

```bash
# On Windows, point JAVA_HOME at Android Studio's bundled JDK first:
#   set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

### 2. Connect it to Supabase (for the real data)

Add your project's values to **`local.properties`** (not committed) so they're compiled into `BuildConfig`:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_ANON_KEY
```

### 3. Deploy the backend

The backend lives in [`supabase/`](supabase/) and has its own setup guide — see **[`supabase/README.md`](supabase/README.md)** for API keys, secrets, and deployment. In short:

```bash
supabase link --project-ref YOUR_PROJECT_REF
supabase db push                       # applies migrations (schema, seed, profiles, transactions)
supabase functions deploy recommendations chat market-refresh
```

> **Requirements:** Android SDK 36, JDK 11+ (the one bundled with Android Studio works), and — for real data — a Supabase project. Secrets (SAHMK / marketaux / OpenRouter keys) live only as Supabase secrets, never in the app.

---

## 🤝 Contributing / working together

- Create a **feature branch** off `main` (`git checkout -b feature/my-change`), commit, and open a Pull Request for review.
- App ↔ backend boundary: the app calls **`ApiClient`**, which hits Edge Functions / PostgREST with the user's JWT. Keep third‑party keys server‑side only.
- Follow the existing structure: **one Fragment per screen** in `ui/`, register it in `res/navigation/nav_graph.xml`, and reuse the `Widget.Rasmal.*` / `Text.Rasmal.*` styles for a consistent look.
- Strings go in `res/values/strings.xml`, colors in `colors.xml`, spacing/sizes in `dimens.xml`.

---

## 🗺 Roadmap — remaining in Sprint 3

- **012 Market news feed** — build `NewsFragment` + adapter over the existing layouts, add the `newsFragment` destination, and point the News tab at it. The backend already caches the data.
- **013 Earnings alerts** — populate `earnings_calendar` from a real source and call `select public.generate_earnings_alerts();` from `market-refresh` on its schedule, then consider push delivery beyond the in‑app feed.
- **011 Shariah compliance filter** — classify each stock نقية / مختلطة from a real source instead of the hardcoded label.
- **Real financial statements** — replace the hand‑seeded `financial_statements` rows once a data source covering them is in place.

**Done since the last README update:** performance chart (005), portfolio health analysis (014), profile & settings (015), and reaching holdings management after onboarding via the Portfolio tab (006).
