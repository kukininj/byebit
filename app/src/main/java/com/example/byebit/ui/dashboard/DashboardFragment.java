package com.example.byebit.ui.dashboard;

import android.os.Bundle;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// Remove TextView import if no longer needed
// import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Import Nullable
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager; // Import LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView; // Import RecyclerView

import com.example.byebit.adapter.WalletAdapter; // Import WalletAdapter
import com.example.byebit.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel; // Make ViewModel accessible
    private WalletAdapter walletAdapter; // Adapter for the RecyclerView

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Initialize ViewModel
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Inflate layout using view binding
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup RecyclerView
        setupRecyclerView();

        // Remove old TextView observation
        // final TextView textView = binding.textDashboard;
        // dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Observe the LiveData from the ViewModel
        dashboardViewModel.getSavedWallets().observe(getViewLifecycleOwner(), wallets -> {
            // Update the adapter's data when the wallet list changes
            if (wallets != null) {
                walletAdapter.setWallets(wallets);
            }
        });
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewWallets; // Get RecyclerView from binding
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // Set LayoutManager
        recyclerView.setHasFixedSize(true); // Optimization if item size doesn't change

        walletAdapter = new WalletAdapter(); // Create the adapter
        recyclerView.setAdapter(walletAdapter); // Set the adapter
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
