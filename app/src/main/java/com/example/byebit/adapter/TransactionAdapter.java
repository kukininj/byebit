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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_DATE_HEADER = 0;
    private static final int VIEW_TYPE_TRANSACTION = 1;

    private final List<Object> items = new ArrayList<>();
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
            items.add(group.getDateLabel());
            items.addAll(group.getTransactions());
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? VIEW_TYPE_DATE_HEADER : VIEW_TYPE_TRANSACTION;
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
        } else {
            View view = inflater.inflate(R.layout.transaction_list_item, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).bind((String) items.get(position));
        } else {
            ((TransactionViewHolder) holder).bind((TransactionHandle) items.get(position));
        }
    }

    // ---------- VIEW HOLDER FOR DATE HEADER ----------
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

    // ---------- VIEW HOLDER FOR TRANSACTION ----------
    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView directionIcon;
        private final TextView directionText;
        private final TextView statusText;
        private final TextView amountText;
        private final TextView timeText;
        private final Button detailsButton;

        public TransactionViewHolder(View itemView) {
            super(itemView);
            directionIcon = itemView.findViewById(R.id.direction_icon);
            directionText = itemView.findViewById(R.id.text_direction);
            statusText = itemView.findViewById(R.id.text_status);
            amountText = itemView.findViewById(R.id.text_amount);
            timeText = itemView.findViewById(R.id.text_time);
            detailsButton = itemView.findViewById(R.id.button_details);
        }

        public void bind(TransactionHandle tx) {
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

            detailsButton.setOnClickListener(v -> {
                if (detailsClickListener != null) {
                    detailsClickListener.onDetailsClick(tx);
                }
            });
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
