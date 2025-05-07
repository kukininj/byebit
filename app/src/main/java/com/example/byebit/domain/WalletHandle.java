package com.example.byebit.domain;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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

    // Add this field
    private String balance;

    // Constructor updated for Room compatibility and new fields
    public WalletHandle(@NonNull UUID id, @NonNull String name, @NonNull String filename, @NonNull String address) {
        this.id = id;
        this.name = name;
        this.filename = filename;
        this.address = address;
        // Initialize balance
        this.balance = "0"; // Or null, if you prefer to explicitly check for null
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

    // Add these getter and setter methods for balance
    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }
}
