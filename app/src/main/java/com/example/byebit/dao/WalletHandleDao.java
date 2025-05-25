package com.example.byebit.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.lifecycle.LiveData; // Import LiveData
import androidx.room.Query;
import androidx.room.Update; // ADDED: Import Update

import com.example.byebit.domain.WalletHandle;

import java.util.List;

@Dao
public interface WalletHandleDao {

    // Return LiveData to observe changes automatically
    @Query("select * from wallethandle ORDER BY name ASC") // Added ORDER BY for consistency
    LiveData<List<WalletHandle>> getAll();

    @Query("SELECT * FROM WalletHandle")
    List<WalletHandle> getAllWalletsSync(); // For background sync

    // Consider using @Insert(onConflict = OnConflictStrategy.REPLACE) if needed
    @Insert
    void insertAll(WalletHandle... handles);

    @Delete
    void delete(WalletHandle handle);

    // ADDED: Method to update a WalletHandle
    @Update
    void update(WalletHandle handle);

    // ADDED: Method to find a wallet by its address synchronously
    @Query("SELECT * FROM wallethandle WHERE address = :address LIMIT 1")
    WalletHandle findByAddressSync(String address);

}
