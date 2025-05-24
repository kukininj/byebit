package com.example.byebit.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.byebit.adapter.TransactionAdapter;
import com.example.byebit.config.AppDatabase;
import com.example.byebit.dao.TransactionHandleDao;
import com.example.byebit.databinding.FragmentHomeBinding;
import com.example.byebit.domain.TransactionHandle;
import com.example.byebit.domain.GroupedTransaction;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private TransactionAdapter transactionAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();
        loadTransactions();

        return root;
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewWallets;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        transactionAdapter = new TransactionAdapter(new ArrayList<>());
        recyclerView.setAdapter(transactionAdapter);
    }

    private void loadTransactions() {
        TransactionHandleDao dao = AppDatabase.getDatabase(requireContext()).getTransactionHandleDao();
        LiveData<List<TransactionHandle>> liveTransactions = dao.getAll();

        liveTransactions.observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                List<GroupedTransaction> grouped = GroupedTransaction.groupTransactionsByDate(transactions);
                transactionAdapter.setGroupedTransactions(grouped);
            }
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
