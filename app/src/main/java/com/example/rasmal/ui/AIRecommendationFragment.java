package com.example.rasmal.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.rasmal.R;
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.databinding.FragmentAiRecommendationBinding;
import com.example.rasmal.model.Recommendation;
import com.example.rasmal.model.Stock;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Shows the full AI trade write-up for one company picked from {@link AIOpportunitiesFragment}. */
public class AIRecommendationFragment extends Fragment {

    public static final String ARG_CODE = "code";

    // Fallbacks used only until the user's saved profile loads (or if it's missing).
    private static final String DEFAULT_RISK = "balanced";
    private static final double DEFAULT_LIQUIDITY = 100000d;

    private FragmentAiRecommendationBinding binding;
    private ApiClient api;
    private Recommendation rec;
    private String code;

    // Sourced from the user's saved onboarding profile (Stories 003 & 004).
    private String riskProfile = DEFAULT_RISK;
    private double liquidity = DEFAULT_LIQUIDITY;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiRecommendationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = new ApiClient(requireContext());
        code = getArguments() != null ? getArguments().getString(ARG_CODE) : null;

        binding.statAmount.label.setText("Amount to invest");
        binding.statRange.label.setText("Buy range");
        binding.statTarget.label.setText("Target price");
        binding.statStop.label.setText("Stop loss");
        binding.statStop.value.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.down_red));

        binding.back.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());
        binding.laterBtn.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());
        binding.boughtBtn.setOnClickListener(v -> showTradeDialog());

        if (code == null || code.isEmpty()) {
            Snackbar.make(binding.getRoot(), "No company selected.", Snackbar.LENGTH_LONG).show();
            return;
        }

        showLoading();
        api.getCompanyCatalog(new ApiClient.Callback<List<Stock>>() {
            @Override public void onSuccess(List<Stock> stocks) { loadProfileThenRecommend(); }
            @Override public void onError(String message) { loadProfileThenRecommend(); }
        });
    }

    /** Load the saved risk profile + liquidity, then request a recommendation. */
    private void loadProfileThenRecommend() {
        api.getProfile(new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject profile) {
                if (binding == null) return;
                String r = profile.optString("risk_profile", "");
                if (!r.isEmpty()) riskProfile = r;
                double liq = profile.optDouble("liquidity", 0);
                if (liq > 0) liquidity = liq;
                loadRecommendation();
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                loadRecommendation(); // fall back to defaults
            }
        });
    }

    private void showLoading() {
        binding.confidencePill.setText("Analyzing…");
        binding.companyName.setText("");
        binding.companySector.setText("");
        binding.statAmount.value.setText("…");
        binding.statRange.value.setText("…");
        binding.statTarget.value.setText("…");
        binding.statStop.value.setText("…");
    }

    private void loadRecommendation() {
        api.getRecommendationDetail(riskProfile, liquidity, code, new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject resp) {
                if (binding == null) return;
                JSONObject pick = resp.optJSONObject("pick");
                if (pick == null) {
                    onError("No recommendation available yet.");
                    return;
                }
                rec = Recommendation.fromJson(pick);
                bind(rec);
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                binding.confidencePill.setText(R.string.recommendation_unavailable_pill);
                binding.companyName.setText(R.string.recommendation_unavailable);
                binding.companySector.setText("");
            }
        });
    }

    private void bind(Recommendation r) {
        Stock st = ApiClient.companyByCode(r.code);
        binding.companyName.setText(st != null ? st.name : r.code);
        binding.companySector.setText(st != null ? st.sector : "");
        binding.confidencePill.setText(String.format(Locale.US, "BUY · %d%% confidence", r.confidence));

        binding.statAmount.value.setText("SAR " + money(r.amount));
        binding.statRange.value.setText(price(r.buyLow) + " – " + price(r.buyHigh));
        binding.statTarget.value.setText("SAR " + price(r.target));
        binding.statStop.value.setText("SAR " + price(r.stop));

        buildReasons(r);
    }

    private void buildReasons(Recommendation r) {
        binding.reasonsContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        if (r.narrative != null && !r.narrative.isEmpty()) {
            TextView intro = new TextView(requireContext());
            intro.setText(r.narrative);
            intro.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            intro.setTextSize(14f);
            intro.setLineSpacing(3f * density, 1f);
            intro.setPadding(0, 0, 0, (int) (10 * density));
            binding.reasonsContainer.addView(intro);
        }

        for (String reason : r.reasons) {
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

    /** Asks whether the user bought or sold, and for the actual quantity + price. */
    private void showTradeDialog() {
        if (rec == null) return;
        View form = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_trade_confirm, null);
        RadioGroup sideGroup = form.findViewById(R.id.side_group);
        EditText qtyInput = form.findViewById(R.id.quantity);
        EditText priceInput = form.findViewById(R.id.price);

        // Pre-fill with the recommendation's suggested size/price as a starting point.
        double suggestedPrice = rec.buyHigh > 0 ? rec.buyHigh : rec.target;
        int suggestedQty = suggestedPrice > 0 ? (int) Math.max(1, Math.round(rec.amount / suggestedPrice)) : 1;
        qtyInput.setText(String.valueOf(suggestedQty));
        if (suggestedPrice > 0) priceInput.setText(String.format(Locale.US, "%.2f", suggestedPrice));

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Rasmal_AlertDialog)
                .setTitle(R.string.confirm_trade_title)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    boolean sold = sideGroup.getCheckedRadioButtonId() == R.id.side_sell;
                    int qty = parseInt(qtyInput.getText().toString().trim());
                    double p = parseDouble(priceInput.getText().toString().trim());
                    if (qty <= 0) { toast(R.string.error_enter_shares); return; }
                    if (p <= 0) { toast(R.string.error_enter_price); return; }
                    applyTrade(sold, qty, p);
                })
                .show();
    }

    /**
     * Updates the portfolio for an executed trade, then appends it to the ledger.
     * Buys blend into a weighted-average cost; sells reduce the position (or close
     * it out when the remaining share count reaches zero).
     */
    private void applyTrade(boolean sold, int qty, double tradePrice) {
        if (rec == null) return;
        final String code = rec.code;
        api.getHoldings(new ApiClient.Callback<JSONArray>() {
            @Override public void onSuccess(JSONArray holdings) {
                if (binding == null) return;
                double curShares = 0, curAvg = 0;
                for (int i = 0; i < holdings.length(); i++) {
                    JSONObject h = holdings.optJSONObject(i);
                    if (h != null && code.equals(h.optString("code"))) {
                        curShares = h.optDouble("shares", 0);
                        curAvg = h.optDouble("avg_price", 0);
                        break;
                    }
                }
                if (sold) {
                    double newShares = curShares - qty;

                    if (newShares <= 0) {
                        api.deleteHolding(code, onPortfolioUpdated("sell", qty, tradePrice,
                                "Sale recorded — position closed."));
                    } else {
                        api.upsertHolding(code, newShares, curAvg, onPortfolioUpdated("sell", qty,
                                tradePrice, "Sale recorded — portfolio updated."));
                    }
                } else {
                    double newShares = curShares + qty;
                    double newAvg = newShares > 0 ? (curShares * curAvg + qty * tradePrice) / newShares : tradePrice;
                    api.upsertHolding(code, newShares, newAvg, onPortfolioUpdated("buy", qty, tradePrice,
                            "Purchase recorded — portfolio updated."));
                }
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    /** On a successful portfolio update, append the trade to the ledger and confirm. */
    private ApiClient.Callback<Void> onPortfolioUpdated(String side, int qty, double price,
                                                        String successMsg) {
        final String code = rec.code;
        return new ApiClient.Callback<Void>() {
            @Override public void onSuccess(Void unused) {
                api.recordTransaction(code, side, qty, price, new ApiClient.Callback<Void>() {
                    @Override public void onSuccess(Void u) { }
                    @Override public void onError(String m) { /* portfolio is updated; ledger is best-effort */ }
                });
                if (binding == null) return;
                Snackbar.make(binding.getRoot(), successMsg, Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        };
    }

    private void toast(int stringRes) {
        if (binding != null) Snackbar.make(binding.getRoot(), stringRes, Snackbar.LENGTH_SHORT).show();
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0d; }
    }

    private String money(double v) {
        return NumberFormat.getNumberInstance(Locale.US).format(Math.round(v));
    }

    private String price(double v) {
        return String.format(Locale.US, "%,.2f", v);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
