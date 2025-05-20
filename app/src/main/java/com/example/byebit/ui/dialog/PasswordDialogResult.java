package com.example.byebit.ui.dialog;

import androidx.annotation.Nullable;

public class PasswordDialogResult {
    @Nullable
    public final String password;
    public final boolean isCancelled;
    public final boolean savePassword; // Add this field

    private PasswordDialogResult(@Nullable String password, boolean isCancelled, boolean savePassword) { // Update constructor
        this.password = password;
        this.isCancelled = isCancelled;
        this.savePassword = savePassword; // Initialize the new field
    }

    // Update success method to include savePassword state
    public static PasswordDialogResult success(String password, boolean savePassword) {
        return new PasswordDialogResult(password, false, savePassword);
    }

    // Update cancelled method (savePassword is irrelevant here, can be false)
    public static PasswordDialogResult cancelled() {
        return new PasswordDialogResult(null, true, false); // Default savePassword to false on cancel
    }

    public boolean isSuccess() {
        return password != null && !isCancelled;
    }
}
