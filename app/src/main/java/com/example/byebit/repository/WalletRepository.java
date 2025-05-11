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
import org.web3j.utils.Convert;

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

    /**
     * Creates a new Ethereum wallet file and saves its handle to the database.
     *
     * @param name The name for the wallet handle.
     * @param password The password to encrypt the wallet file.
     * @param encryptedPassword The password encrypted by biometric prompt (can be null).
     * @param iv The IV used for biometric encryption (can be null).
     * @param savePassword If true, encryptedPassword and iv are saved in the DB. If false, they are set to null before saving.
     * @return The created WalletHandle entity.
     * @throws InvalidAlgorithmParameterException
     * @throws CipherException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws NoSuchProviderException
     */
    // MODIFY THIS METHOD SIGNATURE TO ACCEPT savePassword BOOLEAN
    public WalletHandle createNewWallet(String name, String password, byte[] encryptedPassword, byte[] iv, boolean savePassword) throws InvalidAlgorithmParameterException, CipherException, NoSuchAlgorithmException, IOException, NoSuchProviderException {
        Log.d(TAG, "Creating new wallet file..."); // Add log
        // Generate the wallet file and load credentials to get the address
        String filename = WalletUtils.generateLightNewWalletFile(password, walletsDir);
        Credentials credentials = WalletUtils.loadCredentials(password, new File(walletsDir, filename));
        String address = credentials.getAddress();
        Log.d(TAG, "Wallet file created: " + filename + ", Address: " + address); // Add log

        // Create the WalletHandle entity
        WalletHandle walletHandle = new WalletHandle(UUID.randomUUID(), name, filename, address, encryptedPassword, iv);

        // Save the WalletHandle to the database using the executor
        databaseWriteExecutor.execute(() -> {
            // ADD THIS CONDITIONAL LOGIC
            if (!savePassword) {
                Log.d(TAG, "User opted out of saving password, setting encryptedPassword and iv to null.");
                walletHandle.setEncryptedPassword(null);
                walletHandle.setIv(null);
            } else {
                 Log.d(TAG, "User opted to save password, saving encryptedPassword and iv.");
            }
            walletHandleDao.insertAll(walletHandle);
            Log.d(TAG, "Inserted new wallet into DB: " + walletHandle.getName()); // Log DB insert
        });

        return walletHandle;
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
}
