package com.example.byebit.ui.dashboard;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.byebit.domain.WalletHandle;
import com.example.byebit.repository.WalletRepository;

import java.util.List;

public class DashboardViewModel extends AndroidViewModel {

    private final WalletRepository walletRepository;
    private final LiveData<List<WalletHandle>> savedWallets;

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

    @Override
    protected void onCleared() {
        super.onCleared();
        // Shut down the repository's executors and Web3j client when the ViewModel is cleared
        walletRepository.shutdown();
    }
}
