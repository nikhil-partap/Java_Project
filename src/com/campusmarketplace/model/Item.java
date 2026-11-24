package com.campusmarketplace.model;

public class Item {
    private final int id;
    private final String title;
    private final String category;
    private final double price;
    private final String sellerName;
    private boolean available;

    public Item(int id, String title, String category, double price, String sellerName) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.price = price;
        this.sellerName = sellerName;
        this.available = true;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public String getSellerName() {
        return sellerName;
    }

    // Synchronized getters/setters make state reads/writes safe across threads
    public synchronized boolean isAvailable() {
        return available;
    }

    public synchronized void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public synchronized String toString() {
        String status = available ? "AVAILABLE" : "SOLD";
        return String.format("[%d] %s (%s) - ₹%.2f | Seller: %s | %s",
                id, title, category, price, sellerName, status);
    }
}