package com.example.rasmal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rasmal.R;
import com.example.rasmal.model.Alert;

import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.ViewHolder> {

    public interface OnAlertClick {
        void onClick(Alert alert, int position);
    }

    private final List<Alert> items;
    private final OnAlertClick onClick;

    public AlertAdapter(List<Alert> items, OnAlertClick onClick) {
        this.items = items;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alert a = items.get(position);
        holder.title.setText(a.title);
        holder.body.setText(a.body);
        holder.date.setText(a.earningsDate);
        holder.unreadDot.setVisibility(a.read ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(v -> onClick.onClick(a, holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, body, date;
        final View unreadDot;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.alert_title);
            body = itemView.findViewById(R.id.alert_body);
            date = itemView.findViewById(R.id.alert_date);
            unreadDot = itemView.findViewById(R.id.alert_unread_dot);
        }
    }
}