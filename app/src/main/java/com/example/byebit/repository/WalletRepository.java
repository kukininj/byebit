package com.example.byebit.repository;

import android.content.Context;

import com.example.byebit.config.AppDatabase;
import com.example.byebit.dao.WalletHandleDao;
import com.example.byebit.config.AppDatabase;
import androidx.lifecycle.LiveData; // Import LiveData

import com.example.byebit.config.AppDatabase;
import com.example.byebit.dao.WalletHandleDao;
import com.example.byebit.domain.WalletHandle;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;
import java.util.UUID;

public class WalletRepository {

    private final WalletHandleDao walletHandleDao;

    private final File walletsDir;

    public WalletRepository(Context context) {
        this.walletHandleDao = AppDatabase.getDatabase(context).getWalletHandleDao();
        this.walletsDir = context.getFilesDir();
    }

    // Return LiveData directly from the DAO
    public LiveData<List<WalletHandle>> getSavedWallets() {
        // Room handles background threading for LiveData queries
        return walletHandleDao.getAll();
    }

    // Note: DB operations like insert/delete should still be run off the main thread.
    // Consider using an ExecutorService, Kotlin Coroutines, or RxJava for this.
    public WalletHandle createNewWallet(String name, String password) throws InvalidAlgorithmParameterException, CipherException, NoSuchAlgorithmException, IOException, NoSuchProviderException {
        // Generate the wallet file and load credentials to get the address
        String filename = WalletUtils.generateNewWalletFile(password, walletsDir);
        Credentials credentials = WalletUtils.loadCredentials(password, new File(walletsDir, filename));
        String address = credentials.getAddress();

        // Create the WalletHandle entity
        WalletHandle walletHandle = new WalletHandle(UUID.randomUUID(), name, filename, address);

        // Save the WalletHandle to the database
        // TODO: Run this insert operation off the main thread
        walletHandleDao.insertAll(walletHandle);

        // Return the created WalletHandle (note: ID might not be set immediately if insert is async)
        return walletHandle;
    }
}
