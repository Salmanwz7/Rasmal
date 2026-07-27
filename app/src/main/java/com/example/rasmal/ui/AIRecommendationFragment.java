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
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.data.MockData;
import com.example.rasmal.databinding.FragmentAiRecommendationBinding;
import com.example.rasmal.model.Recommendation;
import com.example.rasmal.model.Stock;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;

/** Shows the top AI recommendation from the backend scoring engine. */
public class AIRecommendationFragment extends Fragment {

    // Fallbacks used only until the user's saved profile loads (or if it's missing).
    private static final String DEFAULT_RISK = "balanced";
    private static final double DEFAULT_LIQUIDITY = 100000d;

    private FragmentAiRecommendationBinding binding;
    private ApiClient api;
    private Recommendation rec;

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
        binding.boughtBtn.setOnClickListener(v -> recordTrade());

        showLoading();
        loadProfileThenRecommend();
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
        binding.statAmount.value.setText("…");
        binding.statRange.value.setText("…");
        binding.statTarget.value.setText("…");
        binding.statStop.value.setText("…");
    }

    private void loadRecommendation() {
        api.getRecommendation(riskProfile, liquidity, new ApiClient.Callback<JSONObject>() {
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
                binding.confidencePill.setText(R.string.buy_confidence);
            }
        });
    }

    private void bind(Recommendation r) {
        Stock st = MockData.stockByCode(r.code);
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

    /** "I bought this" → persist a holding sized from the suggested amount. */
    private void recordTrade() {
        if (rec == null) return;
        double price = rec.buyHigh > 0 ? rec.buyHigh : rec.target;
        int shares = price > 0 ? (int) Math.max(1, Math.round(rec.amount / price)) : 1;
        api.upsertHolding(rec.code, shares, price, new ApiClient.Callback<Void>() {
            @Override public void onSuccess(Void unused) {
                if (binding == null) return;
                Snackbar.make(binding.getRoot(), "Trade recorded — added to your portfolio.",
                        Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
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
