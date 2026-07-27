package com.example.rasmal.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rasmal.R;
import com.example.rasmal.adapter.PortfolioHoldingAdapter;
import com.example.rasmal.data.MockData;
import com.example.rasmal.databinding.FragmentOnboardingPortfolioBinding;
import com.example.rasmal.model.Holding;

import java.util.List;

public class OnboardingPortfolioFragment extends Fragment {

    /** Bundle key carrying the entered available cash to the risk screen. */
    static final String ARG_LIQUIDITY = "liquidity";

    private FragmentOnboardingPortfolioBinding binding;
    private List<Holding> holdings;
    private PortfolioHoldingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingPortfolioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        holdings = MockData.onboardingHoldings();
        adapter = new PortfolioHoldingAdapter(holdings);
        binding.holdingsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.holdingsList.setAdapter(adapter);

        binding.addStock.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_onboardingPortfolio_to_addStock));

        binding.continueBtn.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putDouble(ARG_LIQUIDITY, parseLiquidity());
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_onboardingPortfolio_to_risk, args);
        });
    }

    /** Reads the cash field, tolerating grouping commas and a stray "SAR"/spaces. */
    private double parseLiquidity() {
        String raw = binding.liquidity.getText().toString().replaceAll("[^0-9.]", "");
        try {
            return raw.isEmpty() ? 0d : Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
