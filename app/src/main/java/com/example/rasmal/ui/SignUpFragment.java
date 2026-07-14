package com.example.rasmal.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.rasmal.R;
import com.example.rasmal.auth.AuthCallback;
import com.example.rasmal.auth.Session;
import com.example.rasmal.auth.SessionManager;
import com.example.rasmal.auth.SupabaseAuth;
import com.example.rasmal.databinding.FragmentSignUpBinding;

public class SignUpFragment extends Fragment {

    private FragmentSignUpBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSignUpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.createAccountBtn.setOnClickListener(v -> attemptSignUp());

        binding.goSignIn.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_signUp_to_signIn));
    }

    private void attemptSignUp() {
        String name = binding.name.getText().toString().trim();
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString();

        if (name.isEmpty()) {
            binding.name.setError("Enter your name");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.setError("Enter a valid email address");
            return;
        }
        if (password.length() < 6) {
            binding.password.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);
        SupabaseAuth.getInstance().signUp(name, email, password, new AuthCallback() {
            @Override
            public void onSuccess(@Nullable Session session) {
                if (binding == null) return;
                setLoading(false);
                if (session != null) {
                    // Email confirmation disabled: signed in immediately.
                    new SessionManager(requireContext()).save(session);
                    NavHostFragment.findNavController(SignUpFragment.this)
                            .navigate(R.id.action_signUp_to_onboarding);
                } else {
                    showConfirmEmailDialog(email);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (binding == null) return;
                setLoading(false);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showConfirmEmailDialog(String email) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm your email")
                .setMessage("We sent a confirmation link to " + email
                        + ". Click the link in that email, then sign in.")
                .setCancelable(false)
                .setPositiveButton("Go to Sign In", (d, w) ->
                        NavHostFragment.findNavController(this)
                                .navigate(R.id.action_signUp_to_signIn))
                .show();
    }

    private void setLoading(boolean loading) {
        binding.createAccountBtn.setEnabled(!loading);
        binding.createAccountBtn.setText(loading ? "Creating account…" : getString(R.string.create_account_btn));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
