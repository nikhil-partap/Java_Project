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
    public String[] toFields() {
        return new String[] {getId(), buyerId, message, String.valueOf(offerPrice), status, createdAt};
    }

    /** Rebuilds a request from requests.txt (WRAPPER parsing: Double.parseDouble). */
    public static BuyerRequest fromFields(String[] f) {
        return new BuyerRequest(f[0], f[1], f[2], Double.parseDouble(f[3]), f[4], f[5]);
    }

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
