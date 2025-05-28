package com.example.byebit.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.byebit.R;
import com.example.byebit.databinding.CreateTransactionSpinnerItemBinding;
import com.example.byebit.domain.WalletHandle;

import java.text.DecimalFormat;
import java.util.List;

public class SpinnerTransactionAdapter extends ArrayAdapter<WalletHandle> {
    private CreateTransactionSpinnerItemBinding binding;
    public SpinnerTransactionAdapter(@NonNull Context context, @NonNull List<WalletHandle> wallets) {
        super(context, 0, wallets);
    }

    private View getWalletView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            binding = CreateTransactionSpinnerItemBinding.inflate(LayoutInflater.from(getContext()), parent, false);
            convertView = binding.getRoot();
        }

        TextView walletName = binding.spinnerItemWalletName;
        TextView walletBalance = binding.spinnerItemBalance;

        WalletHandle wallet = getItem(position);
        walletName.setText(wallet.getName());
        DecimalFormat format = new DecimalFormat();
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(5);
        format.setGroupingUsed(false);
        walletBalance.setText(format.format(wallet.getBalance()));

        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getWalletView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getWalletView(position, convertView, parent);
    }

    public void setWallets(List<WalletHandle> wallets) {
        clear();
        addAll(wallets);
        notifyDataSetChanged();
    }
}
