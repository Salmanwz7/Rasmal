package com.example.rasmal.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rasmal.R;
import com.example.rasmal.adapter.DashboardHoldingAdapter;
import com.example.rasmal.auth.Session;
import com.example.rasmal.auth.SessionManager;
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.databinding.FragmentDashboardBinding;
import com.example.rasmal.model.Holding;
import com.example.rasmal.model.Stock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private ApiClient api;
    private double liquidity = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = new ApiClient(requireContext());
        binding.holdingsList.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.aiRecCard.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_dashboard_to_aiRec));
        binding.settingsIcon.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_dashboard_to_profileSettings));

        greetUser();
        loadPortfolio();
    }

    private void greetUser() {
        Session s = new SessionManager(requireContext()).load();
        String name = s != null && s.fullName != null && !s.fullName.isEmpty()
                ? s.fullName.split(" ")[0] : null;
        if (name != null) binding.greeting.setText("Salam, " + name);
    }

    /** Loads the company catalog, then the user's saved liquidity, then holdings + live quotes. */
    private void loadPortfolio() {
        api.getCompanyCatalog(new ApiClient.Callback<List<Stock>>() {
            @Override public void onSuccess(List<Stock> stocks) { loadLiquidity(); }
            @Override public void onError(String message) { loadLiquidity(); }
        });
    }

    private void loadHoldings() {
        api.getHoldings(new ApiClient.Callback<JSONArray>() {
            @Override public void onSuccess(JSONArray holdings) {
                if (binding == null) return;
                if (holdings.length() == 0) { showEmpty(R.string.no_holdings_yet); return; }
                api.getQuotes(new ApiClient.Callback<JSONArray>() {
                    @Override public void onSuccess(JSONArray quotes) {
                        if (binding == null) return;
                        render(holdings, quotes);
                    }
                    @Override public void onError(String message) {
                        if (binding == null) return;
                        showEmpty(R.string.dashboard_load_error);
                    }
                });
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                showEmpty(R.string.dashboard_load_error);
            }
        });
    }

    private void showEmpty(int messageRes) {
        binding.holdingsList.setVisibility(View.GONE);
        binding.emptyHoldings.setText(messageRes);
        binding.emptyHoldings.setVisibility(View.VISIBLE);
        // No holdings, so total assets are just the cash on hand.
        binding.portfolioValue.setText("SAR " + money(liquidity));
        binding.portfolioDelta.setText("0.00%");
        binding.todayPnl.setText("");
        binding.totalReturnValue.setText("0.00%");
    }

    /** Loads the user's saved liquidity (Story 003), then holdings + live quotes. */
    private void loadLiquidity() {
        api.getProfile(new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject profile) {
                if (binding == null) return;
                double liq = profile.optDouble("liquidity", -1);
                if (liq >= 0) {
                    liquidity = liq;
                    binding.liquidityValue.setText(money(liquidity));
                }
                loadHoldings();
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                loadHoldings(); // still show holdings even if the profile fetch failed
            }
        });
    }

    private void render(JSONArray holdings, JSONArray quotes) {
        Map<String, JSONObject> quoteByCode = new HashMap<>();
        for (int i = 0; i < quotes.length(); i++) {
            JSONObject q = quotes.optJSONObject(i);
            if (q != null) quoteByCode.put(q.optString("code"), q);
        }

        List<Holding> rows = new ArrayList<>();
        double holdingsValue = 0, totalCost = 0, totalPrev = 0;

        for (int i = 0; i < holdings.length(); i++) {
            JSONObject h = holdings.optJSONObject(i);
            if (h == null) continue;
            String code = h.optString("code");
            double shares = h.optDouble("shares", 0);
            double avg = h.optDouble("avg_price", 0);

            JSONObject q = quoteByCode.get(code);
            double price = q != null ? q.optDouble("price", avg) : avg;
            double changePct = q != null ? q.optDouble("change_pct", 0) : 0;
            double prevClose = q != null ? q.optDouble("prev_close", 0) : 0;
            // Fall back to reconstructing yesterday's close from the day's % move.
            if (prevClose <= 0) prevClose = changePct > -100 ? price / (1 + changePct / 100) : price;

            holdingsValue += shares * price;
            totalCost += shares * avg;
            totalPrev += shares * prevClose;

            Stock st = ApiClient.companyByCode(code);
            String name = st != null ? st.name : code;
            String sector = st != null ? st.sector : "";
            int color = st != null ? st.badgeColorRes : R.color.badge_snb;
            String badge = st != null ? st.badge : code;

            rows.add(new Holding(
                    name, code, badge, color,
                    code + " · " + sector,
                    "SAR " + money(shares * price),
                    signedPct(changePct),
                    changePct >= 0));
        }

        binding.emptyHoldings.setVisibility(View.GONE);
        binding.holdingsList.setVisibility(View.VISIBLE);
        binding.holdingsList.setAdapter(new DashboardHoldingAdapter(rows));
        // Total portfolio value = cash on hand + current market value of holdings.
        binding.portfolioValue.setText("SAR " + money(liquidity + holdingsValue));

        renderTodayPnl(holdingsValue - totalPrev, totalPrev);
        renderTotalReturn(holdingsValue - totalCost, totalCost);
    }

    /** Hero pill (today's %) + adjacent SAR amount, coloured for up/down. */
    private void renderTodayPnl(double pnl, double prevValue) {
        boolean up = pnl >= 0;
        double pct = prevValue > 0 ? pnl / prevValue * 100 : 0;

        binding.portfolioDelta.setText(signedPct(pct));
        binding.portfolioDelta.setBackgroundResource(
                up ? R.drawable.bg_pill_green : R.drawable.bg_pill_red);
        binding.portfolioDelta.setTextColor(up
                ? ContextCompat.getColor(requireContext(), R.color.on_primary) : Color.WHITE);

        binding.todayPnl.setText((up ? "+ " : "− ") + "SAR " + money(Math.abs(pnl)) + " today");
        binding.todayPnl.setTextColor(ContextCompat.getColor(requireContext(),
                up ? R.color.up_green : R.color.down_red));
    }

    /** Total return card: percentage since cost basis, coloured for gain/loss. */
    private void renderTotalReturn(double gain, double cost) {
        boolean up = gain >= 0;
        double pct = cost > 0 ? gain / cost * 100 : 0;
        binding.totalReturnValue.setText(signedPct(pct));
        binding.totalReturnValue.setTextColor(ContextCompat.getColor(requireContext(),
                up ? R.color.up_green : R.color.down_red));
    }

    private String signedPct(double v) {
        return (v >= 0 ? "+" : "") + fmt2(v) + "%";
    }

    private String money(double v) {
        return NumberFormat.getNumberInstance(Locale.US).format(Math.round(v));
    }

    private String fmt2(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
