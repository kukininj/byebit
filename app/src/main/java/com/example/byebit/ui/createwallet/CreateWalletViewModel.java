package com.example.byebit.ui.createwallet;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.byebit.domain.WalletHandle;
import com.example.byebit.repository.WalletRepository;

import org.web3j.crypto.exception.CipherException;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateWalletViewModel extends AndroidViewModel {

    private final WalletRepository walletRepository;
    private final ExecutorService executorService; // Executor for background tasks

    // LiveData to communicate the result of the creation operation
    private final MutableLiveData<CreationResult> _creationResult = new MutableLiveData<>();
    public LiveData<CreationResult> getCreationResult() {
        return _creationResult;
    }

    // LiveData to indicate loading state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }


    public CreateWalletViewModel(Application application) {
        super(application);
        walletRepository = new WalletRepository(application);
        // Use a fixed thread pool for background operations (WalletUtils calls)
        executorService = Executors.newFixedThreadPool(2); // Adjust pool size as needed
    }

    public void createWallet(String name, String password, byte[] encryptedPassword, byte[] iv) {
        _isLoading.setValue(true); // Indicate loading started
        _creationResult.setValue(CreationResult.loading()); // Set loading state

        executorService.execute(() -> {
            try {
                // Call the repository method on a background thread
                // The repository itself handles the DB insert on its own executor
                WalletHandle newWallet = walletRepository.createNewWallet(name, password, encryptedPassword, iv);
                // Post success result back to the main thread
                _creationResult.postValue(CreationResult.success(newWallet));
            } catch (InvalidAlgorithmParameterException | CipherException | NoSuchAlgorithmException | IOException | NoSuchProviderException e) {
                // Post error result back to the main thread
                _creationResult.postValue(CreationResult.error(e.getMessage()));
            } finally {
                // Post loading finished back to the main thread
                _isLoading.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Shut down the executor service when the ViewModel is no longer used
        executorService.shutdown();
        // Also shut down the repository's executor if it's managed here
        walletRepository.shutdown();
    }

    // Helper class to wrap creation results (Success, Error, Loading)
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
