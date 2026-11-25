package com.campusmarketplace;

// User model
class User {
    private final String id;
    private final String name;
    private final String email;
    private final String password;
    private final String department;

    public User(String id, String name, String email, String password, String department) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.department = department;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public boolean checkPassword(String pass) { return password.equals(pass); }

    public String toLine() {
        return String.join("|", id, name, email, password, department);
    }

    public static User fromLine(String[] f) {
        return new User(f[0], f[1], f[2], f[3], f[4]);
    }
}

// Listing model
class Listing implements Comparable<Listing> {
    private final String id;
    private final String ownerId;
    private final String title;
    private final String category;
    private final double price;
    private final String location;
    private volatile String status; // "Available" or "Sold"

    public Listing(String id, String ownerId, String title, String category, double price, String location, String status) {
        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.category = category;
        this.price = price;
        this.location = location;
        this.status = status;
    }

    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public int compareTo(Listing o) {
        return o.id.compareTo(this.id);
    }

    public String toLine() {
        return String.join("|", id, ownerId, title, category, String.valueOf(price), location, status);
    }

    public static Listing fromLine(String[] f) {
        return new Listing(f[0], f[1], f[2], f[3], Double.parseDouble(f[4]), f[5], f[6]);
    }
}

// Purchase Request model (sent by buyer, approved by seller)
class PurchaseRequest {
    private final String id;
    private final String listingId;
    private final String buyerId;
    private final String note;
    private final String createdAt;

    public PurchaseRequest(String id, String listingId, String buyerId, String note, String createdAt) {
        this.id = id;
        this.listingId = listingId;
        this.buyerId = buyerId;
        this.note = note;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getListingId() { return listingId; }
    public String getBuyerId() { return buyerId; }
    public String getNote() { return note; }
    public String getCreatedAt() { return createdAt; }

    public String toLine() {
        return String.join("|", id, listingId, buyerId, note, createdAt);
    }

    public static PurchaseRequest fromLine(String[] f) {
        return new PurchaseRequest(f[0], f[1], f[2], f[3], f[4]);
    }
}

// Purchase receipt model
class Purchase {
    private final String id;
    private final String listingId;
    private final String title;
    private final double price;
    private final String sellerId;
    private final String buyerId;
    private final String purchasedAt;

    public Purchase(String id, String listingId, String title, double price, String sellerId, String buyerId, String purchasedAt) {
        this.id = id;
        this.listingId = listingId;
        this.title = title;
        this.price = price;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.purchasedAt = purchasedAt;
    }

    public String getId() { return id; }
    public String getListingId() { return listingId; }
    public String getTitle() { return title; }
    public double getPrice() { return price; }
    public String getSellerId() { return sellerId; }
    public String getBuyerId() { return buyerId; }
    public String getPurchasedAt() { return purchasedAt; }

    public String toLine() {
        return String.join("|", id, listingId, title, String.valueOf(price), sellerId, buyerId, purchasedAt);
    }

    public static Purchase fromLine(String[] f) {
        return new Purchase(f[0], f[1], f[2], Double.parseDouble(f[3]), f[4], f[5], f[6]);
    }
}
