package com.example.byebit.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.byebit.domain.WalletHandle;

import java.util.List;

@Dao
public interface WalletHandleDao {

    @Query("select * from wallethandle")
    List<WalletHandle> getAll();

    @Insert
    void insertAll(WalletHandle... handles);

    @Delete
    void delete(WalletHandle handle);

}
