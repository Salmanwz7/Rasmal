package com.example.rasmal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.rasmal.auth.AuthCallback;
import com.example.rasmal.auth.Session;
import com.example.rasmal.auth.SessionManager;
import com.example.rasmal.auth.SupabaseAuth;
import com.example.rasmal.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Single-activity host. Owns the NavHostFragment and the bottom navigation bar,
 * which is only shown on the Dashboard and AI Chat destinations.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Keep content clear of the status/navigation bars.
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        NavHostFragment host = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host);
        navController = host.getNavController();

        BottomNavigationView nav = binding.bottomNav;
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                navigateTab(R.id.dashboardFragment);
                return true;
            }
            if (id == R.id.nav_chat) {
                navigateTab(R.id.aiChatFragment);
                return true;
            }
            if (id == R.id.nav_portfolio) {
                navigateTab(R.id.portfolioFragment);
                return true;
            }
            if (id == R.id.nav_profile) {
                navigateTab(R.id.profileSettingsFragment);
                return true;
            }
            if (id == R.id.nav_news) {
                navigateTab(R.id.newsFragment);
                return true;
            }
            Toast.makeText(this, item.getTitle() + " — coming soon", Toast.LENGTH_SHORT).show();
            return false;
        });

        navController.addOnDestinationChangedListener((controller, destination, args) ->
                updateBottomNav(nav, destination));

        if (savedInstanceState == null && !handleAuthDeepLink(getIntent())) {
            restoreSession();
        }
    }

    /**
     * Handles the rasmal://auth-callback redirect from Supabase confirmation emails.
     * The tokens arrive in the URI fragment, formatted like a query string.
     */
    private boolean handleAuthDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data == null || !"rasmal".equals(data.getScheme())) return false;

        String fragment = data.getFragment();
        if (fragment == null) return false;
        Uri params = Uri.parse("x://x?" + fragment);

        String errorDescription = params.getQueryParameter("error_description");
        if (errorDescription != null) {
            Toast.makeText(this, errorDescription, Toast.LENGTH_LONG).show();
            return false;
        }

        String refreshToken = params.getQueryParameter("refresh_token");
        if (refreshToken == null) return false;

        SessionManager sessionManager = new SessionManager(this);
        SupabaseAuth.getInstance().refresh(refreshToken, new AuthCallback() {
            @Override
            public void onSuccess(@Nullable Session session) {
                if (session == null) return;
                sessionManager.save(session);
                Toast.makeText(MainActivity.this,
                        "Email confirmed, welcome!", Toast.LENGTH_LONG).show();
                navController.navigate(sessionManager.isOnboarded(session.userId)
                        ? R.id.action_signIn_to_dashboard
                        : R.id.action_signIn_to_onboarding);
            }

            @Override
            public void onError(@NonNull String message) {
                Toast.makeText(MainActivity.this,
                        "Email confirmed. Please sign in.", Toast.LENGTH_LONG).show();
            }
        });
        return true;
    }

    /** Skips the sign-in screen when a session is stored, and refreshes it in the background. */
    private void restoreSession() {
        SessionManager sessionManager = new SessionManager(this);
        Session stored = sessionManager.load();
        if (stored == null) return;

        navController.navigate(sessionManager.isOnboarded(stored.userId)
                ? R.id.action_signIn_to_dashboard
                : R.id.action_signIn_to_onboarding);

        SupabaseAuth.getInstance().refresh(stored.refreshToken, new AuthCallback() {
            @Override
            public void onSuccess(@Nullable Session session) {
                if (session != null) sessionManager.save(session);
            }

            @Override
            public void onError(@NonNull String message) {
                // Network errors are tolerated; the stored session may still be valid.
            }
        });
    }

    private void updateBottomNav(BottomNavigationView nav, NavDestination destination) {
        int id = destination.getId();
        boolean show = id == R.id.dashboardFragment || id == R.id.aiChatFragment
                || id == R.id.portfolioFragment || id == R.id.profileSettingsFragment
                || id == R.id.newsFragment;
        nav.setVisibility(show ? View.VISIBLE : View.GONE);
        if (id == R.id.dashboardFragment) {
            nav.getMenu().findItem(R.id.nav_home).setChecked(true);
        } else if (id == R.id.aiChatFragment) {
            nav.getMenu().findItem(R.id.nav_chat).setChecked(true);
        } else if (id == R.id.portfolioFragment) {
            nav.getMenu().findItem(R.id.nav_portfolio).setChecked(true);
        } else if (id == R.id.profileSettingsFragment) {
            nav.getMenu().findItem(R.id.nav_profile).setChecked(true);
        } else if (id == R.id.newsFragment) {
            nav.getMenu().findItem(R.id.nav_news).setChecked(true);
        }
    }

    /** Navigate between the two real tabs while keeping Dashboard as the base. */
    private void navigateTab(int destinationId) {
        NavDestination current = navController.getCurrentDestination();
        if (current != null && current.getId() == destinationId) return;

        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.dashboardFragment, false)
                .build();
        navController.navigate(destinationId, null, options);
    }
}
