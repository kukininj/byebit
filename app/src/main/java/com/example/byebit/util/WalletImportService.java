package com.example.byebit.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import io.reactivex.Single; // Added
import io.reactivex.schedulers.Schedulers; // Added

import com.example.byebit.config.AppDatabase;
import com.example.byebit.dao.WalletHandleDao;
import com.example.byebit.domain.WalletHandle;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.web3j.crypto.WalletFile;
import org.web3j.protocol.ObjectMapperFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
// Removed ExecutorService, Executors
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// Removed Handler, Looper

public class WalletImportService {

    private static final String TAG = "WalletImportService";
    private static final String WALLETS_DIR_NAME = "wallets"; // Consistent directory name

    private final Context applicationContext;
    private final WalletHandleDao walletHandleDao;
    private final File walletsDir;


    public WalletImportService(@NonNull Context context) {
        this.applicationContext = context.getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(applicationContext);
        this.walletHandleDao = db.getWalletHandleDao();
        this.walletsDir = new File(applicationContext.getFilesDir(), WALLETS_DIR_NAME);
        if (!walletsDir.exists()) {
            if (!walletsDir.mkdirs()) {
                Log.e(TAG, "Failed to create wallets directory: " + walletsDir.getAbsolutePath());
            }
        }
    }

    public Single<List<WalletHandle>> importWalletsFromZip(@NonNull Uri zipUri) {
        return Single.<List<WalletHandle>>create(emitter -> {
            List<WalletHandle> importedHandles = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            try (InputStream inputStream = applicationContext.getContentResolver().openInputStream(zipUri);
                 ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

                if (inputStream == null) {
                    // Use emitter.onError for failures
                    emitter.onError(new IOException("Could not open input stream for URI: " + zipUri));
                    return; // Stop execution for this lambda
                }

                ZipEntry entry;
                byte[] buffer = new byte[1024]; // Buffer for extraction

                while ((entry = zipInputStream.getNextEntry()) != null) {
                    // Check if the stream has been disposed (e.g., subscriber unsubscribed)
                    if (emitter.isDisposed()) {
                        Log.w(TAG, "Import process cancelled by subscriber.");
                        zipInputStream.closeEntry(); // Close current entry before returning
                        return;
                    }

                    String entryName = new File(entry.getName()).getName(); // Sanitize name

                    if (entry.isDirectory() || !entryName.contains("UTC--")) {
                        Log.d(TAG, "Skipping non-wallet entry: " + entryName);
                        zipInputStream.closeEntry();
                        continue;
                    }

                    File targetFile = new File(walletsDir, entryName);

                    if (targetFile.exists()) {
                        Log.w(TAG, "Skipping import for existing file: " + entryName);
                        zipInputStream.closeEntry();
                        continue;
                    }

                    Log.d(TAG, "Extracting wallet file: " + entryName);
                    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                        int len;
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to extract file: " + entryName, e);
                        errors.add("Failed to extract " + entryName);
                        if (targetFile.exists()) {
                            targetFile.delete();
                        }
                        zipInputStream.closeEntry();
                        continue;
                    }

                    try {
                        WalletFile walletFile = ObjectMapperFactory.getObjectMapper().readValue(targetFile, WalletFile.class);
                        String address = "0x" + walletFile.getAddress();
                        String walletName = entryName.replace(".json", "");

                        WalletHandle existingWallet = walletHandleDao.findByAddressSync(address);
                        if (existingWallet != null) {
                            Log.w(TAG, "Wallet with address " + address + " already exists in DB. Skipping DB insert for file: " + entryName);
                        } else {
                            WalletHandle newHandle = new WalletHandle(UUID.randomUUID(), walletName, entryName, address, null, null);

                            walletHandleDao.insertAll(newHandle);
                            importedHandles.add(newHandle);
                            Log.i(TAG, "Successfully imported and saved wallet: " + walletName + " (" + address + ")");
                        }

                    } catch (JsonProcessingException e) {
                        Log.e(TAG, "Failed to parse wallet file: " + entryName, e);
                        errors.add("Failed to parse " + entryName);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing wallet file: " + entryName, e);
                        errors.add("Error processing " + entryName);
                    } finally {
                        zipInputStream.closeEntry();
                    }
                } // End while loop

                // Check again if disposed before emitting result
                if (emitter.isDisposed()) {
                     Log.w(TAG, "Import process cancelled before emitting result.");
                     return;
                }

                // Report results
                if (!errors.isEmpty()) {
                    // Propagate error if any occurred, even if some wallets were imported
                    String combinedErrorMessage = "Import finished with errors: " + String.join("; ", errors);
                    // Consider creating a custom Exception class for partial success/failure if needed
                    emitter.onError(new RuntimeException(combinedErrorMessage));
                } else {
                    // Emit the list of successfully imported handles (can be empty)
                    emitter.onSuccess(importedHandles);
                }

            } catch (IOException e) {
                Log.e(TAG, "Failed to read ZIP file from URI: " + zipUri, e);
                 if (!emitter.isDisposed()) {
                    emitter.onError(new IOException("Failed to read ZIP file: " + e.getMessage(), e));
                 }
            } catch (Exception e) { // Catch unexpected errors
                Log.e(TAG, "Unexpected error during import from URI: " + zipUri, e);
                 if (!emitter.isDisposed()) {
                    emitter.onError(new RuntimeException("An unexpected error occurred during import: " + e.getMessage(), e));
                 }
            }
            // --- End of logic moved from performImport ---
        })
        .subscribeOn(Schedulers.io()); // Ensure the operation runs on an I/O thread
    }

    // REMOVED public void shutdown() { ... }
}
