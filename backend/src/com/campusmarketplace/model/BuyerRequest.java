package com.campusmarketplace.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** OOP + INHERITANCE: an offer/request a buyer sends on a listing. Immutable (all final). */
public class BuyerRequest extends BaseEntity {

    private final String buyerId;
    private final String message;
    private final double offerPrice;
    private final String status;
    private final String createdAt;

    public BuyerRequest(String id, String buyerId, String message, double offerPrice,
                        String status, String createdAt) {
        super(id);
        this.buyerId = buyerId;
        this.message = message;
        this.offerPrice = offerPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getBuyerId() { return buyerId; }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", getId());
        map.put("buyerId", buyerId);
        map.put("message", message);
        map.put("offerPrice", offerPrice); // AUTOBOXING: double -> Double
        map.put("status", status);
        map.put("createdAt", createdAt);
        return map;
    }
}
