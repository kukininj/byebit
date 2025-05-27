package com.example.byebit.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.byebit.SignatureConfirmActivity;

// Inside your app that performs the signing
public class ConfirmationDialogFragment extends DialogFragment {

    private static final String ARG_MESSAGE = "message_to_display";

    public static ConfirmationDialogFragment newInstance(byte[] message) {
        ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putByteArray(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        byte[] messageBytes = getArguments() != null ? getArguments().getByteArray(ARG_MESSAGE) : null;
        String messagePreview = (messageBytes != null) ? new String(messageBytes) : "No message provided.";

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Confirm Message Signing")
                .setMessage("Do you want to sign the following message?\n\n" + messagePreview +
                        "\n\nThis action cannot be undone.")
                .setPositiveButton("Sign", (dialog, id) -> {
                    // User confirmed
                    ((SignatureConfirmActivity) requireActivity()).onUserConfirmation(true);
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    // User cancelled
                    ((SignatureConfirmActivity) requireActivity()).onUserConfirmation(false);
                });
        return builder.create();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        // User dismissed dialog (e.g., back button)
        ((SignatureConfirmActivity) requireActivity()).onUserConfirmation(false);
    }
}
