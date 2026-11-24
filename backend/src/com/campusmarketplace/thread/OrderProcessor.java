package com.campusmarketplace.thread;

import com.campusmarketplace.model.Purchase;
import com.campusmarketplace.service.MarketplaceService;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MULTITHREADING - CALLABLE: unlike Runnable, a Callable RETURNS a value (the Purchase)
 * and may THROW an exception. It is submitted to an ExecutorService and the caller
 * reads the result through a Future.
 */
public class OrderProcessor implements Callable<Purchase> {

    // STATIC + ATOMICINTEGER: one counter shared by all OrderProcessor objects/threads.
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
            System.out.println(String.format("[%s] order OK   listing=%s buyer=%s (orders so far: %d)",
                    thread, listingId, buyerId, total));
            return purchase;
        } catch (Exception e) {
            System.out.println(String.format("[%s] order FAIL listing=%s buyer=%s -> %s",
                    thread, listingId, buyerId, e.getMessage()));
            throw e; // re-throw: Future.get() will wrap it in an ExecutionException
        }
    }

    public static int getOrdersProcessed() {
        return ORDERS_PROCESSED.get();
    }
}
