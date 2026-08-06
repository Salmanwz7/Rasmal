package com.example.rasmal.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rasmal.R;
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.model.Opportunity;
import com.example.rasmal.model.Stock;

import java.util.List;
import java.util.Locale;

public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.ViewHolder> {

    public interface OnOpportunityClick {
        void onClick(Opportunity opportunity);
    }

    private final List<Opportunity> items;
    private final OnOpportunityClick onClick;

    public OpportunityAdapter(List<Opportunity> items, OnOpportunityClick onClick) {
        this.items = items;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opportunity, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Opportunity o = items.get(position);
        Stock stock = ApiClient.companyByCode(o.code);

        holder.badge.setText(stock != null ? stock.badge : o.code);
        holder.badge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                holder.itemView.getContext(),
                stock != null ? stock.badgeColorRes : R.color.badge_snb)));

        holder.name.setText(String.format(Locale.US, "%s (%s)", o.name, o.code));
        holder.sector.setText(o.sector);
        holder.confidence.setText(String.format(Locale.US, "%d%%", o.confidence));
        holder.price.setText("SAR " + String.format(Locale.US, "%,.2f", o.price));

        boolean up = o.changePct >= 0;
        holder.change.setText(String.format(Locale.US, "%s%.2f%%", up ? "+" : "", o.changePct));
        holder.change.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                up ? R.color.up_green : R.color.down_red));

        holder.reason.setText(o.reasons.isEmpty() ? "" : o.reasons.get(0));
        holder.reason.setVisibility(o.reasons.isEmpty() ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> onClick.onClick(o));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView badge, name, sector, confidence, price, change, reason;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            badge = itemView.findViewById(R.id.opp_badge);
            name = itemView.findViewById(R.id.opp_name);
            sector = itemView.findViewById(R.id.opp_sector);
            confidence = itemView.findViewById(R.id.opp_confidence);
            price = itemView.findViewById(R.id.opp_price);
            change = itemView.findViewById(R.id.opp_change);
            reason = itemView.findViewById(R.id.opp_reason);
        }
    }
}
