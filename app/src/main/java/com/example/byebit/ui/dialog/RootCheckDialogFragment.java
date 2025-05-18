package com.example.byebit.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.byebit.R;

import java.io.File;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class RootCheckDialogFragment extends DialogFragment {

    private static final String TAG = "RootCheckDialog";

    private TextView messageTextView;
    private ProgressBar progressBar;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public static RootCheckDialogFragment newInstance() {
        return new RootCheckDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_root_check, null); // Use your layout

        messageTextView = view.findViewById(R.id.messageTextView);
        progressBar = view.findViewById(R.id.progressBar);

        builder.setView(view)
                .setTitle(R.string.root_check_title)
                .setPositiveButton(R.string.ok, (dialog, id) -> dismiss());

        // Start the root check when the dialog is created
        performRootCheck();

        return builder.create();
    }

    private void performRootCheck() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (messageTextView != null) {
            messageTextView.setText(R.string.checking_root_status);
        }


        disposables.add(
                Single.fromCallable(this::isDeviceRooted)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.from(getContext().getMainExecutor()))
                        .subscribe(
                                isRooted -> {
                                    if (progressBar != null) {
                                        progressBar.setVisibility(View.GONE);
                                    }
                                    if (isRooted) {
                                        messageTextView.setText(R.string.device_is_rooted_warning);
                                        // You could also change text color, icon, etc.
                                    } else {
                                        messageTextView.setText(R.string.device_not_rooted);
                                    }
                                },
                                throwable -> {
                                    if (progressBar != null) {
                                        progressBar.setVisibility(View.GONE);
                                    }
                                    Log.e(TAG, "Error checking for root", throwable);
                                    messageTextView.setText(R.string.error_checking_root);
                                }
                        )
        );
    }

    /**
     * Checks for the presence of su binary in common locations.
     * This is a basic check and can be bypassed. For more robust checks,
     * consider libraries like RootBeer.
     */
    private boolean isDeviceRooted() {
        // Simulate some work for the progress bar
        try {
            Thread.sleep(1000); // Simulate delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String[] suPaths = {
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
        };

        for (String path : suPaths) {
            if (new File(path).exists()) {
                Log.d(TAG, "Found su binary at: " + path);
                return true;
            }
        }
        Log.d(TAG, "No su binary found in common paths.");
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Dispose of any RxJava subscriptions to prevent memory leaks
        disposables.clear();
    }
}
