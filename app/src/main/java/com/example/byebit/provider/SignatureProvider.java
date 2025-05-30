package com.example.byebit.provider;

import android.app.PendingIntent;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.byebit.SignatureConfirmActivity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Inside your app that performs the signing
public class SignatureProvider extends ContentProvider {

    public static final String AUTHORITY = "com.example.byebit.signatureprovider";
    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    public static final String METHOD_SIGN_MESSAGE = "sign_message";
    public static final String METHOD_CONFIRM_SIGNING_RESULT = "confirm_signing_result"; // New method for callback from confirmation Activity

    public static final String KEY_SIGNATURE_RESULT = AUTHORITY + ".signature_result";
    public static final String KEY_ERROR_MESSAGE = AUTHORITY + ".error_message";
    public static final String KEY_MESSAGE_TO_SIGN = "message_to_sign";
    public static final String KEY_REQUEST_ID = "request_id"; // To link initial request to confirmation
    public static final String KEY_IS_CONFIRMED = "is_confirmed"; // From confirmation activity
    public static final String KEY_SELECTED_WALLET_ADDRESS = "selected_wallet_address"; // From confirmation activity
    public static final String KEY_SIGNATURE = "signature"; // From confirmation activity
    public static final String KEY_CLIENT_CALLBACK_PENDING_INTENT = "client_callback_pending_intent"; // Provided by the calling app

    // A temporary store for pending signing requests, keyed by request ID.
    // In a real app, this should be more robust (e.g., SQLite, persistant storage, or handle IPC state properly).
    // Using a Map<String, byte[]> here to store message_to_sign for the request_id.
    // Also store the client's PendingIntent so we can call it back.
    private static final Map<String, PendingRequestData> pendingRequests = new ConcurrentHashMap<>();

    private static class PendingRequestData {
        byte[] messageToSign;
        PendingIntent clientCallbackPendingIntent;

        PendingRequestData(byte[] messageToSign, PendingIntent clientCallbackPendingIntent) {
            this.messageToSign = messageToSign;
            this.clientCallbackPendingIntent = clientCallbackPendingIntent;
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "";
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    // ... (query, getType, insert, delete, update - likely unimplemented or return null) ...

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        Bundle resultBundle = new Bundle();

        if (METHOD_SIGN_MESSAGE.equals(method)) {
            // --- Step 1: Initial request from client app ---
            if (extras == null || !extras.containsKey(KEY_MESSAGE_TO_SIGN) || !extras.containsKey(KEY_CLIENT_CALLBACK_PENDING_INTENT)) {
                resultBundle.putString(KEY_ERROR_MESSAGE, "Missing message or client callback.");
                return resultBundle;
            }

            byte[] messageToSign = extras.getByteArray(KEY_MESSAGE_TO_SIGN);
            PendingIntent clientCallbackPendingIntent = extras.getParcelable(KEY_CLIENT_CALLBACK_PENDING_INTENT);

            if (messageToSign == null || clientCallbackPendingIntent == null) {
                resultBundle.putString(KEY_ERROR_MESSAGE, "Invalid message or callback.");
                return resultBundle;
            }

            // Generate a unique ID for this signing request
            String requestId = UUID.randomUUID().toString();
            pendingRequests.put(requestId, new PendingRequestData(messageToSign, clientCallbackPendingIntent));

            // --- Step 2: Launch the Confirmation Activity ---
            Intent confirmationIntent = new Intent(getContext(), SignatureConfirmActivity.class);
            confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Necessary when launching from non-Activity context
            confirmationIntent.putExtra(KEY_MESSAGE_TO_SIGN, messageToSign);
            confirmationIntent.putExtra(KEY_REQUEST_ID, requestId);
            // No need to pass clientCallbackPendingIntent to SignatureConfirmActivity
            // as it will call back to METHOD_CONFIRM_SIGNING_RESULT on this provider.

            getContext().startActivity(confirmationIntent);

            // --- Step 3: Return immediately, indicating confirmation is pending ---
            resultBundle.putString(KEY_ERROR_MESSAGE, "Confirmation pending via UI.");
            resultBundle.putString(KEY_REQUEST_ID, requestId); // Client can optionally track this
            return resultBundle;

        } else if (METHOD_CONFIRM_SIGNING_RESULT.equals(method)) {
            // --- Step 4: Callback from SignatureConfirmActivity after user interaction ---
            if (extras == null || !extras.containsKey(KEY_REQUEST_ID) || !extras.containsKey(KEY_IS_CONFIRMED)) {
                Log.e("SignatureProvider", "Invalid data from confirmation activity.");
                return null; // Or return an error bundle if the caller expects it
            }

            String requestId = extras.getString(KEY_REQUEST_ID);
            boolean isConfirmed = extras.getBoolean(KEY_IS_CONFIRMED);
            String selectedWalletId = extras.getString(KEY_SELECTED_WALLET_ADDRESS);
            String signature = extras.getString(KEY_SIGNATURE);

            PendingRequestData requestData = pendingRequests.remove(requestId); // Remove once processed
            if (requestData == null) {
                Log.e("SignatureProvider", "Request ID not found or already processed: " + requestId);
                return null; // Or error
            }

            byte[] originalMessage = requestData.messageToSign;
            PendingIntent clientCallbackPendingIntent = requestData.clientCallbackPendingIntent;

            try {
                Intent clientResultIntent = new Intent();
                if (isConfirmed) {
                    // Perform the actual signing operation securely here
                    clientResultIntent.putExtra(KEY_SIGNATURE_RESULT, signature);
                    Log.d("SignatureProvider", "Message signed for requestId: " + requestId);
                } else {
                    clientResultIntent.putExtra(KEY_ERROR_MESSAGE, "User denied signing.");
                    Log.d("SignatureProvider", "Signing denied by user for requestId: " + requestId);
                }

                // --- Step 5: Send the final result back to the original client app ---
                // The client app receives this via its PendingIntent (e.g., BroadcastReceiver)
                clientCallbackPendingIntent.send(getContext(), 0, clientResultIntent);

            } catch (PendingIntent.CanceledException e) {
                Log.e("SignatureProvider", "Client callback pending intent was canceled.", e);
            } catch (Exception e) {
                Log.e("SignatureProvider", "Signing failed during confirmation callback for requestId: " + requestId, e);
                try {
                    Intent errorIntent = new Intent();
                    errorIntent.putExtra(KEY_ERROR_MESSAGE, "Signing failed: " + e.getMessage());
                    clientCallbackPendingIntent.send(getContext(), 0, errorIntent);
                } catch (PendingIntent.CanceledException ignored) {}
            }
            return null; // No synchronous return needed for this callback method
        } else {
            // Unknown method call
            resultBundle.putString(KEY_ERROR_MESSAGE, "Unknown method: " + method);
            return resultBundle;
        }
    }

    // Placeholder for actual signing logic (do NOT use in production)
    private byte[] simulateSigning(byte[] message, String selectedWalletId) {
        String simulatedSig = "SIMULATED_SIGNATURE_FOR_" + new String(message) + "_FROM_WALLET_" + selectedWalletId + "_CONFIRMED";
        return simulatedSig.getBytes();
    }
}
