package com.example.byebit.ui.dialog;

import androidx.annotation.Nullable;

public class PasswordDialogResult {
    @Nullable
    public final String password;
    public final boolean isCancelled;

    private PasswordDialogResult(@Nullable String password, boolean isCancelled) {
        this.password = password;
        this.isCancelled = isCancelled;
    }

    public static PasswordDialogResult success(String password) {
        return new PasswordDialogResult(password, false);
    }

    public static PasswordDialogResult cancelled() {
        return new PasswordDialogResult(null, true);
    }

    public boolean isSuccess() {
        return password != null && !isCancelled;
    }
}
