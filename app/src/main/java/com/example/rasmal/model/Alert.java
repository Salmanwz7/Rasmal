package com.example.rasmal.model;

/** One row from the alerts table — an in-app notification (Story 013). */
public class Alert {
    public final long id;
    public final String code;
    public final String title;
    public final String body;
    public final String earningsDate; // yyyy-MM-dd
    public final boolean read;

    public Alert(long id, String code, String title, String body,
                 String earningsDate, boolean read) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.body = body;
        this.earningsDate = earningsDate;
        this.read = read;
    }
}