package com.campusmarketplace.thread;

import com.campusmarketplace.model.Item;
import com.campusmarketplace.service.MarketplaceService;
import java.util.List;

public class MarketplaceMonitorThread extends Thread {
    private final MarketplaceService service;
    private volatile boolean running = true;
    private volatile String latestSnapshot = "Monitor initializing...";

    public MarketplaceMonitorThread(MarketplaceService service) {
        super("Campus-Monitor-Thread");
        this.service = service;
        setDaemon(true); // Automatically stops when the main application exits
    }

    @Override
    public void run() {
        while (running) {
            try {
                List<Item> items = service.getAllItems();
                long availableCount = items.stream().filter(Item::isAvailable).count();
                long soldCount = items.size() - availableCount;

                latestSnapshot = String.format("[%s] Active Listings: %d | Sold: %d | Processed Orders: %d",
                        getName(), availableCount, soldCount, service.getCompletedOrdersCount());

                Thread.sleep(2000); // Refreshes marketplace stats every 2 seconds in the background
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public String getLatestSnapshot() {
        return latestSnapshot;
    }

    public void shutdownMonitor() {
        running = false;
        this.interrupt();
    }
}