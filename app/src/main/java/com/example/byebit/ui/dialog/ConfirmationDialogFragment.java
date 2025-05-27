package com.example.byebit.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LiveData;

// Inside your app that performs the signing
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.example.byebit.domain.WalletHandle;
import java.util.ArrayList;
import java.util.List;

import android.widget.AdapterView;


public class ConfirmationDialogFragment extends DialogFragment {

    public interface ConfirmationDialogListener {
        void onUserConfirmation(boolean confirmed, String selectedWalletId);
    }

    private static final String ARG_MESSAGE = "message_to_display";
    private static final String ARG_WALLETS = "wallet_handles";

    private ConfirmationDialogListener listener;
    private WalletHandle selectedWallet;
    private LiveData<List<WalletHandle>> walletHandles;

    private ArrayAdapter<String> adapter;

    public static ConfirmationDialogFragment newInstance(byte[] message, LiveData<List<WalletHandle>> walletHandles) {
        ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putByteArray(ARG_MESSAGE, message);
        fragment.walletHandles = walletHandles;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ConfirmationDialogListener) {
            listener = (ConfirmationDialogListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ConfirmationDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        byte[] messageBytes = getArguments() != null ? getArguments().getByteArray(ARG_MESSAGE) : null;
        String messagePreview = (messageBytes != null) ? new String(messageBytes) : "No message provided.";

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(com.example.byebit.R.layout.dialog_confirm_signing, null);
        TextView messageTextView = view.findViewById(com.example.byebit.R.id.message_preview_text_view);
        Spinner walletSpinner = view.findViewById(com.example.byebit.R.id.wallet_spinner);

        messageTextView.setText(messagePreview);

        adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        walletSpinner.setAdapter(adapter);


        walletSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedWallet = walletHandles.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedWallet = null;
            }
        });

        selectedWallet = walletHandles.get(0); // Select the first wallet by default
        walletHandles.observe(getViewLifecycleOwner(), walletHandles -> {
        });

        builder.setView(view)
                .setTitle("Confirm Message Signing")
                .setPositiveButton("Sign", (dialog, id) -> {
                    if (listener != null) {
                        listener.onUserConfirmation(true, selectedWallet != null ? selectedWallet.getId().toString() : null);
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    if (listener != null) {
                        listener.onUserConfirmation(false, null);
                    }
                });
        return builder.create();
    }

    @Nullable
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        walletHandles.observe(getViewLifecycleOwner(), walletHandles -> {
            if (walletHandles != null && !walletHandles.isEmpty()) {
                walletHandles.stream()
                        .map(WalletHandle::getName)
                        .forEach(adapter::add);
            } else {
                // No wallets available, disable sign button and show a message
                walletSpinner.setVisibility(View.GONE);
                view.findViewById(com.example.byebit.R.id.wallet_selection_label).setVisibility(View.GONE);
                messageTextView.setText("No wallets available to sign with.");
                builder.setPositiveButton("OK", (dialog, id) -> {
                    if (listener != null) {
                        listener.onUserConfirmation(false, null);
                    }
                });
                builder.setNegativeButton("Cancel", (dialog, id) -> {
                    if (listener != null) {
                        listener.onUserConfirmation(false, null);
                    }
                });
            }
        });
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        // User dismissed dialog (e.g., back button)
        if (listener != null) {
            listener.onUserConfirmation(false, null);
        }
    }
}
