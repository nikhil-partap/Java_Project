package com.campusmarketplace.exception;

/** Thrown when two buyers try to purchase the same item at once. */
public class ItemSoldException extends MarketplaceException {
    public ItemSoldException(String message) {
        super(message);
    }
}
