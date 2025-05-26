package com.example.byebit.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.byebit.R;
import com.example.byebit.domain.GroupedTransaction;
import com.example.byebit.domain.TransactionHandle;
import com.example.byebit.domain.TransactionWithWallet;
import com.example.byebit.domain.WalletHandle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_DATE_HEADER = 0;
    private static final int VIEW_TYPE_TRANSACTION = 1;

    // --- New: Define interface for list items ---
    public interface ListItem {}

    // --- New: Wrapper class for Date Header ---
    public static class DateHeaderItem implements ListItem {
        private final String dateLabel;

        public DateHeaderItem(String dateLabel) {
            this.dateLabel = dateLabel;
        }

        public String getDateLabel() {
            return dateLabel;
        }
    }

    // --- New: Wrapper class for Transaction ---
    public static class TransactionItem implements ListItem {
        private final TransactionHandle transaction;
        private final WalletHandle walletHandle;

        public TransactionItem(TransactionHandle transaction, WalletHandle walletHandle) {
            this.transaction = transaction;
            this.walletHandle = walletHandle;
        }

        public TransactionHandle getTransaction() {
            return transaction;
        }
    }
    // --- End of new classes ---


    private final List<ListItem> items = new ArrayList<>(); // Changed to List<ListItem>
    private final OnTransactionDetailsClickListener detailsClickListener;

    public interface OnTransactionDetailsClickListener {
        void onDetailsClick(TransactionHandle transaction);
    }

    public TransactionAdapter(List<GroupedTransaction> groupedTransactions, OnTransactionDetailsClickListener listener) {
        this.detailsClickListener = listener;
        setGroupedTransactions(groupedTransactions);
    }

    public void setGroupedTransactions(List<GroupedTransaction> groupedList) {
        items.clear();
        for (GroupedTransaction group : groupedList) {
            items.add(new DateHeaderItem(group.date)); // Wrap String in DateHeaderItem
            for (TransactionWithWallet result : group.transactions) {
                items.add(new TransactionItem(result.transaction, result.wallet)); // Wrap TransactionHandle in TransactionItem
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        // Check the type of the ListItem to determine the view type
        ListItem item = items.get(position);
        return item instanceof DateHeaderItem ? VIEW_TYPE_DATE_HEADER : VIEW_TYPE_TRANSACTION;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_DATE_HEADER) {
            View view = inflater.inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else { // VIEW_TYPE_TRANSACTION
            View view = inflater.inflate(R.layout.transaction_list_item, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = items.get(position); // Get the ListItem

        if (holder instanceof DateHeaderViewHolder) {
            DateHeaderItem dateHeaderItem = (DateHeaderItem) item; // Cast to DateHeaderItem
            ((DateHeaderViewHolder) holder).bind(dateHeaderItem.getDateLabel()); // Unwrap and bind
        } else if (holder instanceof TransactionViewHolder ) { // It must be a TransactionViewHolder
            TransactionItem transactionItem = (TransactionItem) item; // Cast to TransactionItem
            ((TransactionViewHolder) holder).bind(transactionItem.getTransaction(), transactionItem.walletHandle); // Unwrap and bind
        }
    }

    // DateHeaderViewHolder and TransactionViewHolder remain largely the same,
    // as their bind methods still expect String and TransactionHandle respectively.
    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateText;

        public DateHeaderViewHolder(View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.date_header_text);
        }

        public void bind(String date) {
            dateText.setText(date);
        }
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView directionIcon;
        private final TextView directionText;
        private final TextView statusText;
        private final TextView amountText;
        private final TextView timeText;
        private final Button detailsButton;
        private final TextView walletNameText; // This field was defined but not used in original code, keeping it for consistency

        public TransactionViewHolder(View itemView) {
            super(itemView);
            directionIcon = itemView.findViewById(R.id.direction_icon);
            directionText = itemView.findViewById(R.id.text_direction);
            statusText = itemView.findViewById(R.id.text_status);
            amountText = itemView.findViewById(R.id.text_amount);
            timeText = itemView.findViewById(R.id.text_time);
            detailsButton = itemView.findViewById(R.id.button_details);
            walletNameText = itemView.findViewById(R.id.text_wallet_name); // keeping for consistency
        }

        public void bind(TransactionHandle tx, WalletHandle wallet) {
            directionText.setText(tx.getDirection());
            directionText.setTypeface(null, Typeface.BOLD);

            switch (tx.getStatus()) {
                case "Success":
                    statusText.setTextColor(Color.parseColor("#4CAF50")); // green
                    break;
                case "Pending":
                    statusText.setTextColor(Color.parseColor("#2196F3")); // blue
                    break;
                case "Failure":
                    statusText.setTextColor(Color.parseColor("#F44336")); // red
                    break;
                default:
                    statusText.setTextColor(Color.DKGRAY);
            }

            statusText.setText(tx.getStatus());

            if ("Send".equalsIgnoreCase(tx.getDirection())) {
                directionIcon.setImageResource(R.drawable.ic_send);
                amountText.setText("-" + tx.getTransactionAmount() + " ETH");
            } else {
                directionIcon.setImageResource(R.drawable.ic_receive);
                amountText.setText("+" + tx.getTransactionAmount() + " ETH");
            }

            timeText.setText(formatTime(tx.getTimestamp().toEpochMilli()));
            walletNameText.setText(wallet.getName());

            detailsButton.setOnClickListener(v -> {
                if (detailsClickListener != null) {
                    detailsClickListener.onDetailsClick(tx);
                }
            });
            // The walletName TextView was found but not used in the original bind method.
            // If it's intended to display wallet name, you would add:
            // walletName.setText(tx.getWalletName()); // Assuming TransactionHandle has getWalletName()
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}