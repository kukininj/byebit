package com.example.byebit;

import android.app.Application;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.byebit.repository.AppPreferencesRepository;
import com.example.byebit.worker.TransactionSyncWorker;

import java.util.concurrent.TimeUnit;

public class ByeBitApplication extends Application {
    private static final String TAG = "MyApp";
    private static final String SYNC_WORK_TAG = "transaction_sync_work";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate called. Scheduling work.");
        schedulePeriodicSync();
    }

    private void schedulePeriodicSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Only run when connected to network
                // .setRequiresCharging(true) // Optional: only run when charging
                // .setRequiresBatteryNotLow(true) // Optional: don't run if battery is low
                .build();

        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                TransactionSyncWorker.class,
                12,
                TimeUnit.SECONDS)
                .setConstraints(constraints)
                .addTag(SYNC_WORK_TAG)
                // You can add initial delay if you don't want it to run immediately on app start
                // .setInitialDelay(10, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                SYNC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP, // Keep the existing work if already enqueued
                syncWorkRequest
        );

        Log.d(TAG, "Periodic transaction sync scheduled.");
    }
}
