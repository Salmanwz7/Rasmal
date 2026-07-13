package com.example.rasmal.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.rasmal.R;
import com.example.rasmal.databinding.FragmentOnboardingRiskBinding;
import com.google.android.material.card.MaterialCardView;

public class OnboardingRiskFragment extends Fragment {

    private FragmentOnboardingRiskBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingRiskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.cardConservative.setOnClickListener(v ->
                select(binding.cardConservative, binding.radioConservative));
        binding.cardBalanced.setOnClickListener(v ->
                select(binding.cardBalanced, binding.radioBalanced));
        binding.cardAggressive.setOnClickListener(v ->
                select(binding.cardAggressive, binding.radioAggressive));

        binding.continueBtn.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_onboardingRisk_to_dashboard));
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
