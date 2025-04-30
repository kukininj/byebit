package com.example.byebit.config;

import androidx.room.TypeConverter;
import java.util.UUID;

public class UuidConverter {
    @TypeConverter
    public static String fromUuid(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    @TypeConverter
    public static UUID toUuid(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }
}