package com.example.byebit.ui.dashboard;

import android.app.Application;
import android.util.Log; // ADD THIS

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import org.web3j.crypto.Credentials;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.byebit.domain.WalletHandle;
import com.example.byebit.repository.WalletRepository;

import java.util.List;

public class DashboardViewModel extends AndroidViewModel {

    private static final String TAG = "DashboardViewModel"; // Add a TAG for logging

    private final WalletRepository walletRepository;
    private final LiveData<List<WalletHandle>> savedWallets;

    private final ExecutorService viewModelExecutor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<CredentialsResult> _credentialsResult = new MutableLiveData<>();
    public LiveData<CredentialsResult> getCredentialsResult() {
        return _credentialsResult;
    }

    public DashboardViewModel(Application application) {
        super(application);
        walletRepository = new WalletRepository(application);
        savedWallets = walletRepository.getSavedWallets();
    }

    // Getter for the wallet list LiveData
    public LiveData<List<WalletHandle>> getSavedWallets() {
        return savedWallets;
    }

    // Add a method to get the balance for a specific wallet address
    public void fetchBalanceForAddress(String address) {
        walletRepository.getWalletBalance(address);
    }

    // ADD: Method to delete a wallet
    public void deleteWallet(WalletHandle wallet) {
        walletRepository.deleteWallet(wallet);
        // Optionally, you could add LiveData here to observe deletion status/errors
        // For now, the list will update automatically via getSavedWallets() LiveData
    }

    // ADD THIS METHOD to refresh all wallet balances
    public void refreshAllWalletBalances() {
        List<WalletHandle> currentWallets = savedWallets.getValue();
        if (currentWallets != null && !currentWallets.isEmpty()) {
            Log.d(TAG, "Refreshing balances for " + currentWallets.size() + " wallets.");
            for (WalletHandle wallet : currentWallets) {
                walletRepository.getWalletBalance(wallet.getAddress());
            }
        } else {
            Log.d(TAG, "No wallets to refresh or wallet list is null.");
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        walletRepository.shutdown();
        if (viewModelExecutor != null && !viewModelExecutor.isShutdown()) {
            viewModelExecutor.shutdown();
            Log.d(TAG, "DashboardViewModel executor service shut down.");
        }
    }

    public void loadCredentialsForWallet(WalletHandle wallet, String password) {
        _credentialsResult.setValue(CredentialsResult.loading());
        viewModelExecutor.execute(() -> {
            try {
                Credentials credentials = walletRepository.getCredentials(wallet, password);
                _credentialsResult.postValue(CredentialsResult.success(credentials));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load credentials for wallet " + wallet.getAddress(), e);
                String errorMessage = e.getMessage();
                if (e instanceof org.web3j.crypto.exception.CipherException) {
                    errorMessage = "Invalid password or corrupted wallet file.";
                } else if (e instanceof java.io.FileNotFoundException) {
                    errorMessage = "Wallet file not found.";
                }
                _credentialsResult.postValue(CredentialsResult.error(errorMessage));
            }
        });
    }

    public void clearCredentialsResult() {
        _credentialsResult.setValue(null);
    }

    public static class CredentialsResult {
        public final Credentials credentials;
        public final String error;
        public final boolean isLoading;

        private CredentialsResult(Credentials credentials, String error, boolean isLoading) {
            this.credentials = credentials;
            this.error = error;
            this.isLoading = isLoading;
        }

        public static CredentialsResult success(Credentials credentials) {
            return new CredentialsResult(credentials, null, false);
        }

        public static CredentialsResult error(String errorMessage) {
            return new CredentialsResult(null, errorMessage, false);
        }

        public static CredentialsResult loading() {
            return new CredentialsResult(null, null, true);
        }

        public boolean isSuccess() { return credentials != null && error == null && !isLoading; }
        public boolean isError() { return error != null && !isLoading; }
        public boolean isLoading() { return isLoading; }
        public Credentials getCredentials() { return credentials; }
        public String getError() { return error; }
    }
}
