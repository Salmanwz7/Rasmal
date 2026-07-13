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

        binding.continueBtn.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_onboardingPortfolio_to_risk));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
