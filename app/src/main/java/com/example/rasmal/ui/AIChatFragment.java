package com.example.rasmal.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rasmal.adapter.ChatAdapter;
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.databinding.FragmentAiChatBinding;
import com.example.rasmal.model.ChatMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AIChatFragment extends Fragment {

    private FragmentAiChatBinding binding;
    private List<ChatMessage> messages;
    private ChatAdapter adapter;
    private ApiClient api;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = new ApiClient(requireContext());

        messages = new ArrayList<>();
        messages.add(new ChatMessage(
                "Salam 👋 I've loaded your portfolio and today's market. What would you like to know?",
                false));
        adapter = new ChatAdapter(messages);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        binding.messages.setLayoutManager(lm);
        binding.messages.setAdapter(adapter);
        scrollToBottom();

        binding.send.setOnClickListener(v -> {
            String text = binding.input.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;
            binding.input.setText("");
            sendUserMessage(text);
        });

        binding.chipFull.setOnClickListener(v -> sendUserMessage(binding.chipFull.getText().toString()));
        binding.chipRiskier.setOnClickListener(v -> sendUserMessage(binding.chipRiskier.getText().toString()));
        binding.chipArabic.setOnClickListener(v -> sendUserMessage(binding.chipArabic.getText().toString()));
    }

    /** Appends the user's message, shows a typing bubble, then fills it with the real AI reply. */
    private void sendUserMessage(String text) {
        JSONArray history = buildHistory();

        messages.add(new ChatMessage(text, true));
        adapter.notifyItemInserted(messages.size() - 1);

        messages.add(new ChatMessage("…", false));
        int typingIndex = messages.size() - 1;
        adapter.notifyItemInserted(typingIndex);
        scrollToBottom();

        api.chat(text, history, new ApiClient.Callback<String>() {
            @Override public void onSuccess(String reply) {
                if (binding == null) return;
                String r = (reply == null || reply.trim().isEmpty())
                        ? "Sorry, I didn't catch that. Could you rephrase?" : reply.trim();
                replaceMessage(typingIndex, r);
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                replaceMessage(typingIndex, message);
            }
        });
    }

    private void replaceMessage(int index, String text) {
        if (index < 0 || index >= messages.size()) return;
        messages.set(index, new ChatMessage(text, false));
        adapter.notifyItemChanged(index);
        scrollToBottom();
    }

    /** Prior turns (excluding the message being sent) as [{role, content}] for the backend. */
    private JSONArray buildHistory() {
        JSONArray arr = new JSONArray();
        for (ChatMessage m : messages) {
            try {
                arr.put(new JSONObject()
                        .put("role", m.fromUser ? "user" : "assistant")
                        .put("content", m.text));
            } catch (JSONException ignored) { }
        }
        return arr;
    }

    private void scrollToBottom() {
        binding.messages.post(() -> binding.messages.scrollToPosition(messages.size() - 1));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
