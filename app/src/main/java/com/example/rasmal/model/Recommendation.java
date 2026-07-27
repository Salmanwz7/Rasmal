package com.example.rasmal.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** The AI recommendation returned by the {@code recommendations} Edge Function. */
public class Recommendation {

    public final String code;
    public final int confidence;      // 0..100
    public final double amount;
    public final double buyLow;
    public final double buyHigh;
    public final double target;
    public final double stop;
    public final String narrative;
    public final List<String> reasons;

    public Recommendation(String code, int confidence, double amount, double buyLow,
                          double buyHigh, double target, double stop,
                          String narrative, List<String> reasons) {
        this.code = code;
        this.confidence = confidence;
        this.amount = amount;
        this.buyLow = buyLow;
        this.buyHigh = buyHigh;
        this.target = target;
        this.stop = stop;
        this.narrative = narrative;
        this.reasons = reasons;
    }

    /** Parses the {@code pick} object from the recommendations response. */
    public static Recommendation fromJson(JSONObject o) {
        List<String> reasons = new ArrayList<>();
        JSONArray arr = o.optJSONArray("reasons");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                String r = arr.optString(i, null);
                if (r != null && !r.isEmpty()) reasons.add(r);
            }
        }
        return new Recommendation(
                o.optString("code", ""),
                o.optInt("confidence", 0),
                o.optDouble("amount", 0),
                o.optDouble("buy_low", 0),
                o.optDouble("buy_high", 0),
                o.optDouble("target", 0),
                o.optDouble("stop", 0),
                o.optString("narrative", ""),
                reasons);
    }
}
