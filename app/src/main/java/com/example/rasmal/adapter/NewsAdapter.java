package com.example.rasmal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rasmal.R;
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.model.NewsItem;
import com.example.rasmal.model.Stock;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

    public interface OnNewsClick {
        void onClick(NewsItem item);
    }

    private final List<NewsItem> items;
    private final OnNewsClick onClick;

    public NewsAdapter(List<NewsItem> items, OnNewsClick onClick) {
        this.items = items;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NewsItem n = items.get(position);
        Stock stock = ApiClient.companyByCode(n.code);

        holder.tag.setText(stock != null ? stock.badge : holder.itemView.getContext()
                .getString(R.string.news_market_wide));
        holder.headline.setText(n.headline);

        String date = formatDate(n.publishedAt);
        holder.meta.setText(n.source != null && !n.source.isEmpty()
                ? n.source + (date.isEmpty() ? "" : " · " + date) : date);

        bindSentiment(holder, n.sentiment);
        holder.itemView.setOnClickListener(v -> onClick.onClick(n));
    }

    private void bindSentiment(ViewHolder holder, Double sentiment) {
        if (sentiment == null) {
            holder.sentiment.setVisibility(View.GONE);
            return;
        }
        holder.sentiment.setVisibility(View.VISIBLE);
        if (sentiment > 0.15) {
            holder.sentiment.setText(R.string.news_sentiment_positive);
            holder.sentiment.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.up_green));
        } else if (sentiment < -0.15) {
            holder.sentiment.setText(R.string.news_sentiment_negative);
            holder.sentiment.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.down_red));
        } else {
            holder.sentiment.setText(R.string.news_sentiment_neutral);
            holder.sentiment.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.text_tertiary));
        }
    }

    /** Parses a PostgREST timestamptz string (ignores fractional seconds/offset). Empty on failure. */
    private String formatDate(String raw) {
        if (raw == null || raw.length() < 19) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            long ts = in.parse(raw.substring(0, 19)).getTime();
            return new SimpleDateFormat("d MMM yyyy", Locale.US).format(new java.util.Date(ts));
        } catch (ParseException e) {
            return "";
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tag, sentiment, headline, meta;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tag = itemView.findViewById(R.id.news_tag);
            sentiment = itemView.findViewById(R.id.news_sentiment);
            headline = itemView.findViewById(R.id.news_headline);
            meta = itemView.findViewById(R.id.news_meta);
        }
    }
}
