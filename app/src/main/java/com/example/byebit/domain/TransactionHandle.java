package com.example.byebit.domain;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity(
        foreignKeys = @ForeignKey(
                entity = WalletHandle.class,
                parentColumns = "id",
                childColumns = "walletId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = {"hash"}, unique = true), @Index(value = {"walletId"})}
)
public class TransactionHandle {

    @PrimaryKey
    @NonNull
    private final UUID id;

    @NonNull
    private final String hash;

    @NonNull
    private final String direction; // "Send" or "Received"

    @NonNull
    private final String status; // "Pending", "Success", "Failure"

    @NonNull
    private final UUID walletId;

    @NonNull
    private final String senderAddress;

    private final String receiverAddress;

    @NonNull
    private final BigDecimal transactionAmount;

    private final BigDecimal transactionFee;

    private final String blockchainType;

    @NonNull
    private final Instant timestamp;

    // --- Constructor ---
    public TransactionHandle(@NonNull UUID id,
                             @NonNull String hash,
                             @NonNull String direction,
                             @NonNull String status,
                             @NonNull UUID walletId,
                             @NonNull String senderAddress,
                             String receiverAddress,
                             @NonNull BigDecimal transactionAmount,
                             BigDecimal transactionFee,
                             String blockchainType,
                             @NonNull Instant timestamp) {
        this.id = id;
        this.hash = hash;
        this.direction = direction;
        this.status = status;
        this.walletId = walletId;
        this.senderAddress = senderAddress;
        this.receiverAddress = receiverAddress;
        this.transactionAmount = transactionAmount;
        this.transactionFee = transactionFee;
        this.blockchainType = blockchainType;
        this.timestamp = timestamp;
    }

    // --- Gettery ---
    @NonNull
    public UUID getId() {
        return id;
    }

    @NonNull
    public String getDirection() {
        return direction;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    @NonNull
    public UUID getWalletId() {
        return walletId;
    }

    @NonNull
    public String getSenderAddress() {
        return senderAddress;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    @NonNull
    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public BigDecimal getTransactionFee() {
        return transactionFee;
    }

    public String getBlockchainType() {
        return blockchainType;
    }

    @NonNull
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "TransactionHandle{" +
                "direction='" + direction + '\'' +
                ", status='" + status + '\'' +
                ", amount=" + transactionAmount +
                ", timestamp=" + timestamp +
                '}';
    }

    @NonNull
    public String getHash() {
        return hash;
    }
}
