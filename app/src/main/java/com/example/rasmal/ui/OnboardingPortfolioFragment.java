package com.example.rasmal.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rasmal.R;
import com.example.rasmal.adapter.PortfolioHoldingAdapter;
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.data.OnboardingHoldings;
import com.example.rasmal.databinding.FragmentOnboardingPortfolioBinding;
import com.example.rasmal.model.Holding;

import java.util.List;

public class OnboardingPortfolioFragment extends Fragment {

    /** Bundle key carrying the entered available cash to the risk screen. */
    static final String ARG_LIQUIDITY = "liquidity";

    private FragmentOnboardingPortfolioBinding binding;
    private List<Holding> holdings;
    private PortfolioHoldingAdapter adapter;
    private ApiClient api;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingPortfolioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = new ApiClient(requireContext());
        holdings = OnboardingHoldings.list();
        adapter = new PortfolioHoldingAdapter(holdings, new PortfolioHoldingAdapter.Listener() {
            @Override public void onEdit(Holding h) { editHolding(h); }
            @Override public void onRemove(Holding h) { removeHolding(h); }
        });
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

    /** Opens the details screen pre-filled with this holding's shares + price. */
    private void editHolding(Holding h) {
        Bundle args = new Bundle();
        args.putString(AddStockFragment.ARG_STOCK_CODE, h.code);
        args.putInt(AddStockDetailsFragment.ARG_EDIT_SHARES, h.shares);
        args.putDouble(AddStockDetailsFragment.ARG_EDIT_PRICE, h.buyPrice);
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_onboardingPortfolio_to_details, args);
    }

    /** The row was already dropped locally; delete it from Supabase (best-effort). */
    private void removeHolding(Holding h) {
        final Context appCtx = requireContext().getApplicationContext();
        api.deleteHolding(h.code, new ApiClient.Callback<Void>() {
            @Override public void onSuccess(Void unused) { }
            @Override public void onError(String message) {
                Toast.makeText(appCtx, "Removed on device; sync failed: " + message,
                        Toast.LENGTH_SHORT).show();
            }
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
