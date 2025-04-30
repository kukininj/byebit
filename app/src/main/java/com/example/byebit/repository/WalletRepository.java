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
import java.util.concurrent.ExecutorService; // Import ExecutorService
import java.util.concurrent.Executors; // Import Executors

public class WalletRepository {

    private final WalletHandleDao walletHandleDao;
    private final File walletsDir;
    private final ExecutorService databaseWriteExecutor; // Executor for database operations

    public WalletRepository(Context context) {
        this.walletHandleDao = AppDatabase.getDatabase(context).getWalletHandleDao();
        this.walletsDir = context.getFilesDir();
        // Initialize the executor service
        databaseWriteExecutor = Executors.newFixedThreadPool(4); // Use a small pool for DB writes
    }

    // Return LiveData directly from the DAO
    public LiveData<List<WalletHandle>> getSavedWallets() {
        // Room handles background threading for LiveData queries
        return walletHandleDao.getAll();
    }

    // Note: This method still needs to be called off the main thread
    // because WalletUtils operations can be blocking.
    public WalletHandle createNewWallet(String name, String password) throws InvalidAlgorithmParameterException, CipherException, NoSuchAlgorithmException, IOException, NoSuchProviderException {
        // Generate the wallet file and load credentials to get the address
        String filename = WalletUtils.generateLightNewWalletFile(password, walletsDir);
        Credentials credentials = WalletUtils.loadCredentials(password, new File(walletsDir, filename));
        String address = credentials.getAddress();

        // Create the WalletHandle entity
        WalletHandle walletHandle = new WalletHandle(UUID.randomUUID(), name, filename, address);

        // Save the WalletHandle to the database using the executor
        databaseWriteExecutor.execute(() -> {
            walletHandleDao.insertAll(walletHandle);
        });

        // Return the created WalletHandle (note: ID might not be set immediately if insert is async)
        return walletHandle;
    }

    // Add a method to shut down the executor when the repository is no longer needed
    // This might be called from the ViewModel's onCleared()
    public void shutdown() {
        databaseWriteExecutor.shutdown();
    }
}
