package com.example.byebit;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.example.byebit.domain.WalletHandle;
import com.example.byebit.provider.SignatureProvider;
import com.example.byebit.repository.WalletRepository;
import com.example.byebit.ui.dialog.ConfirmationDialogFragment;
import com.example.byebit.ui.dialog.WalletUnlockDialogFragment;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class SignatureConfirmActivity extends AppCompatActivity implements ConfirmationDialogFragment.ConfirmationDialogListener {

    private String requestId;
    private byte[] messageToSign;
    private WalletRepository walletRepository;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        walletRepository = new WalletRepository(this);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(SignatureProvider.KEY_REQUEST_ID) &&
                intent.hasExtra(SignatureProvider.KEY_MESSAGE_TO_SIGN)) {

            requestId = intent.getStringExtra(SignatureProvider.KEY_REQUEST_ID);
            messageToSign = intent.getByteArrayExtra(SignatureProvider.KEY_MESSAGE_TO_SIGN);

            if (savedInstanceState == null) {
                // Fetch wallets synchronously (consider async if UI thread blocking is an issue)
                LiveData<List<WalletHandle>> walletHandles = walletRepository.getSavedWallets();

                ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(messageToSign, walletHandles);

                dialogFragment.show(getSupportFragmentManager(), "ConfirmationDialog");
            }
        } else {
            Log.e("SignatureConfirmActivity", "Missing required intent extras.");
            finish();
        }
    }

    @Override
    public void onUserConfirmation(boolean confirmed, WalletHandle selectedWallet) {
        ContentResolver contentResolver = getContentResolver();
        byte[] signature = null;

        if (confirmed) {
            WalletUnlockDialogFragment fragment = WalletUnlockDialogFragment.newInstance(selectedWallet);

            disposables.add(
                    fragment.getDialogEvents()
                            .observeOn(Schedulers.io())
                            .subscribe(result -> {
                                if (result.isSuccess()) {
                                    walletRepository.getCredentials(
                                            selectedWallet,
                                            result.password
                                    );
                                }
                            })
            );


            fragment.show(getSupportFragmentManager(), "WalletUnlockDialogFragment");
        } else {
            Bundle extras = new Bundle();
            extras.putString(SignatureProvider.KEY_REQUEST_ID, requestId);
            extras.putBoolean(SignatureProvider.KEY_IS_CONFIRMED, confirmed);
            extras.putString(SignatureProvider.KEY_SELECTED_WALLET_ADDRESS, selectedWallet.getAddress());
            extras.putByteArray(SignatureProvider.KEY_SIGNATURE, signature);

            try {
                contentResolver.call(
                        SignatureProvider.BASE_URI,
                        SignatureProvider.METHOD_CONFIRM_SIGNING_RESULT,
                        null,
                        extras
                );
            } catch (SecurityException e) {
                Log.e("SignatureConfirmActivity", "Permission denied to call back to provider: " + e.getMessage());
            } catch (Exception e) {
                Log.e("SignatureConfirmActivity", "Error calling provider callback: " + e.getMessage());
            }
        }

        finish();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (walletRepository != null) {
            walletRepository.shutdown();
        }
    }
}
