package com.example.rasmal.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.rasmal.R;
import com.example.rasmal.auth.AuthCallback;
import com.example.rasmal.auth.Session;
import com.example.rasmal.auth.SessionManager;
import com.example.rasmal.auth.SupabaseAuth;
import com.example.rasmal.databinding.FragmentSignInBinding;

public class SignInFragment extends Fragment {

    private FragmentSignInBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSignInBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.signInBtn.setOnClickListener(v -> attemptSignIn());

        binding.goSignUp.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_signIn_to_signUp));
    }

    private void attemptSignIn() {
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.setError("Enter a valid email address");
            return;
        }
        if (password.isEmpty()) {
            binding.password.setError("Enter your password");
            return;
        }

        setLoading(true);
        SupabaseAuth.getInstance().signIn(email, password, new AuthCallback() {
            @Override
            public void onSuccess(@Nullable Session session) {
                if (binding == null) return;
                setLoading(false);
                if (session == null) {
                    Toast.makeText(requireContext(),
                            "Sign in failed. Please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
                SessionManager sessionManager = new SessionManager(requireContext());
                sessionManager.save(session);
                boolean onboarded = sessionManager.isOnboarded(session.userId);
                NavHostFragment.findNavController(SignInFragment.this)
                        .navigate(onboarded
                                ? R.id.action_signIn_to_dashboard
                                : R.id.action_signIn_to_onboarding);
            }

            @Override
            public void onError(@NonNull String message) {
                if (binding == null) return;
                setLoading(false);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.signInBtn.setEnabled(!loading);
        binding.signInBtn.setText(loading ? "Signing in…" : getString(R.string.sign_in));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
