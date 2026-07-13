package com.example.rasmal.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.rasmal.R;
import com.example.rasmal.data.MockData;
import com.example.rasmal.databinding.FragmentAiRecommendationBinding;
import com.google.android.material.snackbar.Snackbar;

public class AIRecommendationFragment extends Fragment {

    private FragmentAiRecommendationBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiRecommendationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Stat grid
        binding.statAmount.label.setText("Amount to invest");
        binding.statAmount.value.setText("SAR 20,000");
        binding.statRange.label.setText("Buy range");
        binding.statRange.value.setText("990 - 1,020");
        binding.statTarget.label.setText("Target price");
        binding.statTarget.value.setText("SAR 1,200");
        binding.statStop.label.setText("Stop loss");
        binding.statStop.value.setText("SAR 940");
        binding.statStop.value.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.down_red));

        buildReasons();

        binding.back.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());
        binding.laterBtn.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());
        binding.boughtBtn.setOnClickListener(v ->
                Snackbar.make(v, "Trade recorded — added to your portfolio.",
                        Snackbar.LENGTH_SHORT).show());
    }

    private void buildReasons() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        float density = getResources().getDisplayMetrics().density;
        for (String reason : MockData.AI_REASONS) {
            TextView tv = new TextView(requireContext());
            tv.setText("•  " + reason);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tv.setTextSize(14f);
            tv.setLineSpacing(3f * density, 1f);
            tv.setGravity(Gravity.START);
            tv.setPadding(0, (int) (6 * density), 0, (int) (6 * density));
            binding.reasonsContainer.addView(tv);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
