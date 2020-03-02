package com.example.atom.Library;

import androidx.room.TypeConverter;

import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Converters {
    private DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }
}
