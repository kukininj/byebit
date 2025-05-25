package com.example.byebit.ui.home; // Adjust package

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.WorkInfo;

import com.example.byebit.SettingsActivity;
import com.example.byebit.adapter.TransactionAdapter;
import com.example.byebit.R; // Make sure this is correct
import com.example.byebit.config.AppDatabase; // Adjust package
import com.example.byebit.dao.TransactionHandleDao; // Adjust package
import com.example.byebit.domain.GroupedTransaction;
import com.example.byebit.domain.TransactionHandle; // Adjust package
import com.example.byebit.databinding.FragmentHomeBinding; // Import your generated ViewBinding class
import com.example.byebit.ui.settings.SettingsFragment; // Adjust package (if used for settings)
import com.example.byebit.ui.dialog.TransactionDetailsDialogFragment; // Adjust package

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements MenuProvider {

    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding; // For View Binding
    private TransactionAdapter transactionAdapter; // Your existing adapter
    private HomeViewModel homeViewModel; // New: ViewModel for WorkManager interaction
    // Note: TransactionHandleDao is directly accessed here, typically it should be via a Repository
    // which would then be passed to the ViewModel, which would expose the LiveData.
    // For this refactor, keeping it as is to minimally change your existing data loading.

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Setup MenuProvider (your existing menu logic)
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        // --- Swipe-to-Refresh Setup ---
        // Set up the listener for swipe-to-refresh gesture
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Swipe-to-refresh triggered. Requesting transaction sync.");
            homeViewModel.refreshTransactions(); // Trigger the WorkManager task
        });

        // Observe WorkManager's status to control the SwipeRefreshLayout spinner
        homeViewModel.getSyncWorkInfoLiveData().observe(getViewLifecycleOwner(), workInfos -> {
            if (workInfos == null || workInfos.isEmpty()) {
                // No work scheduled yet or info not available.
                // Could be initial state or after app restart if WorkManager hasn't reported yet.
                // Ensure spinner is off initially or if no work is active.
                binding.swipeRefreshLayout.setRefreshing(false);
                return;
            }

            // Get the first WorkInfo from the list (since we use unique work with REPLACE policy)
            WorkInfo workInfo = workInfos.get(0);
            Log.d(TAG, "WorkInfo state for sync: " + workInfo.getState());

            boolean isRunning = workInfo.getState() == WorkInfo.State.RUNNING ||
                    workInfo.getState() == WorkInfo.State.ENQUEUED; // Show spinner for enqueued/running states

            binding.swipeRefreshLayout.setRefreshing(isRunning); // Control the spinner visibility

            // Provide user feedback based on work state
            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                Toast.makeText(requireContext(), "Transactions synced successfully! " + getLifecycle().getCurrentState().name(), Toast.LENGTH_SHORT).show();
            } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                Toast.makeText(requireContext(), "Transaction sync failed. Please try again.", Toast.LENGTH_LONG).show();
            } else if (workInfo.getState() == WorkInfo.State.CANCELLED) {
                Toast.makeText(requireContext(), "Transaction sync cancelled.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Initial Data Load and Optional Sync Trigger ---
        // Observe transactions from the database (your existing data loading)
        // This will update the UI whenever data changes in the DB, whether by sync or other means.
        TransactionHandleDao dao = AppDatabase.getDatabase(requireContext()).getTransactionHandleDao();
        dao.getAll().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                // Assuming GroupedTransaction and its static method exist
                List<GroupedTransaction> grouped = GroupedTransaction.groupTransactionsByDate(transactions);
                transactionAdapter.setGroupedTransactions(grouped);

                // Optional: Trigger an initial sync if the adapter is empty,
                // and the sync work isn't already running or enqueued.
                // This provides data on first launch without requiring an immediate swipe.
                if (grouped.isEmpty()) {
                    Log.d(TAG, "No grouped transactions found, triggering initial refresh.");
                    // Post to ensure the UI is ready before showing the spinner and triggering work
                    binding.swipeRefreshLayout.post(() -> {
                        // Only set refreshing and trigger if not already refreshing/enqueued
                        if (!binding.swipeRefreshLayout.isRefreshing()) {
                            binding.swipeRefreshLayout.setRefreshing(true);
                            homeViewModel.refreshTransactions();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.home_menu, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this.getContext(), SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return false; // Return false to indicate that the event was not handled.
        // super.onOptionsItemSelected(menuItem) is deprecated for MenuProvider.
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewWallets; // Use binding
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        transactionAdapter = new TransactionAdapter(new ArrayList<>(), transaction -> {
            TransactionDetailsDialogFragment dialog = TransactionDetailsDialogFragment.newInstance(
                    transaction.getHash(), // Use getTransactionHash() if you updated TransactionHandle
                    transaction.getTimestamp().toString(),
                    transaction.getSenderAddress(),
                    transaction.getReceiverAddress(),
                    String.valueOf(transaction.getTransactionAmount()),
                    String.valueOf(transaction.getTransactionFee()),
                    transaction.getBlockchainType()
            );
            dialog.show(getParentFragmentManager(), "TransactionDetailsDialog");
        });

        recyclerView.setAdapter(transactionAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Clear binding reference
    }
}