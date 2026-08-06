package com.example.rasmal.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rasmal.R;
import com.example.rasmal.databinding.ItemStockBinding;
import com.example.rasmal.model.Stock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Search-result list of catalog stocks. Supports case-insensitive filtering + a Shariah filter. */
public class StockAdapter extends RecyclerView.Adapter<StockAdapter.VH> {

    public interface OnStockClick {
        void onStockClick(Stock stock);
    }

    private final List<Stock> all = new ArrayList<>();
    private final List<Stock> shown;
    private final OnStockClick listener;
    private String lastQuery = "";
    private boolean compliantOnly = false;

    public StockAdapter(List<Stock> all, OnStockClick listener) {
        this.all.addAll(all);
        this.shown = new ArrayList<>(all);
        this.listener = listener;
    }

    /** Replaces the full catalog (e.g. once it finishes loading) and re-applies the last filters. */
    public void setAll(List<Stock> newAll) {
        all.clear();
        all.addAll(newAll);
        applyFilters();
    }

    /** Filters by name/code/badge; returns the number of visible rows. */
    public int filter(String query) {
        lastQuery = query == null ? "" : query;
        return applyFilters();
    }

    /** Restricts the list to Shariah-compliant (نقية) stocks only, or shows all. */
    public int setCompliantOnly(boolean compliantOnly) {
        this.compliantOnly = compliantOnly;
        return applyFilters();
    }

    private int applyFilters() {
        shown.clear();
        String q = lastQuery.trim().toLowerCase(Locale.US);
        for (Stock s : all) {
            if (compliantOnly && !s.shariahCompliant) continue;
            if (!q.isEmpty()
                    && !s.name.toLowerCase(Locale.US).contains(q)
                    && !s.code.toLowerCase(Locale.US).contains(q)
                    && !s.badge.toLowerCase(Locale.US).contains(q)) continue;
            shown.add(s);
        }
        notifyDataSetChanged();
        return shown.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStockBinding b = ItemStockBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Stock s = shown.get(position);
        holder.b.badge.setText(s.badge);
        holder.b.badge.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(holder.b.getRoot().getContext(), s.badgeColorRes)));
        holder.b.name.setText(s.name);
        holder.b.subtitle.setText(String.format(Locale.US, "%s · %s", s.code, s.sector));
        holder.b.price.setText(formatPrice(s.price));
        holder.b.shariahBadge.setText(s.shariahCompliant ? R.string.shariah_compliant_short
                : R.string.shariah_mixed_short);
        holder.b.shariahBadge.setTextColor(ContextCompat.getColor(holder.b.getRoot().getContext(),
                s.shariahCompliant ? R.color.up_green : R.color.warning));
        holder.b.row.setOnClickListener(v -> {
            if (listener != null) listener.onStockClick(s);
        });
    }

    private String formatPrice(double p) {
        return (p == Math.floor(p))
                ? String.format(Locale.US, "%.0f", p)
                : String.format(Locale.US, "%.2f", p);
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemStockBinding b;

        VH(ItemStockBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
