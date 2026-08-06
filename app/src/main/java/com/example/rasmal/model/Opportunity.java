package com.example.rasmal.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** One entry from the {@code recommendations} Edge Function's market-wide list. */
public class Opportunity {

    public final String code;
    public final String name;
    public final String sector;
    public final int confidence;      // 0..100
    public final double price;
    public final double changePct;
    public final double amount;
    public final double buyLow;
    public final double buyHigh;
    public final double target;
    public final double stop;
    public final List<String> reasons;

    public Opportunity(String code, String name, String sector, int confidence, double price,
                       double changePct, double amount, double buyLow, double buyHigh,
                       double target, double stop, List<String> reasons) {
        this.code = code;
        this.name = name;
        this.sector = sector;
        this.confidence = confidence;
        this.price = price;
        this.changePct = changePct;
        this.amount = amount;
        this.buyLow = buyLow;
        this.buyHigh = buyHigh;
        this.target = target;
        this.stop = stop;
        this.reasons = reasons;
    }

    public static Opportunity fromJson(JSONObject o) {
        List<String> reasons = new ArrayList<>();
        JSONArray arr = o.optJSONArray("reasons");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                String r = arr.optString(i, null);
                if (r != null && !r.isEmpty()) reasons.add(r);
            }
        }
        return new Opportunity(
                o.optString("code", ""),
                o.optString("name", ""),
                o.optString("sector", ""),
                o.optInt("confidence", 0),
                o.optDouble("price", 0),
                o.optDouble("change_pct", 0),
                o.optDouble("amount", 0),
                o.optDouble("buy_low", 0),
                o.optDouble("buy_high", 0),
                o.optDouble("target", 0),
                o.optDouble("stop", 0),
                reasons);
    }
}
