package com.campusmarketplace.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Abstract parent class for all stored entities. */
public abstract class BaseEntity {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final String id;

    protected BaseEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public abstract String toFileLine();

    public static String nowText() {
        return LocalDateTime.now().format(FMT);
    }
}
