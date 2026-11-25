package com.campusmarketplace.exception;

/** Custom checked exception for invalid marketplace operations. */
public class MarketplaceException extends Exception {
    public MarketplaceException(String message) {
        super(message);
    }
}
