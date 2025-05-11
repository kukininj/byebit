package com.example.byebit.ui.createwallet;

import android.app.Application;
import android.util.Log;
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

    private static final String TAG = "CreateWalletViewModel";

    private final WalletRepository walletRepository;
    private final ExecutorService executorService;

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
        executorService = Executors.newFixedThreadPool(1);
    }

    public void createWallet(String name, String password, byte[] encryptedPassword, byte[] iv, boolean savePassword) {
        _isLoading.setValue(true);
        _creationResult.setValue(CreationResult.loading());

        executorService.execute(() -> {
            try {
                WalletHandle newWallet = walletRepository.createNewWallet(name, password, encryptedPassword, iv, savePassword);
                _creationResult.postValue(CreationResult.success(newWallet));
                Log.d(TAG, "Wallet creation task completed successfully.");
            } catch (InvalidAlgorithmParameterException | CipherException | NoSuchAlgorithmException | IOException | NoSuchProviderException e) {
                Log.e(TAG, "Error creating wallet", e);
                _creationResult.postValue(CreationResult.error(e.getMessage()));
            } finally {
                _isLoading.postValue(false);
                Log.d(TAG, "Wallet creation task finished.");
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
        Log.d(TAG, "ViewModel executor shut down.");
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
