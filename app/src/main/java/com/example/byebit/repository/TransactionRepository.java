package com.example.byebit.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.byebit.config.AppDatabase;
import com.example.byebit.dao.TransactionHandleDao;
import com.example.byebit.domain.TransactionHandle;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {

    private static final String TAG = "TransactionRepository";

    private final TransactionHandleDao transactionHandleDao;
    private final ExecutorService databaseWriteExecutor;

    public TransactionRepository(Context context) {
        this.transactionHandleDao = AppDatabase.getDatabase(context).getTransactionHandleDao();
        this.databaseWriteExecutor = Executors.newFixedThreadPool(2);
    }

    public void insertTransaction(TransactionHandle transaction) {
        databaseWriteExecutor.execute(() -> {
            try {
                transactionHandleDao.insert(transaction);
                Log.d(TAG, "Inserted transaction: " + transaction.getId());
            } catch (Exception e) {
                Log.e(TAG, "Error inserting transaction", e);
            }
        });
    }

    public LiveData<List<TransactionHandle>> getAllTransactions() {
        return transactionHandleDao.getAll();
    }

    public LiveData<List<TransactionHandle>> getTransactionsForWallet(UUID walletId) {
        return transactionHandleDao.getByWalletOwnerId(walletId);
    }

    public void deleteTransaction(TransactionHandle transaction) {
        databaseWriteExecutor.execute(() -> {
            try {
                transactionHandleDao.delete(transaction);
                Log.d(TAG, "Deleted transaction: " + transaction.getId());
            } catch (Exception e) {
                Log.e(TAG, "Error deleting transaction", e);
            }
        });
    }

    public void shutdown() {
        databaseWriteExecutor.shutdown();
    }
}
