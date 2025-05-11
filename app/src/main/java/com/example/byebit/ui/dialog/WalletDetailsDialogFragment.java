package com.example.byebit.ui.dialog;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.byebit.R;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class WalletDetailsDialogFragment extends DialogFragment {

    public static final String TAG = "WalletDetailsDialogFragment";
    // REQUEST_KEY and action type constants related to FragmentResultListener are removed

    private static final String ARG_WALLET_NAME = "walletName";
    private static final String ARG_WALLET_ADDRESS = "walletAddress";
    private static final String ARG_PRIVATE_KEY = "privateKey";

    // RESULT_KEY_ACTION_TYPE and specific action type strings are removed

    private String walletName;
    private String privateKey;

    private final PublishSubject<WalletDetailDialogResult> resultSubject = PublishSubject.create();
    private boolean actionTaken = false; // To track if a button that dismisses dialog was clicked

    public Observable<WalletDetailDialogResult> getDialogEvents() {
        return resultSubject.hide(); // Hide to prevent casting to Subject
    }

    public static WalletDetailsDialogFragment newInstance(String walletName, String walletAddress, String privateKey) {
        WalletDetailsDialogFragment fragment = new WalletDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_WALLET_NAME, walletName);
        args.putString(ARG_WALLET_ADDRESS, walletAddress);
        args.putString(ARG_PRIVATE_KEY, privateKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            walletName = getArguments().getString(ARG_WALLET_NAME);
            // walletAddress is used in message, privateKey for copy
            privateKey = getArguments().getString(ARG_PRIVATE_KEY);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() == null) {
            // Should not happen if newInstance is used
            return super.onCreateDialog(savedInstanceState);
        }

        String currentWalletName = getArguments().getString(ARG_WALLET_NAME);
        String currentWalletAddress = getArguments().getString(ARG_WALLET_ADDRESS);
        final String currentPrivateKey = getArguments().getString(ARG_PRIVATE_KEY);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        // Assuming R.string.wallet_details_title_format = "Details: %s"
        builder.setTitle(getString(R.string.wallet_details_title_format, currentWalletName));

        String message = getString(R.string.wallet_address_label) + "\n" + currentWalletAddress +
                "\n\n" + getString(R.string.private_key_label) + "\n" + currentPrivateKey;
        builder.setMessage(message);

        // Positive Button: Delete (requests confirmation from parent)
        builder.setPositiveButton(R.string.delete_wallet_button_details, (dialog, which) -> {
            resultSubject.onNext(WalletDetailDialogResult.requestDeleteConfirmation());
            actionTaken = true;
            // Dialog dismisses automatically after button click
        });

        // Neutral Button: Copy Private Key
        builder.setNeutralButton(R.string.copy_private_key_button, (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Private Key", currentPrivateKey);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), R.string.private_key_copied_toast, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), R.string.failed_to_copy_private_key_toast, Toast.LENGTH_SHORT).show();
            }
            // This button does NOT dismiss the dialog, so actionTaken remains false unless other buttons are hit.
        });

        // Negative Button: Close
        builder.setNegativeButton(R.string.close_button, (dialog, which) -> {
            resultSubject.onNext(WalletDetailDialogResult.close());
            actionTaken = true;
            // Dialog dismisses automatically after button click
        });
        
        // Prevent dialog from being cancellable by tapping outside, to ensure onDismiss logic is controlled.
        // Back press will still call onDismiss.
        setCancelable(true);


        return builder.create();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (isAdded()) { // Ensure fragment is still added
            if (!actionTaken) {
                // If dismissed via back press or tapping outside (if cancelable)
                // and no explicit action button was pressed that sets actionTaken = true
                resultSubject.onNext(WalletDetailDialogResult.dismiss());
            }
            resultSubject.onComplete(); // Complete the subject when dialog is dismissed
        }
    }
}
