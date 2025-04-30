package com.example.byebit.config;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.byebit.dao.WalletHandleDao;
import com.example.byebit.domain.WalletHandle;

@Database(entities = {WalletHandle.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract WalletHandleDao getWalletHandleDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "app_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
