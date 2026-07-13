package com.example.rasmal.model;

/**
 * A single portfolio holding. Two flavours are used across the app:
 *  - onboarding rows use {@code shares} + {@code buyPrice}
 *  - dashboard rows use {@code subtitle} + {@code valueLabel} + {@code changeLabel} + {@code up}
 */
public class Holding {

    public final String name;
    public final String code;          // e.g. "7203"
    public final String badge;         // ticker letters, e.g. "ELM"
    public final int badgeColorRes;    // R.color.badge_*

    // Onboarding fields
    public int shares;
    public final double buyPrice;

    // Dashboard fields
    public final String subtitle;      // e.g. "7203 · نقية"
    public final String valueLabel;    // e.g. "SAR 19,800"
    public final String changeLabel;   // e.g. "+3.2%"
    public final boolean up;

    /** Onboarding constructor. */
    public Holding(String name, String code, String badge, int badgeColorRes,
                   int shares, double buyPrice) {
        this.name = name;
        this.code = code;
        this.badge = badge;
        this.badgeColorRes = badgeColorRes;
        this.shares = shares;
        this.buyPrice = buyPrice;
        this.subtitle = null;
        this.valueLabel = null;
        this.changeLabel = null;
        this.up = true;
    }

    /** Dashboard constructor. */
    public Holding(String name, String code, String badge, int badgeColorRes,
                   String subtitle, String valueLabel, String changeLabel, boolean up) {
        this.name = name;
        this.code = code;
        this.badge = badge;
        this.badgeColorRes = badgeColorRes;
        this.subtitle = subtitle;
        this.valueLabel = valueLabel;
        this.changeLabel = changeLabel;
        this.up = up;
        this.shares = 0;
        this.buyPrice = 0d;
    }
}
