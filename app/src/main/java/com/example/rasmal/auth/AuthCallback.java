package com.example.rasmal.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Result of an auth call, delivered on the main thread. */
public interface AuthCallback {
    /** session is null for sign-ups that still need email confirmation. */
    void onSuccess(@Nullable Session session);

    void onError(@NonNull String message);
}
