package com.example.byebit.repository;

import android.content.Context;

import com.example.byebit.domain.WalletHandle;

import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;

import java.io.File;
import java.util.List;

public class WalletRepository {

    private final Context context;
    private final File walletsDirectory;

    public WalletRepository(Context context) {
        this.context = context;
        this.walletsDirectory = context.getFilesDir();
    }

    public List<WalletHandle> getSavedWallets() {
        return null;
    }
}
