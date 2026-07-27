package com.example.rasmal.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.rasmal.R;
import com.example.rasmal.auth.SessionManager;
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.databinding.FragmentOnboardingRiskBinding;
import com.google.android.material.card.MaterialCardView;

public class OnboardingRiskFragment extends Fragment {

    private FragmentOnboardingRiskBinding binding;
    private ApiClient api;
    // Balanced is pre-selected in the layout, so it's the default answer.
    private String selectedRisk = "balanced";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingRiskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = new ApiClient(requireContext());

        binding.cardConservative.setOnClickListener(v -> {
            selectedRisk = "conservative";
            select(binding.cardConservative, binding.radioConservative);
        });
        binding.cardBalanced.setOnClickListener(v -> {
            selectedRisk = "balanced";
            select(binding.cardBalanced, binding.radioBalanced);
        });
        binding.cardAggressive.setOnClickListener(v -> {
            selectedRisk = "aggressive";
            select(binding.cardAggressive, binding.radioAggressive);
        });

        binding.continueBtn.setOnClickListener(v -> finishOnboarding());
    }

    /**
     * Persists the onboarding answers (risk profile + available cash) to the
     * user's Supabase profile, marks onboarding complete, and enters the app.
     * The save is best-effort: we always proceed locally, and use the app
     * context for any error toast since this fragment is torn down on navigate.
     */
    private void finishOnboarding() {
        double liquidity = getArguments() != null
                ? getArguments().getDouble(OnboardingPortfolioFragment.ARG_LIQUIDITY, 0d) : 0d;

        new SessionManager(requireContext()).setOnboarded();

        final Context appCtx = requireContext().getApplicationContext();
        api.upsertProfile(selectedRisk, liquidity, true, new ApiClient.Callback<Void>() {
            @Override public void onSuccess(Void unused) { }
            @Override public void onError(String message) {
                Toast.makeText(appCtx, "Saved on device; profile sync failed: " + message,
                        Toast.LENGTH_SHORT).show();
            }
        });

        NavHostFragment.findNavController(this)
                .navigate(R.id.action_onboardingRisk_to_dashboard);
    }

    private void select(MaterialCardView selectedCard, ImageView selectedRadio) {
        setState(binding.cardConservative, binding.radioConservative, false);
        setState(binding.cardBalanced, binding.radioBalanced, false);
        setState(binding.cardAggressive, binding.radioAggressive, false);
        setState(selectedCard, selectedRadio, true);
    }

    private void setState(MaterialCardView card, ImageView radio, boolean selected) {
        int color = ContextCompat.getColor(requireContext(),
                selected ? R.color.primary : R.color.border_subtle);
        int width = (int) (getResources().getDisplayMetrics().density * (selected ? 2 : 1));
        card.setStrokeColor(color);
        card.setStrokeWidth(width);
        radio.setImageResource(selected
                ? R.drawable.bg_radio_selected
                : R.drawable.bg_radio_unselected);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
