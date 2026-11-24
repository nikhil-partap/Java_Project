package com.campusmarketplace.model;

import com.campusmarketplace.storage.FileStorage;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OOP + INHERITANCE: an item for sale.
 * COMPARABLE: the "natural order" of listings is newest first (used by Collections.sort).
 * MULTITHREADING: getters/setters/toMap are synchronized on the listing object itself,
 * so a listing is never read while another thread is half-way through changing it.
 */
public class Listing extends BaseEntity implements Comparable<Listing> {

    private final String ownerId;
    private String title;
    private String category;
    private double price;
    private String condition;
    private String description;
    private String location;
    private volatile String status; // volatile: every thread always sees the latest value
    private String createdAt;
    private String imageUrl;
    private List<String> images = new ArrayList<>();
    private List<BuyerRequest> buyerRequests = new ArrayList<>();

    public Listing(String id, String ownerId, String title, String category, double price,
                   String condition, String description, String location, String status,
                   String createdAt, String imageUrl) {
        super(id);
        this.ownerId = ownerId;
        this.title = title;
        this.category = category;
        this.price = price;
        this.condition = condition;
        this.description = description;
        this.location = location;
        this.status = status;
        this.createdAt = createdAt;
        this.imageUrl = imageUrl;
    }

    // ---- getters ----
    public String getOwnerId() { return ownerId; }
    public synchronized String getTitle() { return title; }
    public synchronized String getCategory() { return category; }
    public synchronized double getPrice() { return price; }
    public synchronized String getDescription() { return description; }
    public String getStatus() { return status; }
    public synchronized String getImageUrl() { return imageUrl; }
    public synchronized List<BuyerRequest> getBuyerRequests() { return new ArrayList<>(buyerRequests); }

    // ---- setters ----
    public synchronized void setTitle(String title) { this.title = title; }
    public synchronized void setCategory(String category) { this.category = category; }
    public synchronized void setPrice(double price) { this.price = price; }
    public synchronized void setCondition(String condition) { this.condition = condition; }
    public synchronized void setDescription(String description) { this.description = description; }
    public synchronized void setLocation(String location) { this.location = location; }
    public void setStatus(String status) { this.status = status; }
    public synchronized void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public synchronized void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public synchronized void setImages(List<String> images) { this.images = new ArrayList<>(images); }
    public synchronized void setBuyerRequests(List<BuyerRequest> requests) { this.buyerRequests = new ArrayList<>(requests); }

    /** Newest request goes first (same as JavaScript's unshift in the frontend). */
    public synchronized void addRequestFirst(BuyerRequest request) {
        buyerRequests.add(0, request);
    }

    /** Used when loading from the file, where requests are already in newest-first order. */
    public synchronized void addRequestLast(BuyerRequest request) {
        buyerRequests.add(request);
    }

    /** FILE HANDLING: buyer requests are saved separately in requests.txt. */
    @Override
    public synchronized String[] toFields() {
        return new String[] {getId(), ownerId, title, category, String.valueOf(price), condition,
                description, location, status, createdAt, imageUrl, FileStorage.joinList(images)};
    }

    public static Listing fromFields(String[] f) {
        Listing listing = new Listing(f[0], f[1], f[2], f[3], Double.parseDouble(f[4]), f[5], f[6],
                f[7], f[8], f[9], f[10]);
        listing.setImages(FileStorage.splitList(f[11]));
        return listing;
    }

    /** COMPARABLE: natural order = newest createdAt first. */
    @Override
    public int compareTo(Listing other) {
        return parseTime(other.createdAt).compareTo(parseTime(this.createdAt));
    }

    private static Instant parseTime(String text) {
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException | NullPointerException e) { // MULTI-CATCH
            return Instant.EPOCH; // bad or missing dates sort last
        }
    }

    @Override
    public synchronized Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", getId());
        map.put("ownerId", ownerId);
        map.put("title", title);
        map.put("category", category);
        map.put("price", price);
        map.put("condition", condition);
        map.put("description", description);
        map.put("location", location);
        map.put("status", status);
        map.put("createdAt", createdAt);
        map.put("imageUrl", imageUrl);
        map.put("images", new ArrayList<>(images));
        List<Map<String, Object>> requests = new ArrayList<>();
        for (BuyerRequest r : buyerRequests) {
            requests.add(r.toMap());
        }
        map.put("buyerRequests", requests);
        return map;
    }
}
