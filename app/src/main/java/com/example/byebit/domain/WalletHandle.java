package com.example.byebit.domain;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Add this import
import java.math.BigDecimal;
import java.util.UUID;

// Note: @TypeConverters annotation is now on AppDatabase
@Entity
public class WalletHandle {

    @PrimaryKey
    @NonNull
    private final UUID id;

    @NonNull
    private final String name;

    @NonNull
    private final String filename; // Renamed from path

    @NonNull
    private final String address; // Added address field

    // MODIFIED: Change type from String to BigDecimal
    private BigDecimal balance;

    private Long balanceLastUpdated;

    public Long getBalanceLastUpdated() {
        return balanceLastUpdated;
    }

    public void setBalanceLastUpdated(Long balanceLastUpdated) {
        this.balanceLastUpdated = balanceLastUpdated;
    }

    // Constructor updated for Room compatibility and new fields
    public WalletHandle(@NonNull UUID id, @NonNull String name, @NonNull String filename, @NonNull String address) {
        this.id = id;
        this.name = name;
        this.filename = filename;
        this.address = address;
        // MODIFIED: Initialize balance with BigDecimal.ZERO
        this.balance = BigDecimal.ZERO;
    }

    // --- Getters ---

    @NonNull
    public UUID getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getFilename() {
        return filename;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    // MODIFIED: Update getter and setter for balance
    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
