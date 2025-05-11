package com.example.byebit.util;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.byebit.domain.WalletHandle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WalletExporter {

    private static final String TAG = "WalletExporter";
    private final ExecutorService executorService;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final Context applicationContext;

    public interface ExportListener {
        void onExportSuccess();
        void onExportFailure(String errorMessage);
    }

    public WalletExporter(Context context) {
        this.applicationContext = context.getApplicationContext(); // Use application context
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void exportWalletsToZip(Uri destinationUri, List<WalletHandle> wallets, ExportListener listener) {
        executorService.execute(() -> {
            boolean success = false;
            String errorMessage = null;
            File walletsDir = null;

            try {
                walletsDir = applicationContext.getFilesDir();
                if (walletsDir == null) {
                    throw new IOException("Failed to get application files directory (walletsDir is null).");
                }
                if (!walletsDir.exists() || !walletsDir.isDirectory()) {
                    throw new IOException("Wallets directory not found or is not a directory at: " + walletsDir.getAbsolutePath());
                }

                try (OutputStream fos = applicationContext.getContentResolver().openOutputStream(destinationUri);
                     ZipOutputStream zos = new ZipOutputStream(fos)) {

                    if (fos == null) {
                        throw new IOException("Failed to open output stream for URI: " + destinationUri);
                    }

                    byte[] buffer = new byte[4096];
                    for (WalletHandle wallet : wallets) {
                        File walletFile = new File(walletsDir, wallet.getFilename());
                        if (walletFile.exists() && walletFile.isFile()) {
                            Log.d(TAG, "Adding to zip: " + walletFile.getAbsolutePath() + " (Size: " + walletFile.length() + " bytes)");
                            ZipEntry zipEntry = new ZipEntry(wallet.getFilename());
                            zos.putNextEntry(zipEntry);
                            try (FileInputStream fis = new FileInputStream(walletFile)) {
                                int length;
                                while ((length = fis.read(buffer)) > 0) {
                                    zos.write(buffer, 0, length);
                                }
                            }
                            zos.closeEntry();
                            Log.d(TAG, "Successfully added " + wallet.getFilename() + " to zip.");
                        } else {
                            Log.w(TAG, "Wallet file not found or is not a file, skipping: " + walletFile.getAbsolutePath());
                        }
                    }
                    zos.finish();
                    success = true;
                    Log.d(TAG, "Zipping process completed successfully.");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error zipping wallets to " + destinationUri, e);
                errorMessage = e.getMessage();
                if (walletsDir != null) {
                    Log.e(TAG, "Wallets directory was: " + walletsDir.getAbsolutePath());
                }
            } catch (NullPointerException e) { // Catching potential NPEs during file operations
                Log.e(TAG, "NullPointerException during zipping", e);
                errorMessage = "A null pointer occurred during export.";
            }

            final boolean finalSuccess = success;
            final String finalErrorMessage = errorMessage;
            mainThreadHandler.post(() -> {
                if (finalSuccess) {
                    listener.onExportSuccess();
                } else {
                    listener.onExportFailure(finalErrorMessage != null ? finalErrorMessage : "Unknown error");
                }
            });
        });
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            Log.d(TAG, "Shutting down WalletExporter executor service.");
            executorService.shutdown();
        }
    }
}
