package com.example.rasmal.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/** Persists the Supabase session so the user stays signed in across app launches. */
public class SessionManager {

    private static final String PREFS = "rasmal_session";
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "full_name";
    private static final String KEY_ONBOARDED_PREFIX = "onboarded_";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(Session session) {
        prefs.edit()
                .putString(KEY_ACCESS, session.accessToken)
                .putString(KEY_REFRESH, session.refreshToken)
                .putString(KEY_USER_ID, session.userId)
                .putString(KEY_EMAIL, session.email)
                .putString(KEY_NAME, session.fullName)
                .apply();
    }

    @Nullable
    public Session load() {
        String access = prefs.getString(KEY_ACCESS, null);
        String refresh = prefs.getString(KEY_REFRESH, null);
        if (access == null || refresh == null) return null;
        return new Session(access, refresh,
                prefs.getString(KEY_USER_ID, ""),
                prefs.getString(KEY_EMAIL, ""),
                prefs.getString(KEY_NAME, ""));
    }

    public boolean isSignedIn() {
        return prefs.getString(KEY_REFRESH, null) != null;
    }

    /** Clears credentials but keeps per-user onboarded flags for returning users. */
    public void clear() {
        prefs.edit()
                .remove(KEY_ACCESS)
                .remove(KEY_REFRESH)
                .remove(KEY_USER_ID)
                .remove(KEY_EMAIL)
                .remove(KEY_NAME)
                .apply();
    }

    public boolean isOnboarded(String userId) {
        return prefs.getBoolean(KEY_ONBOARDED_PREFIX + userId, false);
    }

    public void setOnboarded() {
        String userId = prefs.getString(KEY_USER_ID, "");
        prefs.edit().putBoolean(KEY_ONBOARDED_PREFIX + userId, true).apply();
    }
}
