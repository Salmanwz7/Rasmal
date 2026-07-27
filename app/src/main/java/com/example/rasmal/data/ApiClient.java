package com.example.rasmal.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.rasmal.BuildConfig;
import com.example.rasmal.auth.Session;
import com.example.rasmal.auth.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Talks to the Rasmal backend: Supabase Edge Functions (recommendations, chat)
 * and PostgREST for the user's own holdings. Every request carries the user's
 * Supabase JWT — no third-party API keys ever live in the app. Callbacks run on
 * the main thread.
 */
public class ApiClient {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SessionManager sessions;

    public ApiClient(Context context) {
        this.sessions = new SessionManager(context);
    }

    private String functions() { return BuildConfig.SUPABASE_URL + "/functions/v1"; }
    private String rest() { return BuildConfig.SUPABASE_URL + "/rest/v1"; }

    // --- Public API --------------------------------------------------------

    /** POST /functions/v1/recommendations → returns the response JSON (has "pick"). */
    public void getRecommendation(String riskProfile, double liquidity,
                                  Callback<JSONObject> cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("risk_profile", riskProfile);
            body.put("liquidity", liquidity);
        } catch (JSONException e) {
            cb.onError("Could not build request.");
            return;
        }
        Request req = signed(functions() + "/recommendations");
        if (req == null) { cb.onError("You're signed out. Please sign in again."); return; }
        enqueueObject(post(req, body), cb);
    }

    /** POST /functions/v1/chat → returns the assistant "reply" string. */
    public void chat(String message, JSONArray history, Callback<String> cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("message", message);
            if (history != null) body.put("history", history);
        } catch (JSONException e) {
            cb.onError("Could not build request.");
            return;
        }
        Request req = signed(functions() + "/chat");
        if (req == null) { cb.onError("You're signed out. Please sign in again."); return; }
        enqueueObject(post(req, body), new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject o) { cb.onSuccess(o.optString("reply", "")); }
            @Override public void onError(String m) { cb.onError(m); }
        });
    }

    /** GET /rest/v1/holdings → the current user's rows (code, shares, avg_price). */
    public void getHoldings(Callback<JSONArray> cb) {
        Request req = signed(rest() + "/holdings?select=code,shares,avg_price");
        if (req == null) { cb.onError("You're signed out. Please sign in again."); return; }
        enqueueArray(req, cb);
    }

    /** GET /rest/v1/quotes → cached market quotes (code, price, change_pct, prev_close). */
    public void getQuotes(Callback<JSONArray> cb) {
        Request req = signed(rest() + "/quotes?select=code,price,change_pct,prev_close");
        if (req == null) { cb.onError("You're signed out. Please sign in again."); return; }
        enqueueArray(req, cb);
    }

    /** GET /rest/v1/profiles → the current user's profile row, or an empty object. */
    public void getProfile(Callback<JSONObject> cb) {
        Request req = signed(rest() + "/profiles?select=risk_profile,liquidity,onboarded&limit=1");
        if (req == null) { cb.onError("You're signed out. Please sign in again."); return; }
        enqueueArray(req, new Callback<JSONArray>() {
            @Override public void onSuccess(JSONArray rows) {
                JSONObject row = rows.length() > 0 ? rows.optJSONObject(0) : null;
                cb.onSuccess(row != null ? row : new JSONObject());
            }
            @Override public void onError(String message) { cb.onError(message); }
        });
    }

    /** Upsert the current user's profile (risk appetite + available cash). */
    public void upsertProfile(String riskProfile, double liquidity, boolean onboarded,
                              Callback<Void> cb) {
        Session s = sessions.load();
        if (s == null) { cb.onError("You're signed out. Please sign in again."); return; }
        JSONObject body = new JSONObject();
        try {
            body.put("user_id", s.userId);
            body.put("risk_profile", riskProfile);
            body.put("liquidity", liquidity);
            body.put("onboarded", onboarded);
        } catch (JSONException e) {
            cb.onError("Could not build request.");
            return;
        }
        Request req = new Request.Builder()
                .url(rest() + "/profiles")
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer " + s.accessToken)
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        client.newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                postError(cb, "Network error. Check your connection.");
            }
            @Override public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) {
                try (Response r = response) {
                    if (r.isSuccessful()) main.post(() -> cb.onSuccess(null));
                    else postError(cb, "Couldn't save profile (" + r.code() + ").");
                }
            }
        });
    }

    /** Upsert one holding for the current user (unique on user_id + code). */
    public void upsertHolding(String code, double shares, double avgPrice, Callback<Void> cb) {
        Session s = sessions.load();
        if (s == null) { cb.onError("You're signed out. Please sign in again."); return; }
        JSONObject body = new JSONObject();
        try {
            body.put("user_id", s.userId);
            body.put("code", code);
            body.put("shares", shares);
            body.put("avg_price", avgPrice);
        } catch (JSONException e) {
            cb.onError("Could not build request.");
            return;
        }
        Request req = new Request.Builder()
                .url(rest() + "/holdings")
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer " + s.accessToken)
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        client.newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                postError(cb, "Network error. Check your connection.");
            }
            @Override public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) {
                try (Response r = response) {
                    if (r.isSuccessful()) main.post(() -> cb.onSuccess(null));
                    else postError(cb, "Couldn't save holding (" + r.code() + ").");
                }
            }
        });
    }

    /** Appends one executed trade to the user's ledger (Story 009). */
    public void recordTransaction(String code, String side, double shares, double price,
                                  Callback<Void> cb) {
        Session s = sessions.load();
        if (s == null) { cb.onError("You're signed out. Please sign in again."); return; }
        JSONObject body = new JSONObject();
        try {
            body.put("user_id", s.userId);
            body.put("code", code);
            body.put("side", side);
            body.put("shares", shares);
            body.put("price", price);
        } catch (JSONException e) {
            cb.onError("Could not build request.");
            return;
        }
        Request req = new Request.Builder()
                .url(rest() + "/transactions")
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer " + s.accessToken)
                .header("Prefer", "return=minimal")
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        client.newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                postError(cb, "Network error. Check your connection.");
            }
            @Override public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) {
                try (Response r = response) {
                    if (r.isSuccessful()) main.post(() -> cb.onSuccess(null));
                    else postError(cb, "Couldn't record the trade (" + r.code() + ").");
                }
            }
        });
    }

    /** Deletes the current user's holding for the given code (RLS scopes it to them). */
    public void deleteHolding(String code, Callback<Void> cb) {
        Session s = sessions.load();
        if (s == null) { cb.onError("You're signed out. Please sign in again."); return; }
        Request req = new Request.Builder()
                .url(rest() + "/holdings?code=eq." + code)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer " + s.accessToken)
                .header("Prefer", "return=minimal")
                .delete()
                .build();
        client.newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                postError(cb, "Network error. Check your connection.");
            }
            @Override public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) {
                try (Response r = response) {
                    if (r.isSuccessful()) main.post(() -> cb.onSuccess(null));
                    else postError(cb, "Couldn't remove holding (" + r.code() + ").");
                }
            }
        });
    }

    // --- Internals ---------------------------------------------------------

    /** Base request builder with apikey + bearer token, or null if signed out. */
    private Request signed(String url) {
        Session s = sessions.load();
        if (s == null || s.accessToken == null || s.accessToken.isEmpty()) return null;
        return new Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer " + s.accessToken)
                .build();
    }

    private Request post(Request base, JSONObject body) {
        return base.newBuilder()
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();
    }

    private void enqueueObject(Request req, Callback<JSONObject> cb) {
        client.newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                postError(cb, "Network error. Check your connection.");
            }
            @Override public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) {
                try (Response r = response) {
                    String raw = r.body() != null ? r.body().string() : "";
                    if (r.isSuccessful()) {
                        JSONObject o = raw.isEmpty() ? new JSONObject() : new JSONObject(raw);
                        main.post(() -> cb.onSuccess(o));
                    } else {
                        postError(cb, errorFrom(raw, r.code()));
                    }
                } catch (IOException | JSONException e) {
                    postError(cb, "Unexpected response from server.");
                }
            }
        });
    }

    private void enqueueArray(Request req, Callback<JSONArray> cb) {
        client.newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                postError(cb, "Network error. Check your connection.");
            }
            @Override public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) {
                try (Response r = response) {
                    String raw = r.body() != null ? r.body().string() : "";
                    if (r.isSuccessful()) {
                        JSONArray a = raw.isEmpty() ? new JSONArray() : new JSONArray(raw);
                        main.post(() -> cb.onSuccess(a));
                    } else {
                        postError(cb, errorFrom(raw, r.code()));
                    }
                } catch (IOException | JSONException e) {
                    postError(cb, "Unexpected response from server.");
                }
            }
        });
    }

    private String errorFrom(String raw, int code) {
        try {
            String msg = new JSONObject(raw).optString("error", "");
            if (!msg.isEmpty()) return msg;
        } catch (JSONException ignored) { }
        return "Request failed (" + code + ").";
    }

    private <T> void postError(Callback<T> cb, String message) {
        main.post(() -> cb.onError(message));
    }
}
