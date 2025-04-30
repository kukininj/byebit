package com.example.byebit.domain;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity
public class WalletHandle {

    @PrimaryKey
    private final UUID id;

    private final String name;

    private final String path;

    public WalletHandle(UUID id, String name, String path) {
        this.id = id;
        this.name = name;
        this.path = path;
    }
}
