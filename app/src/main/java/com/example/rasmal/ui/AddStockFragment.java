package com.example.rasmal.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rasmal.R;
import com.example.rasmal.adapter.StockAdapter;
import com.example.rasmal.data.ApiClient;
import com.example.rasmal.databinding.FragmentAddStockBinding;
import com.example.rasmal.model.Stock;

import java.util.ArrayList;
import java.util.List;

public class AddStockFragment extends Fragment {

    /** Bundle key carrying the selected stock's Tadawul code to the details screen. */
    static final String ARG_STOCK_CODE = "stock_code";
    /** Bundle key carrying which "manage holdings" screen to pop back to. */
    static final String ARG_RETURN_TO = "return_to";

    private FragmentAddStockBinding binding;
    private StockAdapter adapter;
    private ApiClient api;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddStockBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = new ApiClient(requireContext());
        int returnTo = getArguments() != null
                ? getArguments().getInt(ARG_RETURN_TO, R.id.onboardingPortfolioFragment)
                : R.id.onboardingPortfolioFragment;
        adapter = new StockAdapter(new ArrayList<>(), stock -> {
            Bundle args = new Bundle();
            args.putString(ARG_STOCK_CODE, stock.code);
            args.putInt(AddStockDetailsFragment.ARG_RETURN_TO, returnTo);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_addStock_to_details, args);
        });
        binding.results.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.results.setAdapter(adapter);

        binding.back.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                int count = adapter.filter(s.toString());
                binding.empty.setText(R.string.no_stocks_found);
                binding.empty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            }
        });

        loadCatalog();
    }

    private void loadCatalog() {
        api.getCompanyCatalog(new ApiClient.Callback<List<Stock>>() {
            @Override public void onSuccess(List<Stock> stocks) {
                if (binding == null) return;
                adapter.setAll(stocks);
                binding.empty.setVisibility(stocks.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                binding.empty.setText(getString(R.string.catalog_load_error, message));
                binding.empty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
