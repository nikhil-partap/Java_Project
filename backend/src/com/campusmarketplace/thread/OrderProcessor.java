package com.campusmarketplace.thread;

import com.campusmarketplace.model.Purchase;
import com.campusmarketplace.service.MarketplaceService;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/** Callable worker task that executes a single purchase on the order thread pool. */
public class OrderProcessor implements Callable<Purchase> {

    private static final AtomicInteger ORDERS_PROCESSED = new AtomicInteger();

    private final MarketplaceService service;
    private final String listingId;
    private final String buyerId;

    public OrderProcessor(MarketplaceService service, String listingId, String buyerId) {
        this.service = service;
        this.listingId = listingId;
        this.buyerId = buyerId;
    }

    @Override
    public Purchase call() throws Exception {
        String thread = Thread.currentThread().getName();
        try {
            Purchase purchase = service.completePurchase(listingId, buyerId);
            int total = ORDERS_PROCESSED.incrementAndGet();
            System.out.printf("[%s] Order OK   listing=%s buyer=%s (total: %d)%n", thread, listingId, buyerId, total);
            return purchase;
        } catch (Exception e) {
            System.out.printf("[%s] Order FAIL listing=%s buyer=%s -> %s%n", thread, listingId, buyerId, e.getMessage());
            throw e;
        }
    }

    public static int getOrdersProcessed() {
        return ORDERS_PROCESSED.get();
    }
}
