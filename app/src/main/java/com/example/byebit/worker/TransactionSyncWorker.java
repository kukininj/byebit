package com.example.byebit.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.byebit.domain.TransactionHandle; // Adjust package
import com.example.byebit.domain.WalletHandle; // Adjust package
import com.example.byebit.remote.EtherscanApiResponse;
import com.example.byebit.remote.EtherscanApiService;
import com.example.byebit.remote.EtherscanTransaction;
import com.example.byebit.remote.RetrofitClient;
import com.example.byebit.repository.AppPreferencesRepository; // Import the new repository
import com.example.byebit.repository.TransactionRepository; // Adjust package
import com.example.byebit.repository.WalletRepository; // Adjust package

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import retrofit2.Response;

public class TransactionSyncWorker extends Worker {

    private static final String TAG = "TransactionSyncWorker";
    private static final long DEFAULT_START_BLOCK = 0;
    private static final int PAGE_SIZE = 50;

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final EtherscanApiService etherscanApiService;
    private final AppPreferencesRepository appPreferencesRepository;

    public TransactionSyncWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
        this.transactionRepository = new TransactionRepository(context);
        this.walletRepository = new WalletRepository(context);
        this.etherscanApiService = RetrofitClient.getApiService();
        this.appPreferencesRepository = AppPreferencesRepository.getInstance(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting transaction sync worker...");

        // 1. Get API Key from SharedPreferences (now synchronous)
        String apiKey = appPreferencesRepository.getEtherscanApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "Etherscan API key not found in SharedPreferences. Cannot proceed with sync.");
            return Result.failure();
        }

        List<WalletHandle> wallets = walletRepository.getAllWalletsSync();
        if (wallets == null || wallets.isEmpty()) {
            Log.d(TAG, "No wallets found to sync. Ending worker.");
            return Result.success();
        }

        boolean allWalletsSyncedSuccessfully = true;

        for (WalletHandle wallet : wallets) {
            Log.d(TAG, "Syncing transactions for wallet: " + wallet.getAddress());
            try {
                long lastSyncedBlock = DEFAULT_START_BLOCK; // TODO: Implement persistent last synced block per wallet

                Response<EtherscanApiResponse> response = etherscanApiService.getAccountTransactions(
                        wallet.getAddress(),
                        lastSyncedBlock,
                        99999999,
                        1,
                        PAGE_SIZE,
                        apiKey
                ).execute();

                if (response.isSuccessful() && response.body() != null && "1".equals(response.body().getStatus())) {
                    List<EtherscanTransaction> etherscanTransactions = response.body().getResult();
                    if (etherscanTransactions != null) {
                        for (EtherscanTransaction ethTx : etherscanTransactions) {
                            TransactionHandle transaction = mapEtherscanTransactionToHandle(ethTx, wallet);
                            if (transaction != null) {
                                transactionRepository.insertTransactionSync(transaction);
                            }
                        }
                        Log.d(TAG, "Successfully synced " + etherscanTransactions.size() + " transactions for " + wallet.getAddress());
                    }
                } else {
                    Log.e(TAG, "Etherscan API error for " + wallet.getAddress() + ": " +
                            (response.body() != null ? response.body().getMessage() : response.message()));
                    allWalletsSyncedSuccessfully = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing transactions for " + wallet.getAddress(), e);
                allWalletsSyncedSuccessfully = false;
            }
        }

        Log.d(TAG, "Transaction sync worker finished.");
        return allWalletsSyncedSuccessfully ? Result.success() : Result.retry();
    }

    // Helper method to map EtherscanTransaction to TransactionHandle (unchanged)
    private TransactionHandle mapEtherscanTransactionToHandle(EtherscanTransaction ethTx, WalletHandle wallet) {
        // ... (same as before)
        try {
            BigDecimal transactionAmount = new BigDecimal(ethTx.getValue()).divide(new BigDecimal("1000000000000000000"));
            BigDecimal gasPrice = new BigDecimal(ethTx.getGasPrice());
            BigDecimal gasUsed = new BigDecimal(ethTx.getGasUsed());
            BigDecimal transactionFee = gasPrice.multiply(gasUsed).divide(new BigDecimal("1000000000000000000"));

            String direction;
            String sender = ethTx.getFrom().toLowerCase();
            String receiver = ethTx.getTo() != null ? ethTx.getTo().toLowerCase() : null;
            String walletAddress = wallet.getAddress().toLowerCase();

            if (sender.equals(walletAddress)) {
                direction = "Send";
            } else if (receiver != null && receiver.equals(walletAddress)) {
                direction = "Received";
            } else {
                Log.w(TAG, "Unexpected transaction direction for " + ethTx.getHash() + " on wallet " + walletAddress);
                return null;
            }

            String status;
            if ("1".equals(ethTx.getTxReceiptStatus())) {
                status = "Success";
            } else if ("0".equals(ethTx.getIsError()) && "0".equals(ethTx.getTxReceiptStatus())) {
                status = "Success";
            }
            else {
                status = "Failure";
            }

            Instant timestamp = Instant.ofEpochSecond(Long.parseLong(ethTx.getTimeStamp()));

            return new TransactionHandle(
                    UUID.randomUUID(),
                    ethTx.getHash(),
                    direction,
                    status,
                    wallet.getId(),
                    ethTx.getFrom(),
                    ethTx.getTo(),
                    transactionAmount,
                    transactionFee,
                    "ETHEREUM",
                    timestamp
            );
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing numeric value from Etherscan transaction: " + ethTx.getHash(), e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Generic error mapping transaction: " + ethTx.getHash(), e);
            return null;
        }
    }
}
