package com.example.rasmal.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rasmal.databinding.ItemChatAiBinding;
import com.example.rasmal.databinding.ItemChatUserBinding;
import com.example.rasmal.model.ChatMessage;

import java.util.List;

/** Two view-type chat list: AI (left) and user (right) bubbles. */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_AI = 0;
    private static final int TYPE_USER = 1;

    private final List<ChatMessage> items;

    public ChatAdapter(List<ChatMessage> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).fromUser ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            return new UserVH(ItemChatUserBinding.inflate(inflater, parent, false));
        }
        return new AiVH(ItemChatAiBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage m = items.get(position);
        if (holder instanceof UserVH) {
            ((UserVH) holder).b.message.setText(m.text);
        } else {
            ((AiVH) holder).b.message.setText(m.text);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AiVH extends RecyclerView.ViewHolder {
        final ItemChatAiBinding b;

        AiVH(ItemChatAiBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    static class UserVH extends RecyclerView.ViewHolder {
        final ItemChatUserBinding b;

        UserVH(ItemChatUserBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
