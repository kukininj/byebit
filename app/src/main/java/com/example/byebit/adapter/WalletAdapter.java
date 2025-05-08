package com.example.byebit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.byebit.R;
import com.example.byebit.domain.WalletHandle;

import java.util.ArrayList;
import java.util.List;
// java.math.BigDecimal import is not strictly needed here if only calling toPlainString()
import android.widget.Button;
import java.math.RoundingMode;
import android.text.format.DateUtils;

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.WalletViewHolder> {

    private List<WalletHandle> wallets = new ArrayList<>();
    // Add this interface definition inside the WalletAdapter class
    public interface OnItemClickListener {
        void onItemClick(WalletHandle wallet);
    }

    // Add this field inside the WalletAdapter class
    private OnItemClickListener listener;

    // ADD: Interface for long click listener
    public interface OnItemLongClickListener {
    }

    // ADD: Field for long click listener
    private OnItemLongClickListener longClickListener;

    // Add this interface definition inside the WalletAdapter class
    public interface OnDetailsClickListener {
        void onDetailsClick(WalletHandle wallet);
    }

    // Add this field inside the WalletAdapter class
    private OnDetailsClickListener detailsClickListener;


    @NonNull
    @Override
    public WalletViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.wallet_list_item, parent, false);
        return new WalletViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull WalletViewHolder holder, int position) {
        WalletHandle currentWallet = wallets.get(position);
        holder.textViewName.setText(currentWallet.getName());
        holder.textViewAddress.setText(currentWallet.getAddress());

        if (currentWallet.getBalance() != null) {
            String balanceStr = currentWallet.getBalance().setScale(4, RoundingMode.HALF_UP).toPlainString() + " ETH";
            holder.textViewBalance.setText("Balance: " + balanceStr);
        } else {
            holder.textViewBalance.setText("Balance: N/A");
        }

        Long lastUpdatedTimestamp = currentWallet.getBalanceLastUpdated();
        if (lastUpdatedTimestamp != null && lastUpdatedTimestamp > 0) {
            CharSequence lastUpdatedStr = DateUtils.getRelativeTimeSpanString(
                    lastUpdatedTimestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            holder.textViewBalanceLastUpdated.setText("Last updated: " + lastUpdatedStr);
        } else {
            holder.textViewBalanceLastUpdated.setText("Last updated: N/A");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentWallet);
            }
        });

        if (holder.buttonDetails != null) {
            holder.buttonDetails.setOnClickListener(v -> {
                if (detailsClickListener != null) {
                    detailsClickListener.onDetailsClick(currentWallet);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return wallets.size();
    }

    // Method to update the list of wallets in the adapter
    public void setWallets(List<WalletHandle> wallets) {
        this.wallets = wallets;
        notifyDataSetChanged(); // Notify the adapter that the data set has changed
    }

    // Add this method inside the WalletAdapter class
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // ADD: Setter for the long click listener
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    // Add this setter method inside the WalletAdapter class
    public void setOnDetailsClickListener(OnDetailsClickListener listener) {
        this.detailsClickListener = listener;
    }


    // ViewHolder class
    static class WalletViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewName;
        private final TextView textViewAddress;
        // Add this field for the balance TextView
        private final TextView textViewBalance;
        // ADD THIS field for the balance last updated TextView
        private final TextView textViewBalanceLastUpdated;
        private final Button buttonDetails; // ADD THIS

        public WalletViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.wallet_name);
            textViewAddress = itemView.findViewById(R.id.wallet_address);
            // Initialize the balance TextView
            textViewBalance = itemView.findViewById(R.id.wallet_balance);
            // INITIALIZE the balance last updated TextView
            textViewBalanceLastUpdated = itemView.findViewById(R.id.wallet_balance_last_updated);
            buttonDetails = itemView.findViewById(R.id.button_details); // INITIALIZE THIS
        }
    }
}
