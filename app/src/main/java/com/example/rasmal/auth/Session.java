package com.example.rasmal.auth;

public class Session {
    public final String accessToken;
    public final String refreshToken;
    public final String userId;
    public final String email;
    public final String fullName;

    public Session(String accessToken, String refreshToken,
                   String userId, String email, String fullName) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
    }
}
