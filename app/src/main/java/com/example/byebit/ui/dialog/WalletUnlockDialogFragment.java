package com.example.byebit.ui.dialog;

import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.byebit.R;
import com.example.byebit.domain.WalletHandle;
import com.example.byebit.security.AuthenticationFailureReason;
import com.example.byebit.security.AuthenticationListener;
import com.example.byebit.security.BiometricService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;

public class WalletUnlockDialogFragment extends DialogFragment {

    private static final String TAG = "WalletUnlockDialog";
    private static final String ARG_WALLET_NAME = "walletName";
    private static final String ARG_ENCRYPTED_PASSWORD = "encryptedPassword";
    private static final String ARG_IV = "iv";

    private String walletName;
    private byte[] encryptedPassword;
    private byte[] iv;

    private BiometricService biometricService;
    private final PublishSubject<PasswordDialogResult> resultSubject = PublishSubject.create();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private AlertDialog dialog; // To control the dialog content dynamically

    public static WalletUnlockDialogFragment newInstance(@NonNull WalletHandle wallet) {
        return newInstance(wallet.getName(), wallet.getEncryptedPassword(), wallet.getIv());
    }

    public static WalletUnlockDialogFragment newInstance(@NonNull String walletName, @Nullable byte[] encryptedPassword, @Nullable byte[] iv) {
        WalletUnlockDialogFragment fragment = new WalletUnlockDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_WALLET_NAME, walletName);
        args.putByteArray(ARG_ENCRYPTED_PASSWORD, encryptedPassword);
        args.putByteArray(ARG_IV, iv);
        fragment.setArguments(args);
        return fragment;
    }

    public Observable<PasswordDialogResult> getDialogEvents() {
        return resultSubject.hide();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            walletName = getArguments().getString(ARG_WALLET_NAME);
            encryptedPassword = getArguments().getByteArray(ARG_ENCRYPTED_PASSWORD);
            iv = getArguments().getByteArray(ARG_IV);
        }

        // It's important that BiometricService can be initialized here or that
        // requireActivity() is valid. Usually, it's fine in onCreate for DialogFragments.
        biometricService = new BiometricService(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_unlock_wallet_progress, null); // A simple layout with a TextView

        TextView messageTextView = view.findViewById(R.id.unlock_message_textview);
        messageTextView.setText(getString(R.string.unlock_wallet_attempting_biometric, walletName));

        builder.setView(view)
                .setTitle(getString(R.string.unlock_wallet_title, walletName));

        // Make dialog non-cancelable initially by user actions like back press or touch outside.
        // Cancellation will be handled by biometric prompt or password dialog.
        setCancelable(false);

        dialog = builder.create();
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Attempt biometric unlock when the dialog is shown
        attemptUnlock();
    }

    private void attemptUnlock() {
        BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.d(TAG, "Authentication succeeded!");
                if (result.getCryptoObject() != null && result.getCryptoObject().getCipher() != null) {
                    Cipher cipher = result.getCryptoObject().getCipher();
                    try {
                        byte[] bytes = cipher.doFinal(encryptedPassword);

                        resultSubject.onNext(PasswordDialogResult.success(new String(bytes, StandardCharsets.UTF_8), false));
                        dismissSafely();
                    } catch (BadPaddingException | IllegalBlockSizeException e) {
                        Log.e(TAG, "Decryption failed: " + e.getMessage(), e);
                        showPasswordInputDialog("Decryption failed: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "Error during decryption operation: " + e.getMessage(), e);
                        showPasswordInputDialog("Error during decryption operation: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Authentication succeeded but crypto object or cipher is null.");
                    showPasswordInputDialog("Authentication succeeded but crypto object or cipher is null.");
                }
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // TODO: add more reasons
                Log.e(TAG, "Authentication error: " + errString);
                showPasswordInputDialog("Authentication error: " + errString);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.e(TAG, "Authentication failed");
                showPasswordInputDialog("Authentication failed");
            }
        };

        if (encryptedPassword != null && encryptedPassword.length > 0 && iv != null && iv.length > 0) {
            // Update UI if necessary
            TextView messageTextView = dialog.findViewById(R.id.unlock_message_textview);
            if (messageTextView != null) {
                messageTextView.setText(getString(R.string.unlock_wallet_attempting_biometric, walletName));
            }

            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);

                SecretKey key = (SecretKey) keyStore.getKey(walletName, null);
                if (key == null) {
                    Log.e(TAG, "Key not found in Keystore for alias: " + walletName);
                    showPasswordInputDialog("Key not found in Keystore for alias: " + walletName);
                    return;
                }

                GCMParameterSpec spec = new GCMParameterSpec(128, iv);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

                cipher.init(Cipher.DECRYPT_MODE, key, spec);

                BiometricPrompt biometricPrompt = new BiometricPrompt(this, this.getActivity().getMainExecutor(), callback);
                BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock to use decryption")
                        .setSubtitle("Confirm your screen lock")
                        .setAllowedAuthenticators(DEVICE_CREDENTIAL)
                        .build();

                biometricPrompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(cipher));

            } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException |
                     InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException |
                     UnrecoverableKeyException e) {
                Log.e(TAG, "Error setting up decryption: " + e.getMessage(), e);
                showPasswordInputDialog("Error setting up decryption: " + e.getMessage());
            }
        } else {
            // No encrypted password or biometrics not available/usable, go directly to password input
            String reasonMessage = encryptedPassword == null || iv == null ?
                    "Biometric information not found for this wallet." :
                    "Biometric authentication is not available or not set up.";
            showPasswordInputDialog(reasonMessage);
        }
    }

    private void showPasswordInputDialog(String messageForUser) {
        if (!isAdded() || getChildFragmentManager().isStateSaved()) {
            Log.w(TAG, "Cannot show PasswordInputDialogFragment, fragment not added or state saved.");
            // If we can't show the dialog, we should probably cancel the operation.
            if (!resultSubject.hasComplete() && !resultSubject.hasThrowable()) {
//                resultSubject.onNext(PasswordDialogResult.cancelled("Failed to show password input dialog."));
                resultSubject.onNext(PasswordDialogResult.cancelled());
            }
            dismissSafely();
            return;
        }

        // Update UI to indicate fallback
        TextView messageTextView = dialog.findViewById(R.id.unlock_message_textview);
        if (messageTextView != null) {
            messageTextView.setText(getString(R.string.unlock_wallet_enter_password, walletName));
        }
        if (getContext() != null && messageForUser != null && !messageForUser.isEmpty()) {
            Toast.makeText(getContext(), messageForUser, Toast.LENGTH_LONG).show();
        }


        PasswordInputDialogFragment passwordDialog = PasswordInputDialogFragment.newInstance(
                getString(R.string.enter_password_for_wallet, walletName),
                getString(R.string.password_hint),
                getString(R.string.unlock_button)
        );

        disposables.add(passwordDialog.getDialogEvents()
                .subscribe(
                        result -> {
                            resultSubject.onNext(result);
                            // onComplete will be called in onDismiss
                            dismissSafely(); // Dismiss WalletUnlockDialog after password input is done
                        },
                        throwable -> {
                            Log.e(TAG, "Error from PasswordInputDialogFragment", throwable);
                            resultSubject.onError(throwable);
                            // onComplete will be called in onDismiss
                            dismissSafely();
                        }
                        // Completion of passwordDialog's subject is handled by its own onDismiss.
                        // WalletUnlockDialogFragment's subject completes in its onDismiss.
                )
        );
        // Show the password dialog. WalletUnlockDialogFragment remains visible underneath or is replaced.
        // To avoid issues with multiple dialogs, we can hide/dismiss the current one before showing the new one,
        // but PasswordInputDialogFragment is designed to be a standalone dialog.
        // Showing it via childFragmentManager should be fine.
        passwordDialog.show(getChildFragmentManager(), "PasswordInputDialog");

        // Hide the current dialog's content view as PasswordInputDialogFragment will take over UI.
        // This is a bit of a hack. A cleaner way might be to have WalletUnlockDialogFragment be truly headless
        // or to replace its content view.
        if (dialog != null && dialog.getWindow() != null) {
            // dialog.hide(); // Hiding the dialog might be too abrupt.
            // Let PasswordInputDialogFragment overlay it.
        }
    }

    private void dismissSafely() {
        if (isAdded() && getDialog() != null && getDialog().isShowing()) {
            dismiss();
        } else if (isAdded()) {
            // If dialog not showing but fragment is added, try fragment transaction
            try {
                getParentFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error dismissing fragment: " + e.getMessage());
            }
        }
        // Ensure subject completes if not already
        if (!resultSubject.hasComplete() && !resultSubject.hasThrowable() && !resultSubject.hasObservers()) {
            // If no observers, it might have been completed already by an observer disposing.
            // Or if it's truly done and no one is listening, complete it.
            // However, onNext should be followed by onComplete in onDismiss.
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!resultSubject.hasThrowable() && !resultSubject.hasComplete()) {
            // If the dialog is dismissed for any reason (e.g. screen rotation if not handled)
            // and no explicit result was sent, consider it a cancellation.
            // However, setCancelable(false) should prevent most external dismissals.
            // This is a safeguard.
            // resultSubject.onNext(PasswordDialogResult.cancelled("Dialog dismissed unexpectedly."));
        }
        resultSubject.onComplete(); // Always complete the subject when the dialog is dismissed.
        disposables.clear();
    }

    @Override
    public void onDestroyView() {
        // Dialog is being destroyed, clear disposables
        disposables.clear();
        super.onDestroyView();
    }
}

