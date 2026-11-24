package com.campusmarketplace.storage;

import com.campusmarketplace.model.BaseEntity;
import com.campusmarketplace.model.BuyerRequest;
import com.campusmarketplace.model.Listing;
import com.campusmarketplace.model.Purchase;
import com.campusmarketplace.model.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * FILE HANDLING: saves all marketplace data to plain text files so it survives a restart.
 *
 * Files created inside the data folder (one record per line, fields separated by '|'):
 *   users.txt      id|name|email|password|phone|department|year|mode|bio|role|blocked
 *   listings.txt   id|ownerId|title|category|price|condition|description|location|status|createdAt|imageUrl|images
 *   requests.txt   listingId|id|buyerId|message|offerPrice|status|createdAt
 *   purchases.txt  id|listingId|title|price|sellerId|buyerId|purchasedAt|imageUrl
 *
 * Uses: File, FileReader/BufferedReader, FileWriter/BufferedWriter, try-with-resources,
 * IOException handling, and String methods (split, replace, StringBuilder).
 */
public class FileStorage {

    private static final String SEPARATOR = "|";
    private static final String USERS = "users.txt";
    private static final String LISTINGS = "listings.txt";
    private static final String REQUESTS = "requests.txt";
    private static final String PURCHASES = "purchases.txt";

    private final File folder;

    public FileStorage(String folderName) {
        this.folder = new File(folderName);
    }

    /** True when data was saved before (users.txt exists), so we load instead of seeding. */
    public boolean hasSavedData() {
        return new File(folder, USERS).exists();
    }

    public String getFolderPath() {
        return folder.getAbsolutePath();
    }

    // ------------------------------------------------------------------ save

    /**
     * Writes every collection to its file (overwrites the old files).
     * synchronized: two HTTP threads must never write the same file at the same time.
     */
    public synchronized void saveAll(List<User> users, List<Listing> listings, List<Purchase> purchases)
            throws IOException {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Could not create folder " + folder.getAbsolutePath());
        }
        writeEntities(USERS, users);
        writeEntities(LISTINGS, listings);
        writeEntities(PURCHASES, purchases);

        // Buyer requests belong to a listing, so each line starts with the listing id.
        List<String> requestLines = new ArrayList<>();
        for (Listing listing : listings) {
            for (BuyerRequest request : listing.getBuyerRequests()) {
                requestLines.add(escape(listing.getId()) + SEPARATOR + toLine(request.toFields()));
            }
        }
        writeLines(REQUESTS, requestLines);
    }

    /** GENERICS + POLYMORPHISM: works for any entity because every entity has toFields(). */
    private void writeEntities(String fileName, List<? extends BaseEntity> items) throws IOException {
        List<String> lines = new ArrayList<>();
        for (BaseEntity item : items) {
            lines.add(toLine(item.toFields()));
        }
        writeLines(fileName, lines);
    }

    private void writeLines(String fileName, List<String> lines) throws IOException {
        // TRY-WITH-RESOURCES: the writer is closed automatically, even if an exception happens.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(folder, fileName)))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    // ------------------------------------------------------------------ load

    public List<User> loadUsers() throws IOException {
        List<User> users = new ArrayList<>();
        for (String[] f : readRecords(USERS)) {
            try {
                users.add(User.fromFields(f));
            } catch (RuntimeException e) { // bad line -> skip it, keep loading the rest
                System.out.println("Skipping bad line in " + USERS + ": " + e.getMessage());
            }
        }
        return users;
    }

    /** Loads listings and attaches their buyer requests from requests.txt. */
    public List<Listing> loadListings() throws IOException {
        List<Listing> listings = new ArrayList<>();
        for (String[] f : readRecords(LISTINGS)) {
            try {
                listings.add(Listing.fromFields(f));
            } catch (RuntimeException e) {
                System.out.println("Skipping bad line in " + LISTINGS + ": " + e.getMessage());
            }
        }
        for (String[] f : readRecords(REQUESTS)) {
            try {
                String listingId = f[0];
                String[] requestFields = new String[f.length - 1];
                System.arraycopy(f, 1, requestFields, 0, requestFields.length);
                BuyerRequest request = BuyerRequest.fromFields(requestFields);
                for (Listing listing : listings) {
                    if (listing.getId().equals(listingId)) {
                        listing.addRequestLast(request); // file order is already newest first
                        break;
                    }
                }
            } catch (RuntimeException e) {
                System.out.println("Skipping bad line in " + REQUESTS + ": " + e.getMessage());
            }
        }
        return listings;
    }

    public List<Purchase> loadPurchases() throws IOException {
        List<Purchase> purchases = new ArrayList<>();
        for (String[] f : readRecords(PURCHASES)) {
            try {
                purchases.add(Purchase.fromFields(f));
            } catch (RuntimeException e) {
                System.out.println("Skipping bad line in " + PURCHASES + ": " + e.getMessage());
            }
        }
        return purchases;
    }

    /** Reads a file line by line and splits each line into its (unescaped) fields. */
    private List<String[]> readRecords(String fileName) throws IOException {
        List<String[]> records = new ArrayList<>();
        File file = new File(folder, fileName);
        if (!file.exists()) {
            return records; // e.g. no purchases saved yet
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1); // -1 keeps empty fields at the end
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = unescape(parts[i]);
                }
                records.add(parts);
            }
        }
        return records;
    }

    // ------------------------------------------------------------------ text helpers

    private static String toLine(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    /**
     * STRINGS: makes a value safe to store on one line. Backslash, '|', ',' and line
     * breaks are replaced by codes, so the separators never appear inside a value.
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("|", "\\p")
                .replace(",", "\\c")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /** Reverses escape(): reads the text character by character. */
    public static String unescape(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                switch (next) {
                    case 'p' -> sb.append('|');
                    case 'c' -> sb.append(',');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    default -> sb.append(next); // "\\" -> "\"
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Stores a list (e.g. image URLs) in one field as comma-separated escaped values. */
    public static String joinList(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(escape(v));
        }
        return sb.toString();
    }

    public static List<String> splitList(String text) {
        List<String> values = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return values;
        }
        for (String part : text.split(",", -1)) {
            values.add(unescape(part));
        }
        return values;
    }
}
