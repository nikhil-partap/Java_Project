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
 * Saves and loads users, listings, buyer requests, and purchases as plain text
 * files inside the data/ folder (fields separated by '|').
 */
public class FileStorage {

    private static final String USERS_FILE = "users.txt";
    private static final String LISTINGS_FILE = "listings.txt";
    private static final String REQUESTS_FILE = "requests.txt";
    private static final String PURCHASES_FILE = "purchases.txt";

    private final File folder;

    public FileStorage(String folderName) {
        this.folder = new File(folderName);
    }

    public boolean hasSavedData() {
        return new File(folder, USERS_FILE).exists();
    }

    public String getFolderPath() {
        return folder.getAbsolutePath();
    }

    public synchronized void saveAll(List<User> users, List<Listing> listings, List<Purchase> purchases)
            throws IOException {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        writeEntities(USERS_FILE, users);
        writeEntities(LISTINGS_FILE, listings);
        writeEntities(PURCHASES_FILE, purchases);

        List<String> requestLines = new ArrayList<>();
        for (Listing listing : listings) {
            for (BuyerRequest req : listing.getBuyerRequests()) {
                requestLines.add(listing.getId() + "|" + req.toFileLine());
            }
        }
        writeLines(REQUESTS_FILE, requestLines);
    }

    private void writeEntities(String fileName, List<? extends BaseEntity> items) throws IOException {
        List<String> lines = new ArrayList<>();
        for (BaseEntity item : items) {
            lines.add(item.toFileLine());
        }
        writeLines(fileName, lines);
    }

    private void writeLines(String fileName, List<String> lines) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(folder, fileName)))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    public List<User> loadUsers() throws IOException {
        List<User> list = new ArrayList<>();
        for (String[] f : readLines(USERS_FILE)) {
            list.add(User.fromFileLine(f));
        }
        return list;
    }

    public List<Listing> loadListings() throws IOException {
        List<Listing> list = new ArrayList<>();
        for (String[] f : readLines(LISTINGS_FILE)) {
            list.add(Listing.fromFileLine(f));
        }
        for (String[] f : readLines(REQUESTS_FILE)) {
            String listingId = f[0];
            String[] reqFields = new String[f.length - 1];
            System.arraycopy(f, 1, reqFields, 0, reqFields.length);
            BuyerRequest req = BuyerRequest.fromFileLine(reqFields);
            for (Listing listing : list) {
                if (listing.getId().equals(listingId)) {
                    listing.addRequest(req);
                    break;
                }
            }
        }
        return list;
    }

    public List<Purchase> loadPurchases() throws IOException {
        List<Purchase> list = new ArrayList<>();
        for (String[] f : readLines(PURCHASES_FILE)) {
            list.add(Purchase.fromFileLine(f));
        }
        return list;
    }

    private List<String[]> readLines(String fileName) throws IOException {
        List<String[]> rows = new ArrayList<>();
        File file = new File(folder, fileName);
        if (!file.exists()) {
            return rows;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    rows.add(line.split("\\|", -1));
                }
            }
        }
        return rows;
    }

    /** Strips '|' so user input never breaks the text-file separator. */
    public static String clean(String text) {
        return text == null ? "" : text.replace("|", "/").trim();
    }
}
