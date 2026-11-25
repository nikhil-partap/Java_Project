package com.campusmarketplace;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Simple file storage writing users.txt, listings.txt, purchases.txt in data/
class FileStorage {
    private final File folder;

    public FileStorage(String folderName) {
        this.folder = new File(folderName);
    }

    public boolean hasData() {
        return new File(folder, "users.txt").exists();
    }

    public synchronized void saveAll(List<User> users, List<Listing> listings, List<Purchase> purchases) {
        if (!folder.exists()) folder.mkdirs();
        writeFile("users.txt", users.stream().map(User::toLine).toList());
        writeFile("listings.txt", listings.stream().map(Listing::toLine).toList());
        writeFile("purchases.txt", purchases.stream().map(Purchase::toLine).toList());
    }

    private void writeFile(String fileName, List<String> lines) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(folder, fileName)))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("File write error: " + e.getMessage());
        }
    }

    public List<User> loadUsers() {
        List<User> list = new ArrayList<>();
        for (String[] f : readLines("users.txt")) list.add(User.fromLine(f));
        return list;
    }

    public List<Listing> loadListings() {
        List<Listing> list = new ArrayList<>();
        for (String[] f : readLines("listings.txt")) list.add(Listing.fromLine(f));
        return list;
    }

    public List<Purchase> loadPurchases() {
        List<Purchase> list = new ArrayList<>();
        for (String[] f : readLines("purchases.txt")) list.add(Purchase.fromLine(f));
        return list;
    }

    private List<String[]> readLines(String fileName) {
        List<String[]> list = new ArrayList<>();
        File file = new File(folder, fileName);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isBlank()) list.add(line.split("\\|", -1));
            }
        } catch (IOException e) {
            System.out.println("File read error: " + e.getMessage());
        }
        return list;
    }

    public static String clean(String s) {
        return s == null ? "" : s.replace("|", "/").trim();
    }
}
