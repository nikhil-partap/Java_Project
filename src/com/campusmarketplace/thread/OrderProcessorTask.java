package com.campusmarketplace.thread;

import com.campusmarketplace.model.Item;
import com.campusmarketplace.service.MarketplaceService;

public class OrderProcessorTask implements Runnable {
    private final MarketplaceService service;
    private final int itemId;
    private final String buyerName;
    private final String meetupSpot;

    public OrderProcessorTask(MarketplaceService service, int itemId, String buyerName, String meetupSpot) {
        this.service = service;
        this.itemId = itemId;
        this.buyerName = buyerName;
        this.meetupSpot = meetupSpot;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        System.out.println("\n[" + threadName + "] Processing purchase request for Item #" + itemId + " by " + buyerName + "...");

        try {
            Thread.sleep(1200);

            // Thread-safe reservation inside service prevents race conditions
            Item item = service.reserveItemForPurchase(itemId);
            service.recordCompletedOrder();

            System.out.println("[" + threadName + "] CONFIRMED: '" + item.getTitle()
                    + "' reserved for " + buyerName + " | Meetup: " + meetupSpot
                    + " | Total Completed Orders: " + service.getCompletedOrdersCount());

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("[" + threadName + "] REJECTED for " + buyerName + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[" + threadName + "] Order processing interrupted.");
        }
    }
}