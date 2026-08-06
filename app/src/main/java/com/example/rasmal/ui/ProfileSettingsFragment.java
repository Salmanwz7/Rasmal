package com.example.rasmal.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.rasmal.R;
import com.example.rasmal.auth.Session;
import com.example.rasmal.auth.SessionManager;
import com.example.rasmal.auth.SupabaseAuth;
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.databinding.FragmentProfileSettingsBinding;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

public class ProfileSettingsFragment extends Fragment {

    private FragmentProfileSettingsBinding binding;
    private ApiClient api;
    private SessionManager sessionManager;
    private String selectedRisk = "balanced";
    private String originalRisk = "balanced";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = new ApiClient(requireContext());
        sessionManager = new SessionManager(requireContext());

        Session s = sessionManager.load();
        if (s != null) {
            binding.etDisplayName.setText(s.fullName);
            binding.tvEmail.setText(s.email);
        }

        binding.cardConservative.setOnClickListener(v -> select("conservative"));
        binding.cardBalanced.setOnClickListener(v -> select("balanced"));
        binding.cardAggressive.setOnClickListener(v -> select("aggressive"));

        loadCurrentRisk();

        binding.btnSave.setOnClickListener(v -> saveChanges());
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.btnLogout.setOnClickListener(v -> {
            if (s != null) {
                SupabaseAuth.getInstance().signOut(s.accessToken);
            }
            sessionManager.clear();
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_profileSettings_to_signIn);
        });
    }

    private void loadCurrentRisk() {
        binding.btnSave.setEnabled(false);
        api.getProfile(new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject profile) {
                if (!isAdded()) return;
                String risk = profile.optString("risk_profile", "balanced");
                originalRisk = risk;
                select(risk);
                binding.btnSave.setEnabled(true);
            }
            @Override public void onError(String message) {
                if (!isAdded()) return;
                select(originalRisk);
                binding.btnSave.setEnabled(true);
                Toast.makeText(requireContext(), "Couldn't load risk profile: " + message,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveChanges() {
        String name = binding.etDisplayName.getText().toString().trim();
        if (name.isEmpty()) {
            binding.etDisplayName.setError("Name can't be empty");
            return;
        }

        sessionManager.updateFullName(name);

        if (selectedRisk.equals(originalRisk)) {
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(ProfileSettingsFragment.this).navigateUp();
            return;
        }

        binding.btnSave.setEnabled(false);
        api.updateRiskProfile(selectedRisk, new ApiClient.Callback<Void>() {
            @Override public void onSuccess(Void unused) {
                if (!isAdded()) return;
                originalRisk = selectedRisk;
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(ProfileSettingsFragment.this).navigateUp();
            }
            @Override public void onError(String message) {
                if (!isAdded()) return;
                binding.btnSave.setEnabled(true);
                Toast.makeText(requireContext(), "Couldn't save risk profile: " + message,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void select(String risk) {
        selectedRisk = risk;
        setState(binding.cardConservative, binding.radioConservative, risk.equals("conservative"));
        setState(binding.cardBalanced, binding.radioBalanced, risk.equals("balanced"));
        setState(binding.cardAggressive, binding.radioAggressive, risk.equals("aggressive"));
    }

    private void setState(MaterialCardView card, ImageView radio, boolean selected) {
        int color = ContextCompat.getColor(requireContext(),
                selected ? R.color.primary : R.color.border_subtle);
        int width = (int) (getResources().getDisplayMetrics().density * (selected ? 2 : 1));
        card.setStrokeColor(color);
        card.setStrokeWidth(width);
        radio.setImageResource(selected
                ? R.drawable.bg_radio_selected
                : R.drawable.bg_radio_unselected);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}