package com.example.byebit.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.byebit.R; // Assuming you have R.string.import_wallets_title, etc.
import com.example.byebit.ui.dashboard.DashboardViewModel; // Needed to get WalletImportService
import com.example.byebit.util.WalletImportService;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class WalletImportDialogFragment extends DialogFragment {

    public static final String TAG = "WalletImportDialog";
    private static final String ARG_ZIP_URI = "zipUri";

    private Uri zipUri;
    private WalletImportService walletImportService; // We'll get this from ViewModel or context
    private CompositeDisposable disposables = new CompositeDisposable();

    // RxJava Subject to emit dialog results
    private final PublishSubject<WalletImportResult> resultSubject = PublishSubject.create();

    // UI elements (optional, for progress/message)
    private TextView messageTextView;
    private ProgressBar progressBar;

    public static WalletImportDialogFragment newInstance(@NonNull Uri zipUri) {
        WalletImportDialogFragment fragment = new WalletImportDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ZIP_URI, zipUri);
        fragment.setArguments(args);
        return fragment;
    }

    public Observable<WalletImportResult> getDialogEvents() {
        return resultSubject.hide(); // Hide to prevent casting to Subject
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            zipUri = getArguments().getParcelable(ARG_ZIP_URI);
        }

        // Get WalletImportService. A ViewModelProvider is one way, or pass it in constructor/newInstance
        // For simplicity, let's assume DashboardViewModel provides it or we can instantiate it.
        // A better approach might be dependency injection or passing it via newInstance.
        // Let's get it via the application context for now, assuming it's a singleton or easily created.
        // If WalletImportService requires WalletHandleDao, you might need to get it from AppDatabase.
        // A more robust way is to get it from a ViewModel or a shared service locator.
        // Assuming WalletImportService can be created with just context:
        // walletImportService = new WalletImportService(requireContext().getApplicationContext());
        // Or, if it's provided by the ViewModel:
        DashboardViewModel dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        walletImportService = new WalletImportService(getContext()); // Assuming ViewModel has a getter for it

        // Make the dialog non-cancelable by touch outside or back button during import
        setCancelable(false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate the custom layout
        View view = inflater.inflate(R.layout.dialog_wallet_import_progress, null);

        // Find UI elements in the inflated layout
        progressBar = view.findViewById(R.id.progress_bar_import);

        // Set the custom layout as the dialog's view
        builder.setView(view);

        builder.setTitle(R.string.import_wallets_title);

        // No positive/negative buttons initially, as it's a progress dialog.
        // Buttons can be added later if needed (e.g., Cancel).

        Dialog dialog = builder.create();
        // Prevent dismissal by tapping outside or back button
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                // Optionally handle back button press - maybe show a "Cancel Import?" dialog
                dismissSafely();
                return true; // Consume the event
            }
            return false;
        });

        return dialog;
    }



    @Override
    public void onStart() {
        super.onStart();
        startImport();
    }

    private void startImport() {
        if (zipUri == null) {
            Log.e(TAG, "ZIP Uri is null, cannot start import.");
            emitResultAndDismiss(WalletImportResult.error("Invalid ZIP file selected."));
            return;
        }

        if (walletImportService == null) {
             Log.e(TAG, "WalletImportService is null, cannot start import.");
             emitResultAndDismiss(WalletImportResult.error("Import service not available."));
             return;
        }

        Log.d(TAG, "Starting import from URI: " + zipUri);

        disposables.add(walletImportService.importWalletsFromZip(zipUri)
                .subscribeOn(Schedulers.io()) // Perform import on a background thread
                .observeOn(Schedulers.from(getContext().getMainExecutor())) // Observe results on the main thread
                .subscribe(
                        importedWallets -> {
                            // onSuccess
                            Log.d(TAG, "Import successful. Imported " + importedWallets.size() + " wallets.");
                            emitResultAndDismiss(WalletImportResult.success(importedWallets));
                        },
                        error -> {
                            // onError
                            Log.e(TAG, "Import failed", error);
                            String errorMessage = "Import failed: " + (error.getMessage() != null ? error.getMessage() : "Unknown error");
                            emitResultAndDismiss(WalletImportResult.error(errorMessage));
                        }
                ));
    }

    private void emitResultAndDismiss(WalletImportResult result) {
        if (!resultSubject.hasComplete() && !resultSubject.hasThrowable()) {
            resultSubject.onNext(result);
            resultSubject.onComplete(); // Complete the stream after emitting the result
        }
        dismissSafely(); // Dismiss the dialog
    }

    private void dismissSafely() {
        // Check if the fragment is added and dialog is showing before dismissing
        if (isAdded() && getDialog() != null && getDialog().isShowing()) {
            dismiss();
        } else if (isAdded()) {
            // If dialog not showing but fragment is added, try fragment transaction
            // This can happen if dismiss() is called before the dialog is fully shown
            try {
                getParentFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error dismissing fragment: " + e.getMessage());
            }
        }
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // If the dialog is dismissed for any reason before a result is emitted,
        // emit a cancelled result. This handles cases like configuration changes
        // or explicit cancellation if a cancel button were added.
        if (!resultSubject.hasComplete() && !resultSubject.hasThrowable()) {
            resultSubject.onNext(WalletImportResult.cancelled());
            resultSubject.onComplete();
        }
        disposables.clear(); // Clear subscriptions on dismiss
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clear disposables here as well, in case onDismiss wasn't called (e.g., orientation change)
        disposables.clear();
        // Nullify UI elements to prevent memory leaks
        messageTextView = null;
        progressBar = null;
    }
}
