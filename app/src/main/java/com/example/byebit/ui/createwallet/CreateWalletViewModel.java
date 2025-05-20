package com.example.byebit.ui.createwallet;

import android.app.Application;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.byebit.domain.WalletHandle;
import com.example.byebit.repository.WalletRepository;

import org.web3j.crypto.exception.CipherException;

import java.io.IOException;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class CreateWalletViewModel extends AndroidViewModel {

    private static final String TAG = "CreateWalletViewModel";

    private final WalletRepository walletRepository;
    private final CompositeDisposable disposables = new CompositeDisposable(); // ADD THIS LINE

    private final MutableLiveData<CreationResult> _creationResult = new MutableLiveData<>();

    public LiveData<CreationResult> getCreationResult() {
        return _creationResult;
    }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);

    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }


    public CreateWalletViewModel(Application application) {
        super(application);
        walletRepository = new WalletRepository(application);
    }

    public void createWallet(String name, String password, @Nullable String privateKey, @Nullable byte[] encryptedPassword, @Nullable byte[] iv) {
        _isLoading.setValue(true);
        _creationResult.setValue(CreationResult.loading());

        Single<WalletHandle> walletHandleSingle = privateKey != null ?
                walletRepository.createNewWallet(name, password, privateKey, encryptedPassword, iv)
                : walletRepository.createNewWallet(name, password, encryptedPassword, iv);

        disposables.add(walletHandleSingle
                .subscribeOn(Schedulers.io()) // Perform the operation on a background thread
                .observeOn(Schedulers.from(getApplication().getMainExecutor())) // Observe results on the main thread
                .subscribe(
                        // onSuccess: Lambda for successful wallet creation
                        walletHandle -> {
                            _creationResult.setValue(CreationResult.success(walletHandle));
                            _isLoading.setValue(false);
                            Log.d(TAG, "Wallet creation task completed successfully via Async.");
                        },
                        // onError: Lambda for handling errors
                        throwable -> {
                            // Check for specific exceptions if needed, otherwise use generic message
                            String errorMessage = "Failed to create wallet: " + throwable.getMessage();
                            if (throwable instanceof CipherException) {
                                errorMessage = "Encryption error during wallet creation.";
                            } else if (throwable instanceof IOException) {
                                errorMessage = "File system error during wallet creation.";
                            }
                            // Add more specific error handling if required

                            Log.e(TAG, "Error creating wallet via Async", throwable);
                            _creationResult.setValue(CreationResult.error(errorMessage));
                            _isLoading.setValue(false);
                        }
                )
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel cleared, shutting down executor and clearing disposables."); // Modify log message
        disposables.clear(); // ADD THIS LINE to dispose of subscriptions
        walletRepository.shutdown();
    }

    public static class CreationResult {
        private final WalletHandle success;
        private final String error;
        private final boolean loading;

        private CreationResult(WalletHandle success, String error, boolean loading) {
            this.success = success;
            this.error = error;
            this.loading = loading;
        }

        public static CreationResult success(WalletHandle wallet) {
            return new CreationResult(wallet, null, false);
        }

        public static CreationResult error(String errorMessage) {
            return new CreationResult(null, errorMessage, false);
        }

        public static CreationResult loading() {
            return new CreationResult(null, null, true);
        }

        public boolean isSuccess() {
            return success != null;
        }

        public boolean isError() {
            return error != null;
        }

        public boolean isLoading() {
            return loading;
        }

        public WalletHandle getSuccessData() {
            return success;
        }

        public String getErrorMessage() {
            return error;
        }
    }
}
