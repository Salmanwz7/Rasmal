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
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.data.OnboardingHoldings;
import com.example.rasmal.databinding.FragmentAddStockDetailsBinding;
import com.example.rasmal.model.Holding;
import com.example.rasmal.model.Stock;

import java.util.List;
import java.util.Locale;

public class AddStockDetailsFragment extends Fragment {

    /** When present (shares &gt; 0) the screen edits an existing holding instead of adding one. */
    static final String ARG_EDIT_SHARES = "edit_shares";
    static final String ARG_EDIT_PRICE = "edit_price";
    /** Bundle key carrying which "manage holdings" screen to pop back to on save. */
    static final String ARG_RETURN_TO = "return_to";

    private FragmentAddStockDetailsBinding binding;
    private Stock stock;
    private ApiClient api;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddStockDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = new ApiClient(requireContext());
        String code = getArguments() != null
                ? getArguments().getString(AddStockFragment.ARG_STOCK_CODE) : null;
        stock = ApiClient.companyByCode(code);

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

        int editShares = getArguments() != null ? getArguments().getInt(ARG_EDIT_SHARES, 0) : 0;
        double editPrice = getArguments() != null ? getArguments().getDouble(ARG_EDIT_PRICE, 0) : 0;
        if (editShares > 0) {
            binding.screenTitle.setText(R.string.edit_holding_title);
            binding.addBtn.setText(R.string.save_changes);
            binding.shares.setText(String.valueOf(editShares));
            binding.price.setText(formatPrice(editPrice > 0 ? editPrice : stock.price));
        } else {
            // Pre-fill avg price with the last price as a sensible starting point.
            binding.price.setText(formatPrice(stock.price));
        }

        binding.back.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.addBtn.setOnClickListener(v -> saveHolding());
    }

    private void saveHolding() {
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

        upsertLocal(new Holding(stock.name, stock.code, stock.badge, stock.badgeColorRes, shares, price));

        // Persist to Supabase (best-effort). Use the application context for the
        // toast since we navigate away immediately and this fragment may be gone.
        final android.content.Context appCtx = requireContext().getApplicationContext();
        api.upsertHolding(stock.code, shares, price, new ApiClient.Callback<Void>() {
            @Override public void onSuccess(Void unused) { }
            @Override public void onError(String message) {
                Toast.makeText(appCtx, "Saved on device; sync failed: " + message,
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Pop straight back to whichever "manage holdings" screen sent us here
        // (past the search screen, if that's how we arrived).
        int returnTo = getArguments() != null
                ? getArguments().getInt(ARG_RETURN_TO, R.id.onboardingPortfolioFragment)
                : R.id.onboardingPortfolioFragment;
        NavHostFragment.findNavController(this).popBackStack(returnTo, false);
    }

    /** Replace the in-memory holding with the same code if present, otherwise append. */
    private void upsertLocal(Holding h) {
        List<Holding> list = OnboardingHoldings.list();
        for (int i = 0; i < list.size(); i++) {
            if (h.code.equals(list.get(i).code)) {
                list.set(i, h);
                return;
            }
        }
        list.add(h);
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
