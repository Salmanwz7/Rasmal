package com.example.rasmal.model;

/** One row from the {@code news} table — cached by market-refresh from marketaux (Story 012). */
public class NewsItem {
    public final String code;          // Tadawul code, or null for market-wide news
    public final String headline;
    public final String url;
    public final String source;
    public final Double sentiment;     // -1..1, or null if marketaux didn't score it
    public final String publishedAt;   // ISO-8601 timestamp

    public NewsItem(String code, String headline, String url, String source,
                    Double sentiment, String publishedAt) {
        this.code = code;
        this.headline = headline;
        this.url = url;
        this.source = source;
        this.sentiment = sentiment;
        this.publishedAt = publishedAt;
    }
}
