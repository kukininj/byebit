package com.example.byebit.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.byebit.domain.WalletHandle;
import com.example.byebit.repository.WalletRepository;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final WalletRepository walletRepository;
    private final LiveData<List<WalletHandle>> savedWallets;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        walletRepository = new WalletRepository(application);
        savedWallets = walletRepository.getSavedWallets();
    }

    public LiveData<List<WalletHandle>> getSavedWallets() {
        return savedWallets;
    }
}
