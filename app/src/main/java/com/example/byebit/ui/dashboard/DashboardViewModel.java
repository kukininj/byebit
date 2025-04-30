package com.example.byebit.ui.dashboard;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.byebit.repository.WalletRepository;

public class DashboardViewModel extends AndroidViewModel {

    private final WalletRepository walletRepository;

    private final MutableLiveData<String> mText;

    public DashboardViewModel(Application application) {
        super(application);
        walletRepository = new WalletRepository(application);
        mText = new MutableLiveData<>();
        mText.setValue("This is dashboard fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}