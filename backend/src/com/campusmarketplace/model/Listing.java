package com.campusmarketplace.model;

import java.util.ArrayList;
import java.util.List;

public class Listing extends BaseEntity implements Comparable<Listing> {

    private final String ownerId;
    private final String title;
    private final String category;
    private final double price;
    private final String condition;
    private final String location;
    private volatile String status; // "Available", "Pending", "Sold"
    private final String createdAt;
    private final List<BuyerRequest> buyerRequests = new ArrayList<>();

    public Listing(String id, String ownerId, String title, String category, double price,
                   String condition, String location, String status, String createdAt) {
        super(id);
        this.ownerId = ownerId;
        this.title = title;
        this.category = category;
        this.price = price;
        this.condition = condition;
        this.location = location;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getOwnerId() { return ownerId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getCondition() { return condition; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }

    public synchronized List<BuyerRequest> getBuyerRequests() {
        return new ArrayList<>(buyerRequests);
    }

    public synchronized void addRequest(BuyerRequest request) {
        buyerRequests.add(request);
    }

    @Override
    public int compareTo(Listing other) {
        return other.createdAt.compareTo(this.createdAt); // newest first
    }

    @Override
    public String toFileLine() {
        return String.join("|", getId(), ownerId, title, category,
                String.valueOf(price), condition, location, status, createdAt);
    }

    public static Listing fromFileLine(String[] f) {
        return new Listing(f[0], f[1], f[2], f[3], Double.parseDouble(f[4]), f[5], f[6], f[7], f[8]);
    }
}
