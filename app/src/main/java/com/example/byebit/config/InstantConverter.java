package com.example.byebit.config;

import androidx.room.TypeConverter;

import java.time.Instant;

public class InstantConverter {

    @TypeConverter
    public static Instant fromTimestamp(String value) {
        return value == null ? null : Instant.parse(value);
    }

    @TypeConverter
    public static String dateToTimestamp(Instant instant) {
        return instant == null ? null : instant.toString(); // ISO-8601
    }
}