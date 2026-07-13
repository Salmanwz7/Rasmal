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
import com.example.rasmal.data.MockData;
import com.example.rasmal.databinding.FragmentAddStockBinding;

public class AddStockFragment extends Fragment {

    /** Bundle key carrying the selected stock's Tadawul code to the details screen. */
    static final String ARG_STOCK_CODE = "stock_code";

    private FragmentAddStockBinding binding;
    private StockAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddStockBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new StockAdapter(MockData.availableStocks(), stock -> {
            Bundle args = new Bundle();
            args.putString(ARG_STOCK_CODE, stock.code);
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
                binding.empty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
