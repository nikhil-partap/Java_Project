package com.campusmarketplace.model;

public class BuyerRequest extends BaseEntity {

    private final String buyerId;
    private final String message;
    private final double offerPrice;
    private final String createdAt;

    public BuyerRequest(String id, String buyerId, String message, double offerPrice, String createdAt) {
        super(id);
        this.buyerId = buyerId;
        this.message = message;
        this.offerPrice = offerPrice;
        this.createdAt = createdAt;
    }

    public String getBuyerId() { return buyerId; }
    public String getMessage() { return message; }
    public double getOfferPrice() { return offerPrice; }
    public String getCreatedAt() { return createdAt; }

    @Override
    public String toFileLine() {
        return String.join("|", getId(), buyerId, message, String.valueOf(offerPrice), createdAt);
    }

    public static BuyerRequest fromFileLine(String[] f) {
        return new BuyerRequest(f[0], f[1], f[2], Double.parseDouble(f[3]), f[4]);
    }
}
