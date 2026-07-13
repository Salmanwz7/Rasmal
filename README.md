# Rasmal 📈

**AI‑powered portfolio companion for the Saudi stock market (Tadawul).**

Rasmal is a native Android app that lets a user sign up, describe their portfolio and risk appetite, and get AI‑style recommendations and chat about Saudi (Tadawul) stocks — all wrapped in a clean, dark, Material 3 interface.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84)
![Language](https://img.shields.io/badge/language-Java-orange)
![minSdk](https://img.shields.io/badge/minSdk-24-blue)
![targetSdk](https://img.shields.io/badge/targetSdk-36-blue)

> ⚠️ **This is a UI prototype running on mock data.**
> There is **no backend, no network, and no database**. Every number, stock, chart, and "AI" reply is hardcoded demo data in [`MockData.java`](app/src/main/java/com/example/rasmal/data/MockData.java). The recommendations are **not** real financial advice. The project is a front‑end foundation, ready to be wired to a real API later.

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

---

## ✨ Features

- **Auth screens** — Sign In / Sign Up (UI only, no real authentication).
- **Onboarding flow** (reached via Sign Up):
  - **Portfolio step** — declare available cash and the stocks you own.
  - **Add a stock** — search a Tadawul catalog by name / symbol / ticker, pick a stock, then enter your **number of shares** and **average buy price**.
  - **Risk step** — choose Conservative / Balanced / Aggressive.
- **Dashboard** — portfolio value hero card with a custom line chart, AI recommendation banner, and your holdings list.
- **AI Recommendation** — a detailed buy recommendation with confidence, reasoning, and a "did you execute?" action.
- **AI Chat** — a chat interface with suggestion chips (mock replies).

### Screen flow

```
Sign In ──► Sign Up ──► Onboarding: Portfolio ──► Add a stock (search ──► details)
                                     │
                                     ▼
                              Onboarding: Risk ──► Dashboard ──► AI Recommendation
                                                        └──────► AI Chat
```

> Note: **Sign In goes straight to the Dashboard** (returning‑user shortcut). The onboarding questions only appear when you go through **Sign Up**.

---

## 🛠 Tech stack

| Area | Choice |
|---|---|
| Language | **Java** |
| UI | Android Views + **Material 3**, **View Binding** |
| Navigation | **Jetpack Navigation Component** (single‑Activity, multi‑Fragment) |
| Lists | RecyclerView + custom adapters |
| Charts | Custom `LineChartView` (canvas‑drawn) |
| Build | Gradle (Kotlin DSL) + version catalog (`gradle/libs.versions.toml`) |
| Min / Target SDK | 24 / 36 · compiled with SDK 36 · Java 11 |
| Data | 100% mock — see [`MockData.java`](app/src/main/java/com/example/rasmal/data/MockData.java) |

---

## 📂 Project structure

```
app/src/main/
├── java/com/example/rasmal/
│   ├── MainActivity.java          # single Activity, hosts the nav graph + bottom nav
│   ├── data/MockData.java         # ALL demo data lives here
│   ├── model/                     # Holding, Stock, ChatMessage
│   ├── adapter/                   # RecyclerView adapters
│   ├── ui/                        # Fragments (one per screen)
│   └── view/LineChartView.java    # custom chart view
└── res/
    ├── layout/                    # XML layouts
    ├── navigation/nav_graph.xml   # screen graph + actions
    ├── drawable/                  # icons, backgrounds, gradients
    └── values/                    # colors, strings, dimens, styles, themes
```

---

## 🚀 Getting started (for developers)

### Option A — Android Studio (recommended)

1. **Clone** the repo:
   ```bash
   git clone <this-repo-url>
   ```
2. **Open** the project in **Android Studio** (Ladybug or newer recommended).
3. Let **Gradle sync** finish. Android Studio auto‑generates `local.properties` with your SDK path — you don't commit that file.
4. Press **Run ▶** on an emulator or a physical Android device (needs an Android device/emulator — **iOS is not supported**, this is a native Android app).

### Option B — Build an APK from the command line

Useful on low‑RAM machines where the emulator is too heavy — build the APK and run it elsewhere (a real device, or a cloud emulator like [appetize.io](https://appetize.io)).

```bash
# From the project root. On Windows, point JAVA_HOME at Android Studio's bundled JDK:
#   set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr

./gradlew assembleDebug
```

The APK is produced at:
```
app/build/outputs/apk/debug/app-debug.apk
```
Install it on a device (`adb install app-debug.apk`) or upload it to a browser‑based Android emulator such as **appetize.io**.

> **Requirements:** Android SDK 36 installed, JDK 11+ (the JDK bundled with Android Studio works). No API keys or secrets are needed — the app is fully offline mock data.

---

## 🤝 Contributing / working together

- Create a **feature branch** off `main` (`git checkout -b feature/my-change`), commit, and open a Pull Request for review.
- Keep all demo data in **`MockData.java`** so it's trivial to find and later swap for a real data source.
- Follow the existing structure: **one Fragment per screen** in `ui/`, register it in `res/navigation/nav_graph.xml`, and reuse the `Widget.Rasmal.*` / `Text.Rasmal.*` styles for a consistent look.
- Strings go in `res/values/strings.xml`, colors in `colors.xml`, spacing/sizes in `dimens.xml`.

---

## 🗺 Roadmap ideas

- Replace `MockData` with a real Tadawul market‑data API.
- Real authentication + persistent user profiles.
- Wire the AI chat/recommendations to an actual LLM backend.
- Persist the user's portfolio (Room / DataStore) instead of in‑memory.
