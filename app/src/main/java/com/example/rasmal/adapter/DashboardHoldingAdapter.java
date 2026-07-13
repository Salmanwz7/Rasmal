package com.example.rasmal.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rasmal.R;
import com.example.rasmal.databinding.ItemHoldingBinding;
import com.example.rasmal.model.Holding;

import java.util.List;

/** Read-only holdings list for the Dashboard. */
public class DashboardHoldingAdapter
        extends RecyclerView.Adapter<DashboardHoldingAdapter.VH> {

    private final List<Holding> items;

    public DashboardHoldingAdapter(List<Holding> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHoldingBinding b = ItemHoldingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Holding h = items.get(position);
        holder.b.badge.setText(h.badge);
        holder.b.badge.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(holder.b.getRoot().getContext(), h.badgeColorRes)));
        holder.b.name.setText(h.name);
        holder.b.subtitle.setText(h.subtitle);
        holder.b.value.setText(h.valueLabel);
        holder.b.change.setText(h.changeLabel);
        holder.b.change.setTextColor(ContextCompat.getColor(
                holder.b.getRoot().getContext(),
                h.up ? R.color.up_green : R.color.down_red));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemHoldingBinding b;

        VH(ItemHoldingBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
