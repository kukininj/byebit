package com.example.byebit.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.lifecycle.LiveData; // Import LiveData
import androidx.room.Query;

import com.example.byebit.domain.WalletHandle;

import java.util.List;

@Dao
public interface WalletHandleDao {

    // Return LiveData to observe changes automatically
    @Query("select * from wallethandle ORDER BY name ASC") // Added ORDER BY for consistency
    LiveData<List<WalletHandle>> getAll();

    // Consider using @Insert(onConflict = OnConflictStrategy.REPLACE) if needed
    @Insert
    void insertAll(WalletHandle... handles);

    @Delete
    void delete(WalletHandle handle);

}
