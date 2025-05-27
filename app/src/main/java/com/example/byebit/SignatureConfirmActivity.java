package com.example.byebit;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.byebit.provider.SignatureProvider;
import com.example.byebit.ui.dialog.ConfirmationDialogFragment;

public class SignatureConfirmActivity extends AppCompatActivity {

    private String requestId;
    private byte[] messageToSign;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set a transparent theme for this activity to make it look like a dialog host
        // E.g., in AndroidManifest.xml: android:theme="@android:style/Theme.DeviceDefault.Dialog.NoActionBar"
        // or a custom theme: <item name="android:windowBackground">@android:color/transparent</item>
        // <item name="android:windowIsFloating">true</item>

        // Get data from the Intent that launched this Activity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(SignatureProvider.KEY_REQUEST_ID) &&
                intent.hasExtra(SignatureProvider.KEY_MESSAGE_TO_SIGN)) {

            requestId = intent.getStringExtra(SignatureProvider.KEY_REQUEST_ID);
            messageToSign = intent.getByteArrayExtra(SignatureProvider.KEY_MESSAGE_TO_SIGN);

            // Show your DialogFragment
            if (savedInstanceState == null) {
                ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(messageToSign);
                dialogFragment.show(getSupportFragmentManager(), "ConfirmationDialog");
            }
        } else {
            // Handle error, finish activity
            Log.e("SignatureConfirmActivity", "Missing required intent extras.");
            finish();
        }
    }

    // Callback from the DialogFragment
    public void onUserConfirmation(boolean confirmed) {
        // Call back to the ContentProvider to inform about the user's decision
        ContentResolver contentResolver = getContentResolver();
        Bundle extras = new Bundle();
        extras.putString(SignatureProvider.KEY_REQUEST_ID, requestId);
        extras.putBoolean(SignatureProvider.KEY_IS_CONFIRMED, confirmed);

        try {
            contentResolver.call(
                    SignatureProvider.BASE_URI,
                    SignatureProvider.METHOD_CONFIRM_SIGNING_RESULT,
                    null, // No string argument
                    extras
            );
        } catch (SecurityException e) {
            Log.e("SignatureConfirmActivity", "Permission denied to call back to provider: " + e.getMessage());
            // This should ideally not happen if permissions are set correctly within the same app
        } catch (Exception e) {
            Log.e("SignatureConfirmActivity", "Error calling provider callback: " + e.getMessage());
        }

        // Finish this Activity as its purpose is done
        finish();
    }
}
