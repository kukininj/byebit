package com.example.byebit.ui.createtransaction;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.byebit.domain.WalletHandle;
import com.example.byebit.repository.WalletRepository;

import java.util.List;

public class CreateTransactionViewModel extends AndroidViewModel {
    private final WalletRepository walletRepository;
    private final LiveData<List<WalletHandle>> wallets;
    public CreateTransactionViewModel(Application application) {
        super(application);
        walletRepository = new WalletRepository(application);
        wallets = walletRepository.getSavedWallets();
    }

    public LiveData<List<WalletHandle>> getSavedWallets() {
        return wallets;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        walletRepository.shutdown();
    }
}