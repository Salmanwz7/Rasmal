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
import com.example.rasmal.adapter.AlertAdapter;
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.databinding.FragmentAlertsBinding;
import com.example.rasmal.model.Alert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AlertsFragment extends Fragment {

    private FragmentAlertsBinding binding;
    private ApiClient api;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAlertsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = new ApiClient(requireContext());
        binding.alertsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.backButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        loadAlerts();
    }

    private void loadAlerts() {
        api.getAlerts(new ApiClient.Callback<JSONArray>() {
            @Override public void onSuccess(JSONArray rows) {
                if (binding == null) return;
                if (rows.length() == 0) { showEmpty(); return; }
                List<Alert> alerts = new ArrayList<>();
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject r = rows.optJSONObject(i);
                    if (r == null) continue;
                    alerts.add(new Alert(
                            r.optLong("id"),
                            r.optString("code"),
                            r.optString("title"),
                            r.optString("body"),
                            r.optString("earnings_date"),
                            r.optBoolean("read", false)));
                }
                binding.emptyAlerts.setVisibility(View.GONE);
                binding.alertsList.setVisibility(View.VISIBLE);
                binding.alertsList.setAdapter(new AlertAdapter(alerts, (alert, position) -> {
                    if (alert.read) return;
                    api.markAlertRead(alert.id, new ApiClient.Callback<Void>() {
                        @Override public void onSuccess(Void unused) {
                            if (binding == null) return;
                            alerts.set(position, new Alert(alert.id, alert.code, alert.title,
                                    alert.body, alert.earningsDate, true));
                            binding.alertsList.getAdapter().notifyItemChanged(position);
                        }
                        @Override public void onError(String message) { /* leave as unread */ }
                    });
                }));
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                showEmpty();
            }
        });
    }

    private void showEmpty() {
        binding.alertsList.setVisibility(View.GONE);
        binding.emptyAlerts.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}