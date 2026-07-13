package com.example.rasmal.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.rasmal.R;
import com.example.rasmal.data.MockData;
import com.example.rasmal.databinding.FragmentAddStockDetailsBinding;
import com.example.rasmal.model.Holding;
import com.example.rasmal.model.Stock;

import java.util.Locale;

public class AddStockDetailsFragment extends Fragment {

    private FragmentAddStockDetailsBinding binding;
    private Stock stock;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddStockDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String code = getArguments() != null
                ? getArguments().getString(AddStockFragment.ARG_STOCK_CODE) : null;
        stock = MockData.stockByCode(code);

        // Guard: if we somehow arrived without a valid stock, go back.
        if (stock == null) {
            NavHostFragment.findNavController(this).navigateUp();
            return;
        }

        binding.badge.setText(stock.badge);
        binding.badge.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), stock.badgeColorRes)));
        binding.name.setText(stock.name);
        binding.subtitle.setText(String.format(Locale.US, "%s · %s", stock.code, stock.sector));
        // Pre-fill avg price with the last price as a sensible starting point.
        binding.price.setText(formatPrice(stock.price));

        binding.back.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.addBtn.setOnClickListener(v -> addHolding());
    }

    private void addHolding() {
        String sharesText = binding.shares.getText().toString().trim();
        String priceText = binding.price.getText().toString().trim();

        if (TextUtils.isEmpty(sharesText) || parseInt(sharesText) <= 0) {
            Toast.makeText(requireContext(), R.string.error_enter_shares, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(priceText) || parseDouble(priceText) <= 0) {
            Toast.makeText(requireContext(), R.string.error_enter_price, Toast.LENGTH_SHORT).show();
            return;
        }

        int shares = parseInt(sharesText);
        double price = parseDouble(priceText);

        MockData.onboardingHoldings().add(
                new Holding(stock.name, stock.code, stock.badge, stock.badgeColorRes, shares, price));

        // Pop straight back to the portfolio screen (past the search screen).
        NavHostFragment.findNavController(this)
                .popBackStack(R.id.onboardingPortfolioFragment, false);
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0d; }
    }

    private String formatPrice(double p) {
        return (p == Math.floor(p))
                ? String.format(Locale.US, "%.0f", p)
                : String.format(Locale.US, "%.2f", p);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
