package com.example.byebit.ui.dialog;

public class WalletDetailDialogResult {
    public enum Action {
        REQUEST_DELETE_CONFIRMATION,
        CLOSE, // Explicit close button
        DISMISS // Generic dismiss (back press, tap outside)
    }

    public final Action action;

    private WalletDetailDialogResult(Action action) {
        this.action = action;
    }

    public static WalletDetailDialogResult requestDeleteConfirmation() {
        return new WalletDetailDialogResult(Action.REQUEST_DELETE_CONFIRMATION);
    }

    public static WalletDetailDialogResult close() {
        return new WalletDetailDialogResult(Action.CLOSE);
    }

    public static WalletDetailDialogResult dismiss() {
        return new WalletDetailDialogResult(Action.DISMISS);
    }
}
