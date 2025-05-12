package com.example.byebit.ui.dialog;

import com.example.byebit.domain.WalletHandle;

import java.util.List;

import androidx.annotation.Nullable;

public class WalletImportResult {
    @Nullable
    public final List<WalletHandle> importedWallets;
    @Nullable
    public final String errorMessage;

    public final WalletImportResultStatus status;

    private WalletImportResult(@Nullable List<WalletHandle> importedWallets, @Nullable String errorMessage, WalletImportResultStatus status) {
        this.importedWallets = importedWallets;
        this.errorMessage = errorMessage;
        this.status = status;
    }

    public static WalletImportResult success(List<WalletHandle> importedWallets) {
        return new WalletImportResult(importedWallets, null, WalletImportResultStatus.SUCCESS);
    }

    public static WalletImportResult error(String errorMessage) {
        return new WalletImportResult(null, errorMessage, WalletImportResultStatus.FAILURE);
    }

    public static WalletImportResult cancelled() {
        return new WalletImportResult(null, null, WalletImportResultStatus.CANCELLED);
    }

    public boolean isSuccess() {
        return importedWallets != null && errorMessage == null && !isCancelled();
    }

    public boolean isError() {
        return errorMessage != null && !isCancelled();
    }

    public boolean isCancelled() {
        return status == WalletImportResultStatus.CANCELLED;
    }

    public enum WalletImportResultStatus {
        SUCCESS, FAILURE, CANCELLED
    }
}
