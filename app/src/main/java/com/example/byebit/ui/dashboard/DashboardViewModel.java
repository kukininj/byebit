package com.example.byebit.ui.dashboard;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
// Remove MutableLiveData if no longer needed
// import androidx.lifecycle.MutableLiveData;

import com.example.byebit.domain.WalletHandle; // Import WalletHandle
import com.example.byebit.repository.WalletRepository;

import java.util.List; // Import List

public class DashboardViewModel extends AndroidViewModel {

    private final WalletRepository walletRepository;
    private final LiveData<List<WalletHandle>> savedWallets; // LiveData for wallets

    // Remove mText if not used elsewhere
    // private final MutableLiveData<String> mText;

    public DashboardViewModel(Application application) {
        super(application);
        walletRepository = new WalletRepository(application);
        // Initialize savedWallets by getting LiveData from the repository
        savedWallets = walletRepository.getSavedWallets();

        // Remove placeholder text initialization
        // mText = new MutableLiveData<>();
        // mText.setValue("This is dashboard fragment");
    }

    // Getter for the wallet list LiveData
    public LiveData<List<WalletHandle>> getSavedWallets() {
        return savedWallets;
    }

    // Remove getText() if mText is removed
    // public LiveData<String> getText() {
    //     return mText;
    // }
}
