package com.example.byebit.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.byebit.R;

public class TransactionDetailsDialogFragment extends DialogFragment {

    private static final String ARG_TRANSACTION_ID = "transactionId";
    private static final String ARG_TIMESTAMP = "timestamp";
    private static final String ARG_SENDER = "sender";
    private static final String ARG_RECEIVER = "receiver";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_FEE = "fee";
    private static final String ARG_BLOCKCHAIN = "blockchain";

    public static TransactionDetailsDialogFragment newInstance(String transactionId, String timestamp,
                                                               String sender, String receiver,
                                                               String amount, String fee, String blockchain) {
        TransactionDetailsDialogFragment fragment = new TransactionDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TRANSACTION_ID, transactionId);
        args.putString(ARG_TIMESTAMP, timestamp);
        args.putString(ARG_SENDER, sender);
        args.putString(ARG_RECEIVER, receiver);
        args.putString(ARG_AMOUNT, amount);
        args.putString(ARG_FEE, fee);
        args.putString(ARG_BLOCKCHAIN, blockchain);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args == null) return super.onCreateDialog(savedInstanceState);

        String message =
                "Transaction Id:\n" + args.getString(ARG_TRANSACTION_ID) + "\n\n" +
                        "Timestamp:\n" + args.getString(ARG_TIMESTAMP) + "\n\n" +
                        "Sender Address:\n" + args.getString(ARG_SENDER) + "\n\n" +
                        "Receiver Address:\n" + args.getString(ARG_RECEIVER) + "\n\n" +
                        "Transaction Amount:\n" + args.getString(ARG_AMOUNT) + " ETH\n\n" +
                        "Transaction Fee:\n" + args.getString(ARG_FEE) + " ETH\n\n" +
                        "Blockchain Type:\n" + args.getString(ARG_BLOCKCHAIN);

        return new AlertDialog.Builder(requireContext())
                .setTitle("Transaction details")
                .setMessage(message)
                .setPositiveButton(R.string.close_button, (dialog, which) -> dialog.dismiss())
                .create();
    }
}
