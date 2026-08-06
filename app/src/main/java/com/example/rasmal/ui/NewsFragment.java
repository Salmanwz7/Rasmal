package com.example.rasmal.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rasmal.R;
import com.example.rasmal.adapter.NewsAdapter;
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.databinding.FragmentNewsBinding;
import com.example.rasmal.model.NewsItem;
import com.example.rasmal.model.Stock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Market news feed: cached marketaux headlines for the seeded Tadawul catalog (Story 012). */
public class NewsFragment extends Fragment {

    private FragmentNewsBinding binding;
    private ApiClient api;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = new ApiClient(requireContext());
        binding.newsList.setLayoutManager(new LinearLayoutManager(requireContext()));

        showLoading();
        api.getCompanyCatalog(new ApiClient.Callback<List<Stock>>() {
            @Override public void onSuccess(List<Stock> stocks) { loadNews(); }
            @Override public void onError(String message) { loadNews(); }
        });
    }

    private void showLoading() {
        binding.loading.setVisibility(View.VISIBLE);
        binding.newsList.setVisibility(View.GONE);
        binding.emptyNews.setVisibility(View.GONE);
    }

    private void loadNews() {
        api.getNews(new ApiClient.Callback<JSONArray>() {
            @Override public void onSuccess(JSONArray rows) {
                if (binding == null) return;
                binding.loading.setVisibility(View.GONE);
                if (rows.length() == 0) { showEmpty(); return; }

                List<NewsItem> items = new ArrayList<>();
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject r = rows.optJSONObject(i);
                    if (r == null) continue;
                    items.add(new NewsItem(
                            r.isNull("code") ? null : r.optString("code"),
                            r.optString("headline"),
                            r.isNull("url") ? null : r.optString("url"),
                            r.isNull("source") ? null : r.optString("source"),
                            r.isNull("sentiment") ? null : r.optDouble("sentiment"),
                            r.isNull("published_at") ? null : r.optString("published_at")));
                }
                binding.emptyNews.setVisibility(View.GONE);
                binding.newsList.setVisibility(View.VISIBLE);
                binding.newsList.setAdapter(new NewsAdapter(items, NewsFragment.this::openArticle));
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                binding.loading.setVisibility(View.GONE);
                showEmpty();
            }
        });
    }

    private void openArticle(NewsItem item) {
        if (item.url == null || item.url.isEmpty()) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.news_no_browser, Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmpty() {
        binding.newsList.setVisibility(View.GONE);
        binding.emptyNews.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
