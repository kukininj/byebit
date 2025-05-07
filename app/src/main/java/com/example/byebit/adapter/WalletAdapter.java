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

        // Set click listener on the item view
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

        public WalletViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.wallet_name);
            textViewAddress = itemView.findViewById(R.id.wallet_address);
        }
    }
}
