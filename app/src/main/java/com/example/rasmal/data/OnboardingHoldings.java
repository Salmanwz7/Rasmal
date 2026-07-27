package com.example.rasmal.data;

import com.example.rasmal.model.Holding;

import java.util.ArrayList;
import java.util.List;

/**
 * Holdings a user is staging during onboarding, shared between the portfolio
 * review screen and the "add a stock" flow. Starts empty; each addition is
 * also persisted to Supabase separately via {@link ApiClient#upsertHolding}.
 */
public final class OnboardingHoldings {

    private OnboardingHoldings() {}

    private static final List<Holding> holdings = new ArrayList<>();

    public static List<Holding> list() {
        return holdings;
    }
}
