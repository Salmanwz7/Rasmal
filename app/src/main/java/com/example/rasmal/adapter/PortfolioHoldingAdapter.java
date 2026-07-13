package com.example.rasmal.adapter;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rasmal.databinding.ItemPortfolioHoldingBinding;
import com.example.rasmal.model.Holding;

import java.util.List;
import java.util.Locale;

/** Editable holdings list used in onboarding (supports removing a row). */
public class PortfolioHoldingAdapter
        extends RecyclerView.Adapter<PortfolioHoldingAdapter.VH> {

    private final List<Holding> items;

    public PortfolioHoldingAdapter(List<Holding> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPortfolioHoldingBinding b = ItemPortfolioHoldingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Holding h = items.get(holder.getBindingAdapterPosition());
        holder.b.badge.setText(h.badge);
        holder.b.badge.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(holder.b.getRoot().getContext(), h.badgeColorRes)));
        holder.b.name.setText(String.format(Locale.US, "%s (%s)", h.name, h.code));
        holder.b.detail.setText(formatShares(h));
        holder.b.remove.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            items.remove(pos);
            notifyItemRemoved(pos);
            notifyItemRangeChanged(pos, items.size());
        });
    }

    private String formatShares(Holding h) {
        String price = (h.buyPrice == Math.floor(h.buyPrice))
                ? String.format(Locale.US, "%.0f", h.buyPrice)
                : String.format(Locale.US, "%.2f", h.buyPrice);
        return String.format(Locale.US, "%d shares @ %s SAR", h.shares, price);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemPortfolioHoldingBinding b;

        @SuppressLint("ClickableViewAccessibility")
        VH(ItemPortfolioHoldingBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
