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
import com.example.rasmal.adapter.OpportunityAdapter;
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.databinding.FragmentAiOpportunitiesBinding;
import com.example.rasmal.model.Opportunity;
import com.example.rasmal.model.Stock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Lists the top AI-scored opportunities across the market; tap one for full trade details. */
public class AIOpportunitiesFragment extends Fragment {

    private static final String DEFAULT_RISK = "balanced";
    private static final double DEFAULT_LIQUIDITY = 100000d;

    private FragmentAiOpportunitiesBinding binding;
    private ApiClient api;
    private String riskProfile = DEFAULT_RISK;
    private double liquidity = DEFAULT_LIQUIDITY;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiOpportunitiesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = new ApiClient(requireContext());
        binding.opportunitiesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.backButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        showLoading();
        api.getCompanyCatalog(new ApiClient.Callback<List<Stock>>() {
            @Override public void onSuccess(List<Stock> stocks) { loadProfileThenOpportunities(); }
            @Override public void onError(String message) { loadProfileThenOpportunities(); }
        });
    }

    private void loadProfileThenOpportunities() {
        api.getProfile(new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject profile) {
                if (binding == null) return;
                String r = profile.optString("risk_profile", "");
                if (!r.isEmpty()) riskProfile = r;
                double liq = profile.optDouble("liquidity", 0);
                if (liq > 0) liquidity = liq;
                loadOpportunities();
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                loadOpportunities(); // fall back to defaults
            }
        });
    }

    private void showLoading() {
        binding.loading.setVisibility(View.VISIBLE);
        binding.opportunitiesList.setVisibility(View.GONE);
        binding.emptyOpportunities.setVisibility(View.GONE);
    }

    private void loadOpportunities() {
        api.getOpportunities(riskProfile, liquidity, new ApiClient.Callback<JSONArray>() {
            @Override public void onSuccess(JSONArray rows) {
                if (binding == null) return;
                binding.loading.setVisibility(View.GONE);
                if (rows.length() == 0) { showEmpty(); return; }

                List<Opportunity> opportunities = new ArrayList<>();
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject r = rows.optJSONObject(i);
                    if (r != null) opportunities.add(Opportunity.fromJson(r));
                }
                binding.emptyOpportunities.setVisibility(View.GONE);
                binding.opportunitiesList.setVisibility(View.VISIBLE);
                binding.opportunitiesList.setAdapter(new OpportunityAdapter(opportunities, opp -> {
                    Bundle args = new Bundle();
                    args.putString(AIRecommendationFragment.ARG_CODE, opp.code);
                    NavHostFragment.findNavController(AIOpportunitiesFragment.this)
                            .navigate(R.id.action_aiOpportunities_to_aiRec, args);
                }));
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                binding.loading.setVisibility(View.GONE);
                showEmpty();
            }
        });
    }

    private void showEmpty() {
        binding.opportunitiesList.setVisibility(View.GONE);
        binding.emptyOpportunities.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
