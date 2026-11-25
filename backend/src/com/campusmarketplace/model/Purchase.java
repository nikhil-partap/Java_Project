package com.campusmarketplace.model;

public class Purchase extends BaseEntity {

    private final String listingId;
    private final String title;
    private final double price;
    private final String sellerId;
    private final String buyerId;
    private final String purchasedAt;

    public Purchase(String id, String listingId, String title, double price,
                    String sellerId, String buyerId, String purchasedAt) {
        super(id);
        this.listingId = listingId;
        this.title = title;
        this.price = price;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.purchasedAt = purchasedAt;
    }

    public String getListingId() { return listingId; }
    public String getTitle() { return title; }
    public double getPrice() { return price; }
    public String getSellerId() { return sellerId; }
    public String getBuyerId() { return buyerId; }
    public String getPurchasedAt() { return purchasedAt; }

    @Override
    public String toFileLine() {
        return String.join("|", getId(), listingId, title,
                String.valueOf(price), sellerId, buyerId, purchasedAt);
    }

    public static Purchase fromFileLine(String[] f) {
        return new Purchase(f[0], f[1], f[2], Double.parseDouble(f[3]), f[4], f[5], f[6]);
    }
}
