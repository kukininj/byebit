package com.example.byebit.domain;

import androidx.room.Embedded;
import androidx.room.Relation;

public class TransactionWithWallet {
    @Embedded
    public TransactionHandle transaction;

    @Relation(
        parentColumn = "walletId",
        entityColumn = "id"
    )
    public WalletHandle wallet;
}
