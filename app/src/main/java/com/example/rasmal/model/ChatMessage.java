package com.example.rasmal.model;

/** A single chat bubble. */
public class ChatMessage {

    public final String text;
    public final boolean fromUser; // true = user (right/green), false = AI (left/surface)

    public ChatMessage(String text, boolean fromUser) {
        this.text = text;
        this.fromUser = fromUser;
    }
}
