package com.example.byebit.config;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.byebit.dao.WalletHandleDao;
import com.example.byebit.domain.WalletHandle;
// ADDED: Import BigDecimalConverter
import com.example.byebit.config.BigDecimalConverter;

@Database(entities = {WalletHandle.class}, version = 4) // MODIFIED: Increment version from 2 to 3
@TypeConverters({UuidConverter.class, BigDecimalConverter.class}) // MODIFIED: Add BigDecimalConverter
public abstract class AppDatabase extends RoomDatabase {

    public abstract WalletHandleDao getWalletHandleDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "app_database")
                            .fallbackToDestructiveMigration() // ADDED: Handle schema changes
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
