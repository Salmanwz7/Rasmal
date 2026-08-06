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
import com.example.rasmal.view.LineChartView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

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

        binding.bellButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_dashboard_to_alerts));
        loadUnreadCount();

        greetUser();
        loadPortfolio();
    }

    private void greetUser() {
        Session s = new SessionManager(requireContext()).load();
        String name = s != null && s.fullName != null && !s.fullName.isEmpty()
                ? s.fullName.split(" ")[0] : null;
        if (name != null) binding.greeting.setText("Salam, " + name);
    }
    private void loadUnreadCount() {
        api.getUnreadAlertCount(new ApiClient.Callback<Integer>() {
            @Override public void onSuccess(Integer count) {
                if (binding == null) return;
                binding.bellBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                binding.bellBadge.setText(count > 9 ? "9+" : String.valueOf(count));
            }
            @Override public void onError(String message) { /* badge just stays hidden */ }
        });
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
        binding.concentrationWarningCard.setVisibility(View.GONE);
        renderPerformance(0);
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
        Map<String, double[]> valueByStock = new HashMap<>();   // code -> {value}, name kept alongside
        Map<String, String> nameByCode = new HashMap<>();
        Map<String, Double> valueBySector = new HashMap<>();

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

            double positionValue = shares * price;
            holdingsValue += positionValue;
            totalCost += shares * avg;
            totalPrev += shares * prevClose;

            Stock st = ApiClient.companyByCode(code);
            String name = st != null ? st.name : code;
            String sector = st != null ? st.sector : "Other";
            int color = st != null ? st.badgeColorRes : R.color.badge_snb;
            String badge = st != null ? st.badge : code;

            valueByStock.put(code, new double[]{positionValue});
            nameByCode.put(code, name);
            valueBySector.merge(sector, positionValue, Double::sum);

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
        renderConcentrationWarning(valueByStock, nameByCode, valueBySector, holdingsValue);
        renderPerformance(holdingsValue);
    }

    /**
     * Story 014 — portfolio health analysis. Flags single-stock positions over
     * 25% of holdings value, or a single sector over 40%, using fixed
     * thresholds. Shows the single most severe issue (stock concentration
     * takes priority since it's the narrower, riskier concentration).
     */
    private static final double STOCK_CONCENTRATION_THRESHOLD = 25.0;
    private static final double SECTOR_CONCENTRATION_THRESHOLD = 40.0;

    private void renderConcentrationWarning(Map<String, double[]> valueByStock,
                                             Map<String, String> nameByCode,
                                             Map<String, Double> valueBySector,
                                             double holdingsValue) {
        if (holdingsValue <= 0) {
            binding.concentrationWarningCard.setVisibility(View.GONE);
            return;
        }

        String worstCode = null;
        double worstStockPct = 0;
        for (Map.Entry<String, double[]> e : valueByStock.entrySet()) {
            double pct = e.getValue()[0] / holdingsValue * 100;
            if (pct > worstStockPct) { worstStockPct = pct; worstCode = e.getKey(); }
        }

        String worstSector = null;
        double worstSectorPct = 0;
        for (Map.Entry<String, Double> e : valueBySector.entrySet()) {
            double pct = e.getValue() / holdingsValue * 100;
            if (pct > worstSectorPct) { worstSectorPct = pct; worstSector = e.getKey(); }
        }

        String message = null;
        if (worstCode != null && worstStockPct > STOCK_CONCENTRATION_THRESHOLD) {
            message = "You're overweight in " + nameByCode.get(worstCode) + " ("
                    + Math.round(worstStockPct) + "% of your portfolio). Consider diversifying.";
        } else if (worstSector != null && worstSectorPct > SECTOR_CONCENTRATION_THRESHOLD) {
            message = "You're heavily weighted in " + worstSector + " ("
                    + Math.round(worstSectorPct) + "% of your portfolio). Consider spreading risk across sectors.";
        }

        if (message != null) {
            binding.concentrationWarningText.setText(message);
            binding.concentrationWarningCard.setVisibility(View.VISIBLE);
        } else {
            binding.concentrationWarningCard.setVisibility(View.GONE);
        }
    }

    /**
     * Builds the performance chart entirely from real data: the user's actual
     * trade ledger (price + timestamp of every buy/sell) reconstructs the
     * holdings' value over time, and the final point is pinned to today's
     * live market value (the same holdingsValue used above). No mock points.
     */
    private void renderPerformance(double currentHoldingsValue) {
        api.getTransactions(new ApiClient.Callback<JSONArray>() {
            @Override public void onSuccess(JSONArray txs) {
                if (binding == null) return;
                buildPerformanceChart(txs, currentHoldingsValue);
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                showPerformanceEmpty();
            }
        });
    }

    private void buildPerformanceChart(JSONArray txs, double currentHoldingsValue) {
        if (txs.length() == 0) { showPerformanceEmpty(); return; }

        Map<String, Double> shares = new HashMap<>();
        Map<String, Double> lastPrice = new HashMap<>();
        List<long[]> timestamps = new ArrayList<>(); // parallel to `values`, ms since epoch
        List<Double> values = new ArrayList<>();
        long firstTs = -1;

        for (int i = 0; i < txs.length(); i++) {
            JSONObject t = txs.optJSONObject(i);
            if (t == null) continue;
            String code = t.optString("code");
            String side = t.optString("side");
            double qty = t.optDouble("shares", 0);
            double price = t.optDouble("price", 0);
            long ts = parseTimestamp(t.optString("created_at"));
            if (ts < 0) continue;
            if (firstTs == -1) firstTs = ts;

            lastPrice.put(code, price);
            double cur = shares.containsKey(code) ? shares.get(code) : 0.0;
            cur += "sell".equals(side) ? -qty : qty;
            if (cur <= 0.0001) shares.remove(code); else shares.put(code, cur);

            double val = 0;
            for (Map.Entry<String, Double> e : shares.entrySet()) {
                Double p = lastPrice.get(e.getKey());
                val += e.getValue() * (p != null ? p : 0);
            }
            timestamps.add(new long[]{ts});
            values.add(val);
        }

        if (firstTs < 0 || values.isEmpty()) { showPerformanceEmpty(); return; }

        long now = System.currentTimeMillis();
        // Pin the final point to today's real, live holdings value.
        timestamps.add(new long[]{now});
        values.add(currentHoldingsValue);

        long span = Math.max(now - firstTs, 1L);
        List<LineChartView.Point> points = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            float x = (float) ((timestamps.get(i)[0] - firstTs) / (double) span);
            points.add(new LineChartView.Point(x, values.get(i)));
        }

        binding.performanceChart.setPoints(points);
        binding.performanceChart.setVisibility(View.VISIBLE);
        binding.performanceEmpty.setVisibility(View.GONE);
        binding.performanceRangeRow.setVisibility(View.VISIBLE);

        SimpleDateFormat fmt = new SimpleDateFormat("d MMM", Locale.US);
        binding.performanceStartLabel.setText(fmt.format(new java.util.Date(firstTs)));
        binding.performanceEndLabel.setText("Today");

        double first = values.get(0);
        double pct = first > 0 ? (currentHoldingsValue - first) / first * 100 : 0;
        boolean up = pct >= 0;
        binding.performanceChange.setText(signedPct(pct));
        binding.performanceChange.setTextColor(ContextCompat.getColor(requireContext(),
                up ? R.color.up_green : R.color.down_red));
    }

    private void showPerformanceEmpty() {
        binding.performanceChart.setVisibility(View.GONE);
        binding.performanceRangeRow.setVisibility(View.GONE);
        binding.performanceEmpty.setVisibility(View.VISIBLE);
        binding.performanceChange.setText("");
    }

    /** Parses a PostgREST timestamptz string (ignores fractional seconds/offset). Returns -1 on failure. */
    private long parseTimestamp(String raw) {
        if (raw == null || raw.length() < 19) return -1;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(raw.substring(0, 19)).getTime();
        } catch (ParseException e) {
            return -1;
        }
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
