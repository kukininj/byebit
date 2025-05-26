package com.example.byebit.config;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.byebit.dao.WalletHandleDao;
import com.example.byebit.dao.TransactionHandleDao;
import com.example.byebit.domain.WalletHandle;
import com.example.byebit.domain.TransactionHandle;

// ADDED: Import BigDecimalConverter
import com.example.byebit.config.BigDecimalConverter;

@Database(entities = {WalletHandle.class, TransactionHandle.class}, version = 10)
@TypeConverters({UuidConverter.class, BigDecimalConverter.class, InstantConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract WalletHandleDao getWalletHandleDao();

    public abstract TransactionHandleDao getTransactionHandleDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "app_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

