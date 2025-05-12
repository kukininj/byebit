package com.example.byebit.repository;

import android.content.Context;
import android.util.Log; // Import Log

import com.example.byebit.R; // Import R to access resources
import com.example.byebit.config.AppDatabase;
import com.example.byebit.dao.WalletHandleDao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.byebit.domain.WalletHandle;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;
import org.web3j.protocol.Web3j; // Import Web3j
import org.web3j.protocol.core.DefaultBlockParameterName; // Import DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthGetBalance; // Import EthGetBalance
import org.web3j.protocol.http.HttpService; // Import HttpService
import org.web3j.utils.Convert;

import java.io.File;
import java.io.IOException;
// ADDED: Import RxJava Single
import io.reactivex.Single;
// ADDED: Import RxJava Schedulers
import io.reactivex.schedulers.Schedulers;
// ADDED: Import BigDecimal
import java.math.BigDecimal;
import java.math.BigInteger; // Import BigInteger
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WalletRepository {

    private static final String TAG = "WalletRepository"; // Add a TAG for logging

    private final WalletHandleDao walletHandleDao;
    private final File walletsDir;
    private final ExecutorService databaseWriteExecutor; // Executor for database operations
    private final Web3j web3j; // Add Web3j instance

    public WalletRepository(Context context) {
        this.walletHandleDao = AppDatabase.getDatabase(context).getWalletHandleDao();
        this.walletsDir = context.getFilesDir();
        // Initialize the executor service
        databaseWriteExecutor = Executors.newFixedThreadPool(4); // Use a small pool for DB writes

        // Initialize Web3j client using the Sepolia RPC URL from strings.xml
        String sepoliaRpcUrl = context.getString(R.string.sepolia_rpc_url);
        this.web3j = Web3j.build(new HttpService(sepoliaRpcUrl));
        Log.d(TAG, "Web3j client initialized for URL: " + sepoliaRpcUrl); // Log initialization
    }

    // Return LiveData directly from the DAO
    public LiveData<List<WalletHandle>> getSavedWallets() {
        // Room handles background threading for LiveData queries
        return walletHandleDao.getAll();
    }

    public Single<WalletHandle> createNewWallet(String name, String password, @Nullable byte[] encryptedPassword, @Nullable byte[] encryptedPasswordIv) {
        Log.d(TAG, "Creating new wallet..."); // Add log
        return Single.fromCallable(() -> {
            String filename = WalletUtils.generateLightNewWalletFile(password, walletsDir);
            File walletFile = new File(walletsDir, filename);

            if (!walletFile.exists()) {
                Log.e(TAG, "Wallet file not found for import: " + walletFile.getAbsolutePath());
                throw new IOException("Wallet file not found: " + walletFile.getAbsolutePath());
            }

            Log.d(TAG, "Loading credentials from file: " + walletFile.getName());
            Credentials credentials = WalletUtils.loadCredentials(password, walletFile);
            String address = credentials.getAddress();
            Log.d(TAG, "Successfully loaded credentials. Address: " + address);

            WalletHandle walletHandle = new WalletHandle(UUID.randomUUID(), name, filename, address, encryptedPassword, encryptedPasswordIv);

            walletHandleDao.insertAll(walletHandle);
            Log.d(TAG, "Inserted imported wallet into DB: " + walletHandle.getName() + " (Address: " + walletHandle.getAddress() + ")");

            return walletHandle;
        }).subscribeOn(Schedulers.io());
    }

    public Credentials getCredentials(WalletHandle walletHandle, String password) throws IOException, CipherException {
        File walletFile = new File(walletsDir, walletHandle.getFilename());
        if (!walletFile.exists()) {
            throw new IOException("Wallet file not found: " + walletFile.getAbsolutePath());
        }
        return WalletUtils.loadCredentials(password, walletFile);
    }

    // Add a method to get balance for a given address
    // This method returns LiveData and performs the network call on the executor
    public void getWalletBalance(String address) {
        // Execute the Web3j call on a background thread
        databaseWriteExecutor.execute(() -> {
            try {
                Log.d(TAG, "Fetching balance for address: " + address); // Log balance fetch attempt
                EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
                if (ethGetBalance.hasError()) {
                    // Handle error (e.g., log it, post an error state to LiveData)
                    Log.e(TAG, "Error fetching balance for " + address + ": " + ethGetBalance.getError().getMessage());
                } else {
                    BigInteger balanceWei = ethGetBalance.getBalance();
                    BigDecimal balance = Convert.fromWei(new BigDecimal(balanceWei), Convert.Unit.ETHER);
                    Log.d(TAG, "Successfully fetched balance for " + address + ": " + balanceWei + " Wei"); // Log success
                    updateWalletBalanceInDb(address, balance);
                }
            } catch (IOException e) {
                // Handle network or other exceptions
                Log.e(TAG, "Exception fetching balance for " + address, e);
            }
        });
    }

    // MODIFIED: Update parameter type to BigDecimal
    private void updateWalletBalanceInDb(String address, BigDecimal balance) {
        databaseWriteExecutor.execute(() -> {
            WalletHandle wallet = walletHandleDao.findByAddressSync(address);
            if (wallet != null) {
                wallet.setBalance(balance); // Set BigDecimal balance
                wallet.setBalanceLastUpdated(System.currentTimeMillis());
                walletHandleDao.update(wallet);
                // MODIFIED: Log with toPlainString() for consistent output
                Log.d(TAG, "Updated balance in DB for address " + address + " to " + balance.toPlainString() + " at " + wallet.getBalanceLastUpdated());
            } else {
                Log.w(TAG, "Could not find wallet in DB to update balance for address: " + address);
            }
        });
    }

    // ADD: Method to delete a wallet (file and database entry)
    public void deleteWallet(WalletHandle wallet) {
        databaseWriteExecutor.execute(() -> {
            try {
                // 1. Delete the wallet file from the filesystem
                // Ensure 'walletsDir' correctly points to where files are stored.
                File walletFile = new File(this.walletsDir, wallet.getFilename());
                if (walletFile.exists()) {
                    if (walletFile.delete()) {
                        Log.d(TAG, "Successfully deleted wallet file: " + walletFile.getAbsolutePath());
                    } else {
                        Log.w(TAG, "Failed to delete wallet file: " + walletFile.getAbsolutePath());
                        // Consider how to handle this failure; for now, we proceed to DB deletion.
                    }
                } else {
                    Log.w(TAG, "Wallet file not found, cannot delete from filesystem: " + walletFile.getAbsolutePath());
                }

                // 2. Delete the WalletHandle entry from the database
                walletHandleDao.delete(wallet);
                Log.d(TAG, "Successfully deleted wallet from database: " + wallet.getName() + " (Address: " + wallet.getAddress() + ")");
                // The LiveData in DashboardViewModel will automatically update the UI.

            } catch (Exception e) {
                Log.e(TAG, "Error deleting wallet: " + wallet.getName(), e);
                // Consider how to report this error back to the UI if needed.
                // For now, logging the error.
            }
        });
    }


    // Add a method to shut down the executor and the Web3j client
    // This should be called when the repository is no longer needed (e.g., from ViewModel's onCleared)
    public void shutdown() {
        databaseWriteExecutor.shutdown();
        if (web3j != null) {
            web3j.shutdown(); // Shutdown the Web3j client
            Log.d(TAG, "Web3j client shut down."); // Log shutdown
        }
    }

    public void setPasswordForWallet(WalletHandle handle, byte[] encryptedPassword, byte[] passwordIv) {
        handle.setEncryptedPassword(encryptedPassword);
        handle.setIv(passwordIv);

        databaseWriteExecutor.execute(() -> {
            walletHandleDao.update(handle);
        });
    }
}
