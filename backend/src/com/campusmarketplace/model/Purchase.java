package com.campusmarketplace.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** OOP + INHERITANCE: an immutable record of a completed order (all fields final). */
public class Purchase extends BaseEntity {

    private final String listingId;
    private final String title;
    private final double price;
    private final String sellerId;
    private final String buyerId;
    private final String purchasedAt;
    private final String imageUrl;

    public Purchase(String id, String listingId, String title, double price, String sellerId,
                    String buyerId, String purchasedAt, String imageUrl) {
        super(id);
        this.listingId = listingId;
        this.title = title;
        this.price = price;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.purchasedAt = purchasedAt;
        this.imageUrl = imageUrl;
    }

    public double getPrice() { return price; }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", getId());
        map.put("listingId", listingId);
        map.put("title", title);
        map.put("price", price);
        map.put("sellerId", sellerId);
        map.put("buyerId", buyerId);
        map.put("purchasedAt", purchasedAt);
        map.put("imageUrl", imageUrl);
        return map;
    }
}
