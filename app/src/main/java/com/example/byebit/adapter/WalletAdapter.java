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

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.WalletViewHolder> {

    private List<WalletHandle> wallets = new ArrayList<>();
    // Add this interface definition inside the WalletAdapter class
    public interface OnItemClickListener {
        void onItemClick(WalletHandle wallet);
    }

    // Add this field inside the WalletAdapter class
    private OnItemClickListener listener;

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

        // Set the balance text
        if (currentWallet.getBalance() != null) {
            // MODIFIED: Convert BigDecimal to String for display using toPlainString()
            // TODO: Consider formatting the balance (e.g., Wei to Ether)
            // For now, displaying the raw string value from the database.
            // You might want to add units like "ETH" or "Wei"
            holder.textViewBalance.setText("Balance: " + currentWallet.getBalance().toPlainString());
        } else {
            // Handle case where balance might be null (e.g., still loading or error)
            // This is less likely if initialized to BigDecimal.ZERO
            holder.textViewBalance.setText("Balance: N/A");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentWallet);
            }
        });
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

    // ViewHolder class
    static class WalletViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewName;
        private final TextView textViewAddress;
        // Add this field for the balance TextView
        private final TextView textViewBalance;

        public WalletViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.wallet_name);
            textViewAddress = itemView.findViewById(R.id.wallet_address);
            // Initialize the balance TextView
            textViewBalance = itemView.findViewById(R.id.wallet_balance);
        }
    }
}
