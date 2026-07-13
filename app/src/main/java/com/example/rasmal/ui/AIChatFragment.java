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
import com.example.rasmal.data.MockData;
import com.example.rasmal.databinding.FragmentAiChatBinding;
import com.example.rasmal.model.ChatMessage;

import java.util.List;

public class AIChatFragment extends Fragment {

    private FragmentAiChatBinding binding;
    private List<ChatMessage> messages;
    private ChatAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        messages = MockData.chatMessages();
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

    /** Appends the user's message and a static mock AI reply. */
    private void sendUserMessage(String text) {
        messages.add(new ChatMessage(text, true));
        adapter.notifyItemInserted(messages.size() - 1);
        messages.add(new ChatMessage(MockData.MOCK_AI_REPLY, false));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
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
