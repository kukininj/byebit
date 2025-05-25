package com.example.byebit.ui.home;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.byebit.domain.WalletHandle;
import com.example.byebit.repository.WalletRepository;
import com.example.byebit.worker.TransactionSyncWorker;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private static final String TAG = "HomeViewModel";
    private static final String SYNC_WORK_TAG = "transaction_sync_work_onetime"; // Unique tag for one-time work

    private final WorkManager workManager;
    private final LiveData<List<WorkInfo>> syncWorkInfoLiveData;

    public HomeViewModel(Application application) {
        super(application);
        workManager = WorkManager.getInstance(application);
        // Observe work by tag. We'll only have one unique work with this tag at a time.
        syncWorkInfoLiveData = workManager.getWorkInfosForUniqueWorkLiveData(SYNC_WORK_TAG);
    }

    public LiveData<List<WorkInfo>> getSyncWorkInfoLiveData() {
        return syncWorkInfoLiveData;
    }

    /**
     * Enqueues a one-time work request to sync transactions.
     */
    public void refreshTransactions() {
        Log.d(TAG, "Triggering manual transaction refresh.");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(TransactionSyncWorker.class)
                .setConstraints(constraints)
                .addTag(SYNC_WORK_TAG)
                // You can set backoff policy if you want retries
                // .setBackoffCriteria(BackoffPolicy.LINEAR, OneTimeWorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .build();

        // Enqueue unique work. If work with this name is already running, keep it.
        workManager.enqueueUniqueWork(
                SYNC_WORK_TAG,
                ExistingWorkPolicy.REPLACE, // REPLACE means if old work is running, cancel it and start new. KEEP would let old one finish.
                syncWorkRequest
        );
    }
}
