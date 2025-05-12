package com.example.byebit.security;

import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.widget.Toast;

import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;

import com.example.byebit.domain.WalletHandle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class BiometricService {

    private static String TAG = "BiometricService";
    private Context context;
    private Executor executor;
    private Fragment fragment;

    public BiometricService(Fragment fragment) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        assert context != null;
        this.executor = context.getMainExecutor();
    }

    public void decrypt(WalletHandle wallet, AuthenticationListener listener) {
        if (wallet.getEncryptedPassword() == null) {
            listener.onFailure(AuthenticationFailureReason.PASSWORD_NOT_STORED);
            return;
        }

        BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.d(TAG, "Authentication succeeded!");
                if (result.getCryptoObject() != null && result.getCryptoObject().getCipher() != null) {
                    Cipher cipher = result.getCryptoObject().getCipher();
                    try {
                        byte[] bytes = cipher.doFinal(wallet.getEncryptedPassword());
                        listener.onSuccess(bytes, cipher.getIV());
                    } catch (BadPaddingException | IllegalBlockSizeException e) {
                        Log.e(TAG, "Decryption failed: " + e.getMessage(), e);
                        listener.onFailure(AuthenticationFailureReason.DECRYPTION_FAILED);
                    } catch (Exception e) {
                        Log.e(TAG, "Error during decryption operation: " + e.getMessage(), e);
                        listener.onFailure(AuthenticationFailureReason.INTERNAL_ERROR);
                    }
                } else {
                    Log.e(TAG, "Authentication succeeded but crypto object or cipher is null.");
                    listener.onFailure(AuthenticationFailureReason.INTERNAL_ERROR);
                }
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // TODO: add more reasons
                try {
                    listener.onFailure(AuthenticationFailureReason.INTERNAL_ERROR);
                } finally {
                    Log.e(TAG, "Authentication error: " + errString);
                    Toast.makeText(context, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                try {
                    listener.onFailure(AuthenticationFailureReason.AUTHENTICATION_FAILED);
                } finally {
                    Log.e(TAG, "Authentication failed");
                    Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            }
        };

        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            SecretKey key = (SecretKey) keyStore.getKey(wallet.getName(), null);
            if (key == null) {
                Log.e(TAG, "Key not found in Keystore for alias: " + wallet.getName());
                listener.onFailure(AuthenticationFailureReason.KEYSTORE_ERROR);
                return;
            }

            byte[] iv = wallet.getIv();
            if (iv == null) {
                Log.e(TAG, "IV is null for wallet: " + wallet.getName());
                listener.onFailure(AuthenticationFailureReason.CRYPTO_SETUP_FAILED);
                return;
            }
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            BiometricPrompt biometricPrompt = new BiometricPrompt(fragment, executor, callback);
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
            listener.onFailure(AuthenticationFailureReason.CRYPTO_SETUP_FAILED);
        }
    }

    public void encrypt(String name, String password, AuthenticationListener listener) {
        BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.d(TAG, "Authentication succeeded!");
                if (result.getCryptoObject() != null && result.getCryptoObject().getCipher() != null) {
                    Cipher cipher = result.getCryptoObject().getCipher();
                    try {
                        byte[] ciphertext = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
                        listener.onSuccess(ciphertext, cipher.getIV());
                    } catch (BadPaddingException | IllegalBlockSizeException e) {
                        Log.e(TAG, "Encryption failed: " + e.getMessage(), e);
                        listener.onFailure(AuthenticationFailureReason.ENCRYPTION_FAILED);
                    } catch (Exception e) {
                        Log.e(TAG, "Error during encryption operation: " + e.getMessage(), e);
                        listener.onFailure(AuthenticationFailureReason.INTERNAL_ERROR);
                    }
                } else {
                    Log.e(TAG, "Authentication succeeded but crypto object or cipher is null.");
                    listener.onFailure(AuthenticationFailureReason.INTERNAL_ERROR);
                }
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // TODO: add more reasons
                try {
                    listener.onFailure(AuthenticationFailureReason.INTERNAL_ERROR);
                } finally {
                    Log.e(TAG, "Authentication error: " + errString);
                    Toast.makeText(context, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                try {
                    listener.onFailure(AuthenticationFailureReason.AUTHENTICATION_FAILED);
                } finally {
                    Log.e(TAG, "Authentication failed");
                    Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            }
        };
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            SecretKey key = getOrGenerateKey(name);
            if (key == null) {
                Log.e(TAG, "Failed to get or generate key for alias: " + name);
                listener.onFailure(AuthenticationFailureReason.KEYSTORE_ERROR);
                return;
            }

            cipher.init(Cipher.ENCRYPT_MODE, key);

            BiometricPrompt biometricPrompt = new BiometricPrompt(fragment, executor, callback);
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock to use encryption")
                    .setSubtitle("Confirm your screen lock")
                    .setAllowedAuthenticators(DEVICE_CREDENTIAL)
                    .build();

            biometricPrompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(cipher));

        } catch (IOException | GeneralSecurityException e) {
            Log.e(TAG, "Error setting up encryption: " + e.getMessage(), e);
            listener.onFailure(AuthenticationFailureReason.CRYPTO_SETUP_FAILED);
        }
    }

    private SecretKey getOrGenerateKey(String alias) throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(alias)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            KeyGenParameterSpec.Builder specBuilder = new KeyGenParameterSpec.Builder(alias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                    .setUserAuthenticationParameters(15 * 60, // 15 minutes timeout
                            KeyProperties.AUTH_DEVICE_CREDENTIAL);

            KeyGenParameterSpec spec = specBuilder.build();

            keyGenerator.init(spec);
            keyGenerator.generateKey();
        }

        return (SecretKey) keyStore.getKey(alias, null);
    }
}
