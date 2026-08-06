package com.example.rasmal.model;

/**
 * A searchable stock from the Tadawul catalog (fetched from Supabase). Selecting
 * one during onboarding produces a {@link Holding} once the user enters shares +
 * avg price.
 */
public class Stock {

    public final String name;
    public final String code;          // Tadawul symbol, e.g. "7203"
    public final String badge;         // short ticker letters, e.g. "ELM"
    public final int badgeColorRes;    // R.color.badge_*
    public final String sector;        // e.g. "Banks"
    public final double price;         // last price, used to pre-fill avg buy price
    public final boolean shariahCompliant; // passes the business + debt-ratio screens (Story 011)

    public Stock(String name, String code, String badge, int badgeColorRes,
                 String sector, double price, boolean shariahCompliant) {
        this.name = name;
        this.code = code;
        this.badge = badge;
        this.badgeColorRes = badgeColorRes;
        this.sector = sector;
        this.price = price;
        this.shariahCompliant = shariahCompliant;
    }
}
