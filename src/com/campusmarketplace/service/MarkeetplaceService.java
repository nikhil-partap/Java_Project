package com.campusmarketplace.service;

import com.campusmarketplace.model.Item;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MarketplaceService {
    // Thread-safe list for items shared across main thread and background threads
    private final List<Item> items = Collections.synchronizedList(new ArrayList<>());

    // Thread-safe counter for generating unique item IDs concurrently
    private final AtomicInteger nextId = new AtomicInteger(101);
    private final AtomicInteger completedOrders = new AtomicInteger(0);

    public MarketplaceService() {
        // Seed a couple of sample campus listings so testing takes seconds
        addItem("Engineering Drawing Kit", "Lab Gear", 350.0, "Aarav");
        addItem("DSA & Algorithms Notes Bundle", "Books", 200.0, "Neha");
        addItem("Scientific Calculator FX-991EX", "Electronics", 650.0, "Rohan");
    }

    public Item addItem(String title, String category, double price, String sellerName) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Item title cannot be empty.");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }

        int id = nextId.getAndIncrement();
        Item item = new Item(id, title.trim(), category.trim(), price, sellerName.trim());
        items.add(item);
        return item;
    }

    public List<Item> getAllItems() {
        synchronized (items) {
            return new ArrayList<>(items);
        }
    }

    public Item findItemById(int id) {
        synchronized (items) {
            for (Item item : items) {
                if (item.getId() == id) {
                    return item;
                }
            }
        }
        return null;
    }

    // Atomically checks and reserves/purchases an item so two threads can't buy the same item
    public Item reserveItemForPurchase(int id) {
        synchronized (items) {
            Item target = null;
            for (Item item : items) {
                if (item.getId() == id) {
                    target = item;
                    break;
                }
            }
            if (target == null) {
                throw new IllegalArgumentException("No listing found with ID: " + id);
            }
            if (!target.isAvailable()) {
                throw new IllegalStateException("Item [" + id + "] is already SOLD.");
            }
            target.setAvailable(false);
            return target;
        }
    }

    public void recordCompletedOrder() {
        completedOrders.incrementAndGet();
    }

    public int getCompletedOrdersCount() {
        return completedOrders.get();
    }
}