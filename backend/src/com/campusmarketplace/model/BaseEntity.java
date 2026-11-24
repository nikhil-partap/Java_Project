package com.campusmarketplace.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * OOP - ABSTRACT CLASS + INHERITANCE: every stored object (User, Listing,
 * BuyerRequest, Purchase) "is a" BaseEntity and therefore has an id.
 * POLYMORPHISM: toMap() is abstract; each subclass overrides it, and the JSON
 * writer just calls entity.toMap() without knowing the concrete type.
 */
public abstract class BaseEntity {

    // STATIC: one shared formatter for the whole class (not one per object).
    // Produces the same format as JavaScript's new Date().toISOString().
    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final String id; // ENCAPSULATION: private + final, read through a getter

    protected BaseEntity(String id) {
        this.id = id; // "this" distinguishes the field from the parameter
    }

    public String getId() {
        return id;
    }

    /** Field-name -> value map used by the JSON writer (names match the frontend). */
    public abstract Map<String, Object> toMap();

    /** STATIC utility method: current time as ISO-8601 text. */
    public static String nowIso() {
        return ISO.format(Instant.now());
    }

    @Override
    public String toString() {
        // STRINGS: String.format
        return String.format("%s[id=%s]", getClass().getSimpleName(), id);
    }
}
