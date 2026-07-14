package com.example.rasmal.auth;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.rasmal.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Thin client for the Supabase Auth (GoTrue) REST API. Callbacks run on the main thread. */
public class SupabaseAuth {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String AUTH_BASE = BuildConfig.SUPABASE_URL + "/auth/v1";

    private static SupabaseAuth instance;

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static synchronized SupabaseAuth getInstance() {
        if (instance == null) instance = new SupabaseAuth();
        return instance;
    }

    public void signUp(String fullName, String email, String password, AuthCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
            body.put("data", new JSONObject().put("full_name", fullName));
        } catch (JSONException e) {
            callback.onError("Unexpected error building request.");
            return;
        }
        execute(buildRequest(AUTH_BASE + "/signup", body), callback);
    }

    public void signIn(String email, String password, AuthCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (JSONException e) {
            callback.onError("Unexpected error building request.");
            return;
        }
        execute(buildRequest(AUTH_BASE + "/token?grant_type=password", body), callback);
    }

    public void refresh(String refreshToken, AuthCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("refresh_token", refreshToken);
        } catch (JSONException e) {
            callback.onError("Unexpected error building request.");
            return;
        }
        execute(buildRequest(AUTH_BASE + "/token?grant_type=refresh_token", body), callback);
    }

    /** Revokes the session server-side. Fire-and-forget: local sign-out proceeds regardless. */
    public void signOut(String accessToken) {
        Request request = new Request.Builder()
                .url(AUTH_BASE + "/logout")
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create(new byte[0], null))
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                response.close();
            }
        });
    }

    private Request buildRequest(String url, JSONObject body) {
        return new Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                .post(RequestBody.create(body.toString(), JSON))
                .build();
    }

    private void execute(Request request, AuthCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "Network error. Check your connection and try again.");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response r = response) {
                    String raw = r.body() != null ? r.body().string() : "";
                    JSONObject json = raw.isEmpty() ? new JSONObject() : new JSONObject(raw);
                    if (r.isSuccessful()) {
                        Session session = parseSession(json);
                        mainHandler.post(() -> callback.onSuccess(session));
                    } else {
                        postError(callback, friendlyError(json));
                    }
                } catch (IOException | JSONException e) {
                    postError(callback, "Unexpected response from server.");
                }
            }
        });
    }

    @Nullable
    private Session parseSession(JSONObject json) {
        String accessToken = json.optString("access_token", "");
        if (accessToken.isEmpty()) return null;

        JSONObject user = json.optJSONObject("user");
        String userId = "";
        String email = "";
        String fullName = "";
        if (user != null) {
            userId = user.optString("id", "");
            email = user.optString("email", "");
            JSONObject meta = user.optJSONObject("user_metadata");
            if (meta != null) fullName = meta.optString("full_name", "");
        }
        return new Session(accessToken, json.optString("refresh_token", ""),
                userId, email, fullName);
    }

    private String friendlyError(JSONObject json) {
        String code = json.optString("error_code", "");
        if (code.isEmpty()) code = json.optString("code", "");
        switch (code) {
            case "email_not_confirmed":
                return "Please confirm your email first — check your inbox for the link.";
            case "invalid_credentials":
                return "Incorrect email or password.";
            case "user_already_exists":
            case "email_exists":
                return "An account with this email already exists. Try signing in.";
            case "weak_password":
                return "Password is too weak. Use at least 6 characters.";
            case "over_email_send_rate_limit":
                return "Too many attempts. Please wait a minute and try again.";
        }
        String msg = json.optString("error_description", "");
        if (msg.isEmpty()) msg = json.optString("msg", "");
        if (msg.isEmpty()) msg = json.optString("message", "");
        return msg.isEmpty() ? "Something went wrong. Please try again." : msg;
    }

    private void postError(AuthCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
