package com.example.rasmal.data;

import com.example.rasmal.model.Holding;

import java.util.ArrayList;
import java.util.List;

/**
 * Holdings staged in-memory for the two "manage holdings" screens (onboarding's
 * portfolio review and the post-sign-in Portfolio tab), shared with the
 * "add a stock" flow. Each addition/edit is also persisted to Supabase
 * separately via {@link ApiClient#upsertHolding}.
 */
public final class OnboardingHoldings {

    private OnboardingHoldings() {}

    private static final List<Holding> holdings = new ArrayList<>();

    public static List<Holding> list() {
        return holdings;
    }

    /** Replaces the contents in place (same list instance) so bound adapters keep working. */
    public static void setAll(List<Holding> newHoldings) {
        holdings.clear();
        holdings.addAll(newHoldings);
    }
}
