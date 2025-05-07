package com.example.byebit.repository;

import android.content.Context;
import android.util.Log; // Import Log

import com.example.byebit.R; // Import R to access resources
import com.example.byebit.config.AppDatabase;
import com.example.byebit.dao.WalletHandleDao;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData; // Import MutableLiveData

import com.example.byebit.domain.WalletHandle;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;
import org.web3j.protocol.Web3j; // Import Web3j
import org.web3j.protocol.core.DefaultBlockParameterName; // Import DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthGetBalance; // Import EthGetBalance
import org.web3j.protocol.http.HttpService; // Import HttpService

import java.io.File;
import java.io.IOException;
// ADDED: Import BigDecimal
import java.math.BigDecimal;
import java.math.BigInteger; // Import BigInteger
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
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
            Log.d(TAG, "Inserted new wallet into DB: " + walletHandle.getName()); // Log DB insert
        });

        // Return the created WalletHandle (note: ID might not be set immediately if insert is async)
        return walletHandle;
    }

    // Add a method to get balance for a given address
    // This method returns LiveData and performs the network call on the executor
    public LiveData<BigInteger> getWalletBalance(String address) {
        MutableLiveData<BigInteger> balanceLiveData = new MutableLiveData<>();

        // Execute the Web3j call on a background thread
        databaseWriteExecutor.execute(() -> {
            try {
                Log.d(TAG, "Fetching balance for address: " + address); // Log balance fetch attempt
                EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
                if (ethGetBalance.hasError()) {
                    // Handle error (e.g., log it, post an error state to LiveData)
                    Log.e(TAG, "Error fetching balance for " + address + ": " + ethGetBalance.getError().getMessage());
                    balanceLiveData.postValue(null); // Indicate error or no data
                } else {
                    BigInteger balanceWei = ethGetBalance.getBalance();
                    Log.d(TAG, "Successfully fetched balance for " + address + ": " + balanceWei + " Wei"); // Log success
                    // Post the result back to the main thread via LiveData
                    balanceLiveData.postValue(balanceWei);
                    // MODIFIED: Convert BigInteger to BigDecimal before updating DB
                    updateWalletBalanceInDb(address, new BigDecimal(balanceWei));
                }
            } catch (IOException e) {
                // Handle network or other exceptions
                Log.e(TAG, "Exception fetching balance for " + address, e);
                balanceLiveData.postValue(null); // Indicate error or no data
            }
        });

        return balanceLiveData;
    }

    // MODIFIED: Update parameter type to BigDecimal
    private void updateWalletBalanceInDb(String address, BigDecimal balance) {
        databaseWriteExecutor.execute(() -> {
            WalletHandle wallet = walletHandleDao.findByAddressSync(address);
            if (wallet != null) {
                wallet.setBalance(balance); // Set BigDecimal balance
                walletHandleDao.update(wallet);
                // MODIFIED: Log with toPlainString() for consistent output
                Log.d(TAG, "Updated balance in DB for address " + address + " to " + balance.toPlainString());
            } else {
                Log.w(TAG, "Could not find wallet in DB to update balance for address: " + address);
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
}
