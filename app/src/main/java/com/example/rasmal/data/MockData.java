package com.example.rasmal.data;

import com.example.rasmal.R;
import com.example.rasmal.model.ChatMessage;
import com.example.rasmal.model.Holding;
import com.example.rasmal.model.Stock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * All hardcoded, mock data for Rasmal lives here so it is trivial to find and
 * later swap for a real data source. No network, no database — just static demo data.
 */
public final class MockData {

    private MockData() {}

    // ----- User -----
    public static final String USER_NAME = "Salman";
    public static final String GREETING = "Salam, Salman";
    public static final String TASI = "TASI 12,480 +0.6%";
    public static final String LIQUIDITY = "100,000";

    // ----- Portfolio hero -----
    public static final String PORTFOLIO_VALUE = "SAR 148,230.50";
    public static final String PORTFOLIO_DELTA = "+2.15%";
    public static final String PORTFOLIO_TODAY = "+ SAR 3,120 today";
    public static final String TOTAL_RETURN = "+18.4%";

    // Upward-trending normalized chart points (0..1) for the dashboard hero.
    public static final float[] CHART_POINTS = {
            0.14f, 0.20f, 0.17f, 0.29f, 0.26f, 0.40f,
            0.36f, 0.52f, 0.49f, 0.66f, 0.72f, 0.88f
    };

    // ----- Onboarding holdings (shares @ price) -----
    // Kept as a single shared, mutable instance so holdings added from the
    // "Add a stock" search flow survive navigating back to the portfolio screen.
    private static List<Holding> onboardingHoldings;

    public static List<Holding> onboardingHoldings() {
        if (onboardingHoldings == null) {
            onboardingHoldings = new ArrayList<>(Arrays.asList(
                    new Holding("Elm", "7203", "ELM", R.color.badge_elm, 20, 985.00),
                    new Holding("Al Rajhi Bank", "1120", "RJHI", R.color.badge_rjhi, 50, 78.50)
            ));
        }
        return onboardingHoldings;
    }

    // ----- Searchable stock catalog (mock Tadawul list) -----
    public static List<Stock> availableStocks() {
        return new ArrayList<>(Arrays.asList(
                new Stock("Elm", "7203", "ELM", R.color.badge_elm, "Technology", 985.00),
                new Stock("Al Rajhi Bank", "1120", "RJHI", R.color.badge_rjhi, "Banks", 78.50),
                new Stock("Saudi Aramco", "2222", "ARC", R.color.badge_arc, "Energy", 30.20),
                new Stock("stc", "7010", "STC", R.color.badge_stc, "Telecom", 42.10),
                new Stock("SABIC", "2010", "SBC", R.color.badge_sabic, "Materials", 74.80),
                new Stock("Ma'aden", "1211", "MADN", R.color.badge_maaden, "Materials", 52.30),
                new Stock("Saudi National Bank", "1180", "SNB", R.color.badge_snb, "Banks", 35.60),
                new Stock("Alinma Bank", "1150", "ALIN", R.color.badge_alinma, "Banks", 28.40),
                new Stock("Jarir Marketing", "4190", "JAR", R.color.badge_jarir, "Retail", 13.55),
                new Stock("ACWA Power", "2082", "ACWA", R.color.badge_acwa, "Utilities", 245.00),
                new Stock("Saudi Electricity", "5110", "SEC", R.color.badge_sec, "Utilities", 16.20),
                new Stock("Riyad Bank", "1010", "RYD", R.color.badge_riyad, "Banks", 27.90),
                new Stock("Almarai", "2280", "ALMR", R.color.badge_almarai, "Food", 54.70)
        ));
    }

    /** Looks up a catalog stock by its Tadawul code, or {@code null} if unknown. */
    public static Stock stockByCode(String code) {
        for (Stock s : availableStocks()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

    // ----- Dashboard holdings (value + % change) -----
    public static List<Holding> dashboardHoldings() {
        return new ArrayList<>(Arrays.asList(
                new Holding("Elm", "7203", "ELM", R.color.badge_elm,
                        "7203 · نقية", "SAR 19,800", "+3.2%", true),
                new Holding("Al Rajhi Bank", "1120", "RJHI", R.color.badge_rjhi,
                        "1120 · نقية", "SAR 15,400", "+0.8%", true),
                new Holding("Saudi Aramco", "2222", "ARC", R.color.badge_arc,
                        "2222 · نقية", "SAR 13,030", "-0.4%", false)
        ));
    }

    // ----- Chat -----
    public static List<ChatMessage> chatMessages() {
        return new ArrayList<>(Arrays.asList(
                new ChatMessage("Salam Salman 👋 I've analyzed your portfolio and today's market. "
                        + "What would you like to know?", false),
                new ChatMessage("What should I buy with my liquidity?", true),
                new ChatMessage("Based on your 100,000 SAR liquidity and balanced risk profile, "
                        + "I recommend Elm (7203): buy 20,000 in range 990-1,020, target 1,200. "
                        + "Q1 results were strong. Want the full breakdown?", false)
        ));
    }

    public static final String MOCK_AI_REPLY =
            "Good question. Based on current Tadawul momentum and your balanced risk profile, "
                    + "I'd keep quality names like Elm (7203) and Al Rajhi (1120) core, and add on "
                    + "dips within the buy range. Want me to run a full analysis?";

    // ----- AI recommendation reasons -----
    public static final String[] AI_REASONS = {
            "Q1 net profit up 28% YoY — beat analyst estimates.",
            "Government digital-transformation pipeline is expanding.",
            "P/E below the 3-year sector average — currently undervalued.",
            "Uses only 20% of your liquidity — fits a balanced risk profile."
    };
}
