package com.example.rasmal.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.rasmal.databinding.FragmentPortfolioBinding;
import com.example.rasmal.model.Holding;
import com.example.rasmal.model.Stock;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lets a signed-in user manage liquidity and holdings any time (not just onboarding). */
public class PortfolioFragment extends Fragment {

    private FragmentPortfolioBinding binding;
    private List<Holding> holdings;
    private PortfolioHoldingAdapter adapter;
    private ApiClient api;
    private double liquidity = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPortfolioBinding.inflate(inflater, container, false);
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

        binding.addStock.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt(AddStockFragment.ARG_RETURN_TO, R.id.portfolioFragment);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_portfolio_to_addStock, args);
        });

        binding.editLiquidity.setOnClickListener(v -> showEditLiquidityDialog());

        loadLiquidity();
        loadHoldings();
    }

    private void loadLiquidity() {
        api.getProfile(new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject profile) {
                if (binding == null) return;
                double liq = profile.optDouble("liquidity", -1);
                if (liq >= 0) {
                    liquidity = liq;
                    binding.liquidityValue.setText("SAR " + money(liquidity));
                }
            }
            @Override public void onError(String message) { /* keep last-known value */ }
        });
    }

    private void showEditLiquidityDialog() {
        View form = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_liquidity, null);
        EditText input = form.findViewById(R.id.liquidity_input);
        input.setText(money(liquidity));

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Rasmal_AlertDialog)
                .setTitle(R.string.edit_liquidity_title)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save_changes, (d, w) -> {
                    double newLiquidity = parseLiquidity(input.getText().toString());
                    if (newLiquidity < 0) {
                        Toast.makeText(requireContext(), R.string.error_enter_liquidity,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    liquidity = newLiquidity;
                    binding.liquidityValue.setText("SAR " + money(liquidity));
                    api.updateLiquidity(liquidity, new ApiClient.Callback<Void>() {
                        @Override public void onSuccess(Void unused) { }
                        @Override public void onError(String message) {
                            if (binding == null) return;
                            Toast.makeText(requireContext(),
                                    getString(R.string.liquidity_update_failed, message),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    /** Tolerates grouping commas and a stray "SAR"/spaces; negative means "invalid". */
    private double parseLiquidity(String raw) {
        String digits = raw.replaceAll("[^0-9.]", "");
        if (TextUtils.isEmpty(digits)) return -1;
        try {
            return Double.parseDouble(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String money(double v) {
        return NumberFormat.getNumberInstance(Locale.US).format(Math.round(v));
    }

    /** Loads the company catalog (for names/badges), then the user's real holdings. */
    private void loadHoldings() {
        api.getCompanyCatalog(new ApiClient.Callback<List<Stock>>() {
            @Override public void onSuccess(List<Stock> stocks) { fetchHoldings(); }
            @Override public void onError(String message) { fetchHoldings(); }
        });
    }

    private void fetchHoldings() {
        api.getHoldings(new ApiClient.Callback<JSONArray>() {
            @Override public void onSuccess(JSONArray rows) {
                if (binding == null) return;
                List<Holding> real = new ArrayList<>();
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject h = rows.optJSONObject(i);
                    if (h == null) continue;
                    String code = h.optString("code");
                    int shares = (int) h.optDouble("shares", 0);
                    double avg = h.optDouble("avg_price", 0);
                    Stock st = ApiClient.companyByCode(code);
                    String name = st != null ? st.name : code;
                    String badge = st != null ? st.badge : code;
                    int color = st != null ? st.badgeColorRes : R.color.badge_snb;
                    real.add(new Holding(name, code, badge, color, shares, avg));
                }
                OnboardingHoldings.setAll(real);
                adapter.notifyDataSetChanged();
                showList(!real.isEmpty());
                if (real.isEmpty()) binding.empty.setText(R.string.no_holdings_yet);
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                binding.empty.setText(getString(R.string.dashboard_load_error));
                showList(false);
            }
        });
    }

    private void showList(boolean hasHoldings) {
        binding.holdingsList.setVisibility(hasHoldings ? View.VISIBLE : View.GONE);
        binding.empty.setVisibility(hasHoldings ? View.GONE : View.VISIBLE);
    }

    /** Opens the details screen pre-filled with this holding's shares + price. */
    private void editHolding(Holding h) {
        Bundle args = new Bundle();
        args.putString(AddStockFragment.ARG_STOCK_CODE, h.code);
        args.putInt(AddStockDetailsFragment.ARG_EDIT_SHARES, h.shares);
        args.putDouble(AddStockDetailsFragment.ARG_EDIT_PRICE, h.buyPrice);
        args.putInt(AddStockDetailsFragment.ARG_RETURN_TO, R.id.portfolioFragment);
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_portfolio_to_details, args);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
