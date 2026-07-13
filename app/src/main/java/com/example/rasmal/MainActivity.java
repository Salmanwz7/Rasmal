package com.example.rasmal;

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
            Toast.makeText(this, item.getTitle() + " — coming soon", Toast.LENGTH_SHORT).show();
            return false;
        });

        navController.addOnDestinationChangedListener((controller, destination, args) ->
                updateBottomNav(nav, destination));
    }

    private void updateBottomNav(BottomNavigationView nav, NavDestination destination) {
        int id = destination.getId();
        boolean show = id == R.id.dashboardFragment || id == R.id.aiChatFragment;
        nav.setVisibility(show ? View.VISIBLE : View.GONE);
        if (id == R.id.dashboardFragment) {
            nav.getMenu().findItem(R.id.nav_home).setChecked(true);
        } else if (id == R.id.aiChatFragment) {
            nav.getMenu().findItem(R.id.nav_chat).setChecked(true);
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
