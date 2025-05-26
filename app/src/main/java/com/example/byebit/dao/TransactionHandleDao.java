package com.example.byebit.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;

import com.example.byebit.domain.TransactionHandle;
import com.example.byebit.domain.TransactionWithWallet;

import java.util.List;
import java.util.UUID;

@Dao
public interface TransactionHandleDao {

    @Query("SELECT * FROM transactionhandle ORDER BY timestamp DESC")
    LiveData<List<TransactionHandle>> getAll();

    @Query("SELECT * FROM transactionhandle WHERE walletId = :walletId ORDER BY timestamp DESC")
    LiveData<List<TransactionHandle>> getByWalletId(UUID walletId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(TransactionHandle transaction);

    @Update
    void update(TransactionHandle transaction);

    @Delete
    void delete(TransactionHandle transaction);

    @Query("SELECT * FROM transactionhandle")
    LiveData<List<TransactionWithWallet>> getAllTransactionsWithWallet();
}
